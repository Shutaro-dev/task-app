import { useEffect, useMemo, useRef, useState } from 'react';
import type { Role, Task, SharpenTheSawArea, WeekData, ScheduledTask } from '../types';
import LeftSidebar from './LeftSidebar';
import WeeklyCalendar from './WeeklyCalendar';
import RightSidebar from './RightSidebar';
import SharpenTheSawSettings from './SharpenTheSawSettings';
import MissionStatementModal from './MissionStatementModal';
import OnboardingTour from './OnboardingTour';
import { ONBOARDING_STEPS } from '../onboarding/tourSteps';
import { KeyedDebouncer } from '../utils/debounce';
import * as roleService from '../services/roleService';
import * as taskService from '../services/taskService';
import * as weekDataService from '../services/weekDataService';
import * as sharpenTheSawService from '../services/sharpenTheSawService';
import * as missionStatementService from '../services/missionStatementService';
import { migrateLegacyLocalStorageIfNeeded, readLegacyListMode } from '../services/migrationService';
import styles from './Dashboard.module.css';

const DEFAULT_STORAGE_KEY = 'fourth-gen-time-management';
const ROLE_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085'];
const DEBOUNCE_MS = 500;

function getStartOfWeek(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Monday start
  return new Date(d.setDate(diff));
}

function getWeekKey(date: Date): string {
  return date.toISOString().split('T')[0];
}

function blankWeekData(weekStart: Date): WeekData {
  return {
    weekStart,
    scheduledTasks: [],
    dayNotes: Array.from({ length: 7 }, (_, i) => ({ day: i, notes: '' })),
    weeklyNotes: '',
    temporaryTasks: [],
  };
}

function isTempId(id: string): boolean {
  return id.startsWith('temp-');
}

interface DashboardProps {
  // ログインユーザーごとにDBのデータを分離する必要はないが(Rails側でuser_idスコープ済み)、
  // 旧localStorage運用時代のデータを見つけて取り込むためのキーとして引き続き使う
  storageKey?: string;
  // RightSidebar上部に表示するアカウント情報(未指定時は何も表示しない)
  userLabel?: string;
  onLogout?: () => void;
  // サインアップ直後だけtrue。マウント時に一度だけ読み、オンボーディングツアーの自動起動に使う
  startOnboarding?: boolean;
  // ツアーを起動したら呼び、AuthContext側のフラグを消費済みにする(再マウントでの再起動防止)
  onOnboardingStarted?: () => void;
}

function Dashboard({
  storageKey = DEFAULT_STORAGE_KEY,
  userLabel,
  onLogout,
  startOnboarding = false,
  onOnboardingStarted,
}: DashboardProps) {
  const listModeStorageKey = `${storageKey}:list-mode`;

  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  const [showSettings, setShowSettings] = useState(false);
  const [showMissionSettings, setShowMissionSettings] = useState(false);
  const [isListMode, setIsListMode] = useState(() => {
    try {
      const stored = localStorage.getItem(listModeStorageKey);
      if (stored !== null) return stored === 'true';
      // 新キーが無ければ、DB移行がまだ済んでいない旧localStorageの値を読み(移行完了を待たない)、
      // 新キーへ書き込んで以降のセッションでも保持されるようにする
      const legacyValue = readLegacyListMode(storageKey);
      if (legacyValue !== undefined) {
        localStorage.setItem(listModeStorageKey, String(legacyValue));
        return legacyValue;
      }
      return false;
    } catch {
      return false;
    }
  });
  // リロードのたびに「今週」から始める(週の閲覧位置はDBに持たせるほどの価値が無い表示状態のため)
  const [currentWeek, setCurrentWeek] = useState<Date>(() => getStartOfWeek(new Date()));
  const [roles, setRoles] = useState<Role[]>([]);
  const [sharpenTheSawAreas, setSharpenTheSawAreas] = useState<SharpenTheSawArea[]>([]);
  const [missionStatement, setMissionStatement] = useState<string>('');
  // セッション内で訪れた週だけをキャッシュする(全データをlocalStorageへ丸ごと持つ旧方式は廃止)
  const [weekDataCache, setWeekDataCache] = useState<Map<string, WeekData>>(new Map());
  const [isTourActive, setIsTourActive] = useState(false);

  const draggedTaskRef = useRef<Task | null>(null);
  const dashboardRootRef = useRef<HTMLDivElement>(null);

  // mousemove/キー入力のたびに飛んでくる高頻度な更新をサーバー保存だけ間引くためのデバウンサー群。
  // ローカルstateは各ハンドラ内で即時更新するので、体感速度は落ちない。
  const scheduledTaskDebouncer = useRef(
    new KeyedDebouncer<Partial<ScheduledTask>>((id, updates) => {
      weekDataService.updateScheduledTask(id, updates).catch(handleSaveError);
    }, DEBOUNCE_MS)
  );
  const dayNotesDebouncer = useRef(
    new KeyedDebouncer<{ notes?: string }>((key, updates) => {
      const [weekStart, dayStr] = key.split(':');
      weekDataService.upsertDayNotes(weekStart, Number(dayStr), updates).catch(handleSaveError);
    }, DEBOUNCE_MS + 100)
  );
  const weeklyNotesDebouncer = useRef(
    new KeyedDebouncer<{ weeklyNotes: string }>((weekStart, updates) => {
      weekDataService.updateWeeklyNotes(weekStart, updates.weeklyNotes).catch(handleSaveError);
    }, DEBOUNCE_MS + 100)
  );

  const flushAllDebouncers = () => {
    scheduledTaskDebouncer.current.flushAll();
    dayNotesDebouncer.current.flushAll();
    weeklyNotesDebouncer.current.flushAll();
  };

  // アンマウント時(別アカウントへの切り替え等)にも保留中の書き込みを取りこぼさない
  useEffect(() => flushAllDebouncers, []);

  function handleSaveError(error: unknown) {
    console.error(error);
    setSaveError('保存に失敗しました。通信環境をご確認のうえ、しばらくしてからもう一度お試しください。');
  }

  // サインアップ直後の初回マウント時のみオンボーディングツアーを自動起動する
  useEffect(() => {
    if (!startOnboarding) return;
    setIsTourActive(true);
    onOnboardingStarted?.();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 初回マウント: 旧localStorageデータがあれば一度だけDBへ取り込んでから、DBの内容を読み込む
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setIsLoading(true);
        setLoadError(null);
        await migrateLegacyLocalStorageIfNeeded(storageKey);

        const initialWeekKey = getWeekKey(currentWeek);
        const [fetchedRoles, areas, mission, week] = await Promise.all([
          roleService.fetchRoles(),
          sharpenTheSawService.fetchSharpenTheSawAreas(),
          missionStatementService.fetchMissionStatement(),
          weekDataService.fetchWeekData(initialWeekKey),
        ]);
        if (cancelled) return;

        setRoles(fetchedRoles);
        setSharpenTheSawAreas(areas);
        setMissionStatement(mission);
        setWeekDataCache(new Map([[initialWeekKey, week]]));
      } catch (error) {
        if (cancelled) return;
        console.error(error);
        setLoadError('データの読み込みに失敗しました。ページを再読み込みしてください。');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [storageKey]);

  const roleColorMap = useMemo(() => {
    const map: Record<string, string> = {};
    roles.forEach(r => { map[r.id] = r.color || '#4a90d9'; });
    return map;
  }, [roles]);

  const weekKey = getWeekKey(currentWeek);
  const currentWeekData = weekDataCache.get(weekKey) ?? blankWeekData(currentWeek);

  const updateCurrentWeekData = (updater: (data: WeekData) => WeekData) => {
    setWeekDataCache(prev => {
      const existing = prev.get(weekKey) ?? blankWeekData(currentWeek);
      const next = new Map(prev);
      next.set(weekKey, updater(existing));
      return next;
    });
  };

  const changeWeek = (newWeek: Date) => {
    flushAllDebouncers();
    setCurrentWeek(newWeek);
    const key = getWeekKey(newWeek);
    if (weekDataCache.has(key)) return;
    weekDataService
      .fetchWeekData(key)
      .then(data => setWeekDataCache(prev => (prev.has(key) ? prev : new Map(prev).set(key, data))))
      .catch(handleSaveError);
  };

  const addRole = async (name: string) => {
    const tempId = `temp-${Date.now()}`;
    const color = ROLE_COLORS[roles.length % ROLE_COLORS.length];
    setRoles(prev => [...prev, { id: tempId, name, tasks: [], isExpanded: false, color }]);
    try {
      const created = await roleService.createRole(name, color, false);
      setRoles(prev => prev.map(r => (r.id === tempId ? created : r)));
    } catch (error) {
      setRoles(prev => prev.filter(r => r.id !== tempId));
      handleSaveError(error);
    }
  };

  const deleteRole = (roleId: string) => {
    const previousRoles = roles;
    const previousWeekDataCache = weekDataCache;
    setRoles(prev => prev.filter(role => role.id !== roleId));
    setWeekDataCache(prev => {
      const next = new Map<string, WeekData>();
      prev.forEach((wd, key) => {
        next.set(key, {
          ...wd,
          scheduledTasks: wd.scheduledTasks.filter(task => task.roleId !== roleId),
          // DB側はロール削除でこのロール配下のtasks(一時タスク含む)もカスケード削除されるため、
          // 既に取得済みの週キャッシュからも一時タスクを合わせて消しておく
          temporaryTasks: (wd.temporaryTasks ?? []).filter(task => task.roleId !== roleId),
        });
      });
      return next;
    });
    if (isTempId(roleId)) return;
    roleService.deleteRole(roleId).catch(error => {
      // 削除に失敗したのに画面から消えたままだとDBとの不整合に気付けないため元に戻す
      setRoles(previousRoles);
      setWeekDataCache(previousWeekDataCache);
      handleSaveError(error);
    });
  };

  const addTask = async (roleId: string, taskTitle: string, isPermanent: boolean) => {
    const tempId = `temp-${Date.now()}`;
    const newTask: Task = { id: tempId, title: taskTitle, roleId, isPermanent };
    if (isPermanent) {
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: [...r.tasks, newTask] } : r)));
    } else {
      updateCurrentWeekData(wd => ({ ...wd, temporaryTasks: [...(wd.temporaryTasks ?? []), newTask] }));
    }

    try {
      const created = await taskService.createTask({
        roleId,
        title: taskTitle,
        isPermanent,
        weekStart: isPermanent ? undefined : weekKey,
      });
      if (isPermanent) {
        setRoles(prev => prev.map(r => (
          r.id === roleId ? { ...r, tasks: r.tasks.map(t => (t.id === tempId ? created : t)) } : r
        )));
      } else {
        updateCurrentWeekData(wd => ({
          ...wd,
          temporaryTasks: (wd.temporaryTasks ?? []).map(t => (t.id === tempId ? created : t)),
        }));
      }
    } catch (error) {
      if (isPermanent) {
        setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: r.tasks.filter(t => t.id !== tempId) } : r)));
      } else {
        updateCurrentWeekData(wd => ({
          ...wd,
          temporaryTasks: (wd.temporaryTasks ?? []).filter(t => t.id !== tempId),
        }));
      }
      handleSaveError(error);
    }
  };

  const toggleRole = (roleId: string) => {
    const role = roles.find(r => r.id === roleId);
    if (!role) return;
    const nextExpanded = !role.isExpanded;
    setRoles(prev => prev.map(r => {
      if (r.id !== roleId) return r;
      // 役割を閉じる際にshowAddTaskをリセット
      return nextExpanded ? { ...r, isExpanded: nextExpanded } : { ...r, isExpanded: nextExpanded, showAddTask: false };
    }));
    if (isTempId(roleId)) return;
    roleService.updateRoleExpanded(roleId, nextExpanded).catch(handleSaveError);
  };

  const handleTaskDragStart = (task: Task) => {
    draggedTaskRef.current = task;
  };

  const handleTaskDrop = async (day: number, startTime: string) => {
    const draggedTask = draggedTaskRef.current;
    if (!draggedTask) return;
    draggedTaskRef.current = null;

    const tempId = `temp-${Date.now()}`;
    const optimistic: ScheduledTask = {
      id: tempId,
      taskId: draggedTask.id,
      day,
      startTime,
      duration: 60, // Default 60 minutes
      title: draggedTask.title,
      roleId: draggedTask.roleId,
    };
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: [...wd.scheduledTasks, optimistic] }));

    try {
      const created = await weekDataService.createScheduledTask(weekKey, {
        taskId: draggedTask.id,
        day,
        startTime,
        duration: 60,
        title: draggedTask.title,
        roleId: draggedTask.roleId,
      });
      updateCurrentWeekData(wd => ({
        ...wd,
        scheduledTasks: wd.scheduledTasks.map(t => (t.id === tempId ? created : t)),
      }));
      scheduledTaskDebouncer.current.rekey(tempId, created.id);
    } catch (error) {
      updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: wd.scheduledTasks.filter(t => t.id !== tempId) }));
      handleSaveError(error);
    }
  };

  const updateScheduledTask = (taskId: string, updates: Partial<ScheduledTask>) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      scheduledTasks: wd.scheduledTasks.map(t => (t.id === taskId ? { ...t, ...updates } : t)),
    }));
    // サーバー採番ID反映前の一瞬はスキップし、created応答の反映後に改めて保存される
    if (isTempId(taskId)) return;
    scheduledTaskDebouncer.current.schedule(taskId, updates);
  };

  const updateDayNotes = (day: number, notes: string) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      dayNotes: wd.dayNotes.map(dn => (dn.day === day ? { ...dn, notes } : dn)),
    }));
    dayNotesDebouncer.current.schedule(`${weekKey}:${day}`, { notes });
  };

  const updateSleepTime = (day: number, sleepStart: string, sleepEnd: string) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      dayNotes: wd.dayNotes.map(dn => (dn.day === day ? { ...dn, sleepStart, sleepEnd } : dn)),
    }));
    weekDataService.upsertDayNotes(weekKey, day, { sleepStart, sleepEnd }).catch(handleSaveError);
  };

  const updateWeeklyNotes = (notes: string) => {
    updateCurrentWeekData(wd => ({ ...wd, weeklyNotes: notes }));
    weeklyNotesDebouncer.current.schedule(weekKey, { weeklyNotes: notes });
  };

  const updateSharpenTheSawAreas = async (areas: SharpenTheSawArea[]) => {
    const previousAreas = sharpenTheSawAreas;
    setSharpenTheSawAreas(areas);
    try {
      // サーバー側で新規タスクに採番された本物のIDへ置き換える(次回保存時の重複作成を防ぐため)
      const saved = await sharpenTheSawService.updateSharpenTheSawAreas(areas);
      setSharpenTheSawAreas(saved);
    } catch (error) {
      setSharpenTheSawAreas(previousAreas);
      handleSaveError(error);
    }
  };

  const updateMissionStatement = (text: string) => {
    setMissionStatement(text);
    missionStatementService.updateMissionStatement(text).catch(handleSaveError);
  };

  const updateRoleName = (roleId: string, newName: string) => {
    setRoles(prev => prev.map(role => (role.id === roleId ? { ...role, name: newName } : role)));
    if (isTempId(roleId)) return;
    roleService.updateRoleName(roleId, newName).catch(handleSaveError);
  };

  const updateRoleColor = (roleId: string, color: string) => {
    setRoles(prev => prev.map(role => (role.id === roleId ? { ...role, color } : role)));
    if (isTempId(roleId)) return;
    roleService.updateRoleColor(roleId, color).catch(handleSaveError);
  };

  const updateTaskTitle = (roleId: string, taskId: string, newTitle: string) => {
    const role = roles.find(r => r.id === roleId);
    const taskInRole = role?.tasks.find(t => t.id === taskId);
    if (taskInRole) {
      setRoles(prev => prev.map(r => (
        r.id === roleId ? { ...r, tasks: r.tasks.map(t => (t.id === taskId ? { ...t, title: newTitle } : t)) } : r
      )));
      if (!isTempId(taskId)) {
        taskService.updateTask(taskId, { title: newTitle, isPermanent: true }).catch(handleSaveError);
      }
      return;
    }
    const tempTask = currentWeekData.temporaryTasks?.find(t => t.id === taskId);
    if (tempTask) {
      updateCurrentWeekData(wd => ({
        ...wd,
        temporaryTasks: (wd.temporaryTasks ?? []).map(t => (t.id === taskId ? { ...t, title: newTitle } : t)),
      }));
      if (!isTempId(taskId)) {
        taskService.updateTask(taskId, { title: newTitle, isPermanent: false, weekStart: weekKey }).catch(handleSaveError);
      }
    }
  };

  const reorderTasks = (roleId: string, reorderedTasks: Task[]) => {
    setRoles(prev => prev.map(role => (
      role.id === roleId ? { ...role, tasks: reorderedTasks.filter(t => t.isPermanent) } : role
    )));
    if (currentWeekData.temporaryTasks) {
      updateCurrentWeekData(wd => {
        const tempForRole = reorderedTasks.filter(t => !t.isPermanent && t.roleId === roleId);
        const otherTemp = (wd.temporaryTasks ?? []).filter(t => t.roleId !== roleId);
        return { ...wd, temporaryTasks: [...otherTemp, ...tempForRole] };
      });
    }
    // 仮ID(サーバー未反映)の項目を除いても、sortOrderは並び替え後の実際の位置(index)を保つ
    // ため、必ずmapで全項目のindexを確定させてからfilterする
    const items = reorderedTasks
      .map((t, index) => ({ id: t.id, sortOrder: index }))
      .filter(item => !isTempId(item.id));
    if (items.length > 0) taskService.reorderTasks(items).catch(handleSaveError);
  };

  const toggleTaskPermanent = (roleId: string, taskId: string, currentlyPermanent: boolean) => {
    const previousRoles = roles;
    const previousWeekDataCache = weekDataCache;
    const rollback = (error: unknown) => {
      // 永続/一時をまたぐ移動なので、失敗時に中途半端な状態(両方に無い/両方にある)を残さず戻す
      setRoles(previousRoles);
      setWeekDataCache(previousWeekDataCache);
      handleSaveError(error);
    };

    if (currentlyPermanent) {
      const role = roles.find(r => r.id === roleId);
      const task = role?.tasks.find(t => t.id === taskId);
      if (!task) return;
      const movedTask: Task = { ...task, isPermanent: false };
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: r.tasks.filter(t => t.id !== taskId) } : r)));
      updateCurrentWeekData(wd => ({ ...wd, temporaryTasks: [...(wd.temporaryTasks ?? []), movedTask] }));
      if (!isTempId(taskId)) {
        taskService.updateTask(taskId, { title: task.title, isPermanent: false, weekStart: weekKey }).catch(rollback);
      }
    } else {
      const task = currentWeekData.temporaryTasks?.find(t => t.id === taskId);
      if (!task) return;
      const movedTask: Task = { ...task, isPermanent: true };
      updateCurrentWeekData(wd => ({
        ...wd,
        temporaryTasks: (wd.temporaryTasks ?? []).filter(t => t.id !== taskId),
      }));
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: [...r.tasks, movedTask] } : r)));
      if (!isTempId(taskId)) {
        taskService.updateTask(taskId, { title: task.title, isPermanent: true }).catch(rollback);
      }
    }
  };

  const toggleListMode = () => {
    setIsListMode(prev => {
      const next = !prev;
      try {
        localStorage.setItem(listModeStorageKey, String(next));
      } catch (error) {
        console.warn('Failed to persist list mode preference:', error);
      }
      return next;
    });
  };

  const reorderRoles = (newRoles: Role[]) => {
    // newRolesはLeftSidebarのroleList由来でmerged tasks(permanent + temporary)を含む。
    // roles(permanentタスクのみ)を元に順序だけ入れ替えることで二重追加を防ぐ。
    setRoles(prev => newRoles.map(r => prev.find(existing => existing.id === r.id) ?? r));
    const items = newRoles
      .map((r, index) => ({ id: r.id, sortOrder: index }))
      .filter(item => !isTempId(item.id));
    if (items.length > 0) roleService.reorderRoles(items).catch(handleSaveError);
  };

  const handleTaskDeleted = (taskId: string) => {
    const previousWeekDataCache = weekDataCache;
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: wd.scheduledTasks.filter(t => t.id !== taskId) }));
    scheduledTaskDebouncer.current.cancel(taskId);
    if (isTempId(taskId)) return;
    weekDataService.deleteScheduledTask(taskId).catch(error => {
      setWeekDataCache(previousWeekDataCache);
      handleSaveError(error);
    });
  };

  const deleteTask = (roleId: string, taskId: string) => {
    const previousRoles = roles;
    const previousWeekDataCache = weekDataCache;
    const role = roles.find(r => r.id === roleId);
    const foundInRole = role?.tasks.some(t => t.id === taskId);
    if (foundInRole) {
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: r.tasks.filter(t => t.id !== taskId) } : r)));
    } else {
      updateCurrentWeekData(wd => ({
        ...wd,
        temporaryTasks: (wd.temporaryTasks ?? []).filter(t => t.id !== taskId),
      }));
    }
    // このタスクを参照するスケジュール済みタスクも(DB側はFKカスケード削除される想定なので)ローカルから消す
    setWeekDataCache(prev => {
      const next = new Map<string, WeekData>();
      prev.forEach((wd, key) => {
        next.set(key, { ...wd, scheduledTasks: wd.scheduledTasks.filter(t => t.taskId !== taskId) });
      });
      return next;
    });
    if (isTempId(taskId)) return;
    taskService.deleteTask(taskId).catch(error => {
      setRoles(previousRoles);
      setWeekDataCache(previousWeekDataCache);
      handleSaveError(error);
    });
  };

  const addCopiedTask = async (task: ScheduledTask) => {
    const tempId = `temp-${Date.now()}`;
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: [...wd.scheduledTasks, { ...task, id: tempId }] }));
    try {
      const created = await weekDataService.createScheduledTask(weekKey, {
        taskId: task.taskId,
        day: task.day,
        startTime: task.startTime,
        duration: task.duration,
        title: task.title,
        roleId: task.roleId,
      });
      updateCurrentWeekData(wd => ({
        ...wd,
        scheduledTasks: wd.scheduledTasks.map(t => (t.id === tempId ? created : t)),
      }));
    } catch (error) {
      updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: wd.scheduledTasks.filter(t => t.id !== tempId) }));
      handleSaveError(error);
    }
  };

  const downloadPdf = async () => {
    const root = dashboardRootRef.current;
    if (!root) return;

    // html2canvas/jspdfはPDFダウンロード時にしか使わない重いライブラリ(gzip後で数百KB)なので、
    // 初回バンドルに含めず実際に押されたときだけ動的importする
    const [{ default: html2canvas }, { default: jsPDF }] = await Promise.all([
      import('html2canvas'),
      import('jspdf'),
    ]);

    // 現在のスタイル・スクロール位置を保存
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const prevHeight = root.style.height;
    const prevOverflow = root.style.overflow;

    // textarea要素を一時的にdivに変換して改行を保持
    const textareas = root.querySelectorAll('textarea');
    const replacements: { original: HTMLTextAreaElement; replacement: HTMLDivElement }[] = [];

    textareas.forEach(textarea => {
      const div = document.createElement('div');
      div.style.cssText = textarea.style.cssText;
      div.style.whiteSpace = 'pre-wrap';
      div.style.overflowWrap = 'break-word';
      div.style.overflow = 'hidden';
      div.textContent = textarea.value;

      replacements.push({ original: textarea, replacement: div });
      textarea.parentNode!.replaceChild(div, textarea);
    });

    try {
      // 要素全体が描画されるよう一時的に拡張
      root.style.height = 'auto';
      root.style.overflow = 'visible';

      // 要素の実サイズでキャプチャ
      const width = Math.max(root.scrollWidth, root.clientWidth);
      const height = Math.max(root.scrollHeight, root.clientHeight);
      const captureScale = 2;

      const canvas = await html2canvas(root, {
        scale: captureScale,
        useCORS: true,
        backgroundColor: '#ffffff',
        width,
        height,
        windowWidth: width,
        windowHeight: height,
        scrollX: 0,
        scrollY: 0,
      });

      const imgData = canvas.toDataURL('image/png');

      // 固定のA4に収めると縦横比の違いで余白が出るため、ページサイズ自体をキャプチャした
      // 内容のアスペクト比に合わせ、画像をページ全面(0,0)〜(pageWidth,pageHeight)に敷き詰める
      const PT_PER_PX = 0.75; // 96dpi基準のCSSピクセル→ポイント換算
      const pageWidth = (canvas.width / captureScale) * PT_PER_PX;
      const pageHeight = (canvas.height / captureScale) * PT_PER_PX;

      const pdf = new jsPDF({
        orientation: pageWidth >= pageHeight ? 'landscape' : 'portrait',
        unit: 'pt',
        format: [pageWidth, pageHeight],
      });

      pdf.addImage(imgData, 'PNG', 0, 0, pageWidth, pageHeight);
      pdf.save('dashboard.pdf');
    } finally {
      // 元のtextarea要素を復元
      replacements.forEach(({ original, replacement }) => {
        replacement.parentNode!.replaceChild(original, replacement);
      });

      // スタイル・スクロール位置を復元
      root.style.height = prevHeight;
      root.style.overflow = prevOverflow;
      window.scrollTo(scrollX, scrollY);
    }
  };

  const handleLogout = () => {
    flushAllDebouncers();
    onLogout?.();
  };

  if (isLoading) {
    return <div className={styles.loadingScreen}>読み込み中...</div>;
  }

  if (loadError) {
    return <div className={styles.loadingScreen}>{loadError}</div>;
  }

  return (
    <div className={styles.dashboard} ref={dashboardRootRef}>
      {saveError && (
        <div className={styles.saveErrorBanner}>
          <span>{saveError}</span>
          <button className={styles.saveErrorDismiss} onClick={() => setSaveError(null)}>×</button>
        </div>
      )}

      <LeftSidebar
        roles={roles}
        sharpenTheSawAreas={sharpenTheSawAreas}
        temporaryTasks={currentWeekData.temporaryTasks || []}
        onAddRole={addRole}
        onAddTask={addTask}
        onToggleRole={toggleRole}
        onTaskDragStart={handleTaskDragStart}
        onOpenSettings={() => setShowSettings(true)}
        onDeleteRole={deleteRole}
        onUpdateRoleName={updateRoleName}
        onDeleteTask={deleteTask}
        onUpdateRoleColor={updateRoleColor}
        onUpdateTaskTitle={updateTaskTitle}
        onReorderTasks={reorderTasks}
        onToggleTaskPermanent={toggleTaskPermanent}
        onReorderRoles={reorderRoles}
        userLabel={userLabel}
        onLogout={handleLogout}
        onStartTour={() => setIsTourActive(true)}
      />

      <WeeklyCalendar
        currentWeek={currentWeek}
        scheduledTasks={currentWeekData.scheduledTasks}
        dayNotes={currentWeekData.dayNotes}
        roleColors={roleColorMap}
        isListMode={isListMode}
        onWeekChange={changeWeek}
        onTaskDrop={handleTaskDrop}
        onUpdateDayNotes={updateDayNotes}
        onUpdateSleepTime={updateSleepTime}
        onUpdateTask={updateScheduledTask}
        onTaskDeleted={handleTaskDeleted}
        onAddCopiedTask={addCopiedTask}
        onDownloadPdf={downloadPdf}
        onToggleListMode={toggleListMode}
      />

      <RightSidebar
        weeklyNotes={currentWeekData.weeklyNotes}
        missionStatement={missionStatement}
        onUpdateWeeklyNotes={updateWeeklyNotes}
        onOpenMissionSettings={() => setShowMissionSettings(true)}
      />

      {showSettings && (
        <SharpenTheSawSettings
          areas={sharpenTheSawAreas}
          onClose={() => setShowSettings(false)}
          onUpdateAreas={updateSharpenTheSawAreas}
        />
      )}

      {showMissionSettings && (
        <MissionStatementModal
          missionStatement={missionStatement}
          onClose={() => setShowMissionSettings(false)}
          onSave={updateMissionStatement}
        />
      )}

      {isTourActive && (
        <OnboardingTour
          steps={ONBOARDING_STEPS}
          onFinish={() => setIsTourActive(false)}
          onSkip={() => setIsTourActive(false)}
        />
      )}
    </div>
  );
}

export default Dashboard;
