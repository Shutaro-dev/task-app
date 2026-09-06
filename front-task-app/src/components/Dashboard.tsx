import { useEffect, useMemo, useRef, useState } from 'react';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import type { Role, Task, SharpenTheSawArea, WeekData, ScheduledTask } from '../types';
import LeftSidebar from './LeftSidebar';
import WeeklyCalendar from './WeeklyCalendar';
import RightSidebar from './RightSidebar';
import SharpenTheSawSettings from './SharpenTheSawSettings';
import styles from './Dashboard.module.css';

const STORAGE_KEY = 'fourth-gen-time-management';
const ROLE_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085'];

const DEFAULT_ROLES: Role[] = [
  {
    id: '1',
    name: 'Professional',
    isExpanded: false,
    tasks: [
      { id: 't1', title: 'Review quarterly goals', roleId: '1', isPermanent: true },
      { id: 't2', title: 'Team meeting preparation', roleId: '1', isPermanent: false },
    ],
  },
  {
    id: '2',
    name: 'Family',
    isExpanded: false,
    tasks: [
      { id: 't3', title: 'Quality time with children', roleId: '2', isPermanent: true },
      { id: 't4', title: 'Plan weekend activities', roleId: '2', isPermanent: false },
    ],
  },
];

const DEFAULT_SAW_AREAS: SharpenTheSawArea[] = [
  { id: 'physical', name: 'Physical', icon: '💪', tasks: [] },
  { id: 'mental', name: 'Intellectual', icon: '🧠', tasks: [] },
  { id: 'social-emotional', name: 'Social/Emotional', icon: '❤️', tasks: [] },
  { id: 'spiritual', name: 'Spiritual', icon: '🙏', tasks: [] },
];

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

interface InitialState {
  currentWeek: Date;
  roles: Role[];
  sharpenTheSawAreas: SharpenTheSawArea[];
  isListMode: boolean;
  weekData: Map<string, WeekData>;
}

function loadInitialState(): InitialState {
  const defaults: InitialState = {
    currentWeek: getStartOfWeek(new Date()),
    roles: DEFAULT_ROLES,
    sharpenTheSawAreas: DEFAULT_SAW_AREAS,
    isListMode: false,
    weekData: new Map<string, WeekData>(),
  };

  try {
    const savedData = localStorage.getItem(STORAGE_KEY);
    if (!savedData) return defaults;
    const parsed = JSON.parse(savedData);
    const result: InitialState = { ...defaults };

    if (parsed.currentWeek) {
      result.currentWeek = new Date(parsed.currentWeek);
    }

    if (parsed.roles) {
      result.roles = parsed.roles.map((r: any) => {
        const seen = new Set<string>();
        const tasks = (r.tasks ?? []).filter((t: any) => {
          if (!t.isPermanent || seen.has(t.id)) return false;
          seen.add(t.id);
          return true;
        });
        return { ...r, tasks };
      });
    }

    if (parsed.sharpenTheSawAreas) {
      result.sharpenTheSawAreas = parsed.sharpenTheSawAreas;
    }

    if (typeof parsed.isListMode === 'boolean') {
      result.isListMode = parsed.isListMode;
    }

    if (parsed.weekData) {
      result.weekData = new Map(
        parsed.weekData.map(([key, value]: [string, any]) => [
          key,
          { ...value, weekStart: new Date(value.weekStart) },
        ])
      );
    }

    return result;
  } catch (error) {
    console.warn('Failed to load data from localStorage:', error);
    return defaults;
  }
}

function Dashboard() {
  // localStorage からの初回読み込みは一度だけ同期的に行う（Vue版の loadData() 相当）
  const initialDataRef = useRef<InitialState | null>(null);
  if (initialDataRef.current === null) {
    initialDataRef.current = loadInitialState();
  }
  const initial = initialDataRef.current;

  const [showSettings, setShowSettings] = useState(false);
  const [isListMode, setIsListMode] = useState(initial.isListMode); // ON時はカレンダーを時間非表示のチェックリスト表示にする
  const [currentWeek, setCurrentWeek] = useState<Date>(initial.currentWeek);
  const [roles, setRoles] = useState<Role[]>(initial.roles);
  const [sharpenTheSawAreas, setSharpenTheSawAreas] = useState<SharpenTheSawArea[]>(initial.sharpenTheSawAreas);
  const [weekData, setWeekData] = useState<Map<string, WeekData>>(initial.weekData);

  const draggedTaskRef = useRef<Task | null>(null);
  const dashboardRootRef = useRef<HTMLDivElement>(null);

  const roleColorMap = useMemo(() => {
    const map: Record<string, string> = {};
    roles.forEach(r => { map[r.id] = r.color || '#4a90d9'; });
    return map;
  }, [roles]);

  const weekKey = getWeekKey(currentWeek);

  const currentWeekData = useMemo<WeekData>(() => {
    const existing = weekData.get(weekKey);
    if (existing) return existing.temporaryTasks ? existing : { ...existing, temporaryTasks: [] };
    return blankWeekData(currentWeek);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [weekData, weekKey]);

  // 現在の週の WeekData がまだ存在しない場合は作成しておく
  useEffect(() => {
    setWeekData(prev => {
      if (prev.has(weekKey)) return prev;
      const next = new Map(prev);
      next.set(weekKey, blankWeekData(currentWeek));
      return next;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [weekKey]);

  // 自動保存 (Vue版で各メソッド末尾に書かれていた saveData() 呼び出しに相当)
  useEffect(() => {
    try {
      const dataToSave = {
        currentWeek: currentWeek.toISOString(),
        roles,
        sharpenTheSawAreas,
        isListMode,
        weekData: Array.from(weekData.entries()).map(([key, value]) => [
          key,
          { ...value, weekStart: value.weekStart.toISOString() },
        ]),
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(dataToSave));
    } catch (error) {
      console.warn('Failed to save data to localStorage:', error);
    }
  }, [currentWeek, roles, sharpenTheSawAreas, isListMode, weekData]);

  const updateWeekData = (key: string, weekStartForNew: Date, updater: (data: WeekData) => WeekData) => {
    setWeekData(prev => {
      const existing = prev.get(key) ?? blankWeekData(weekStartForNew);
      const withDefaults: WeekData = { ...existing, temporaryTasks: existing.temporaryTasks ?? [] };
      const updated = updater(withDefaults);
      const next = new Map(prev);
      next.set(key, updated);
      return next;
    });
  };
  const updateCurrentWeekData = (updater: (data: WeekData) => WeekData) =>
    updateWeekData(weekKey, currentWeek, updater);

  const changeWeek = (newWeek: Date) => {
    // 週切り替え時に全Role.tasksからisPermanent: falseのタスクを除外
    setRoles(prev => prev.map(role => ({ ...role, tasks: role.tasks.filter(task => task.isPermanent) })));
    setCurrentWeek(newWeek);
  };

  const addRole = (name: string) => {
    setRoles(prev => [
      ...prev,
      { id: Date.now().toString(), name, tasks: [], isExpanded: false, color: ROLE_COLORS[prev.length % ROLE_COLORS.length] },
    ]);
  };

  const deleteRole = (roleId: string) => {
    setRoles(prev => prev.filter(role => role.id !== roleId));
    // Also remove any scheduled tasks for this role
    setWeekData(prev => {
      const next = new Map<string, WeekData>();
      prev.forEach((wd, key) => {
        next.set(key, { ...wd, scheduledTasks: wd.scheduledTasks.filter(task => task.roleId !== roleId) });
      });
      return next;
    });
  };

  const addTask = (roleId: string, taskTitle: string, isPermanent: boolean) => {
    const newTask: Task = { id: Date.now().toString(), title: taskTitle, roleId, isPermanent };
    if (isPermanent) {
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: [...r.tasks, newTask] } : r)));
    } else {
      // 一時タスクは今週のWeekDataにTask型で追加
      updateCurrentWeekData(wd => ({ ...wd, temporaryTasks: [...(wd.temporaryTasks ?? []), newTask] }));
    }
  };

  const toggleRole = (roleId: string) => {
    setRoles(prev => prev.map(role => {
      if (role.id !== roleId) return role;
      const isExpanded = !role.isExpanded;
      // 役割を閉じる際にshowAddTaskをリセット
      return isExpanded ? { ...role, isExpanded } : { ...role, isExpanded, showAddTask: false };
    }));
  };

  const handleTaskDragStart = (task: Task) => {
    draggedTaskRef.current = task;
  };

  const handleTaskDrop = (day: number, startTime: string) => {
    const draggedTask = draggedTaskRef.current;
    if (!draggedTask) return;
    const scheduledTask: ScheduledTask = {
      id: Date.now().toString(),
      taskId: draggedTask.id,
      day,
      startTime,
      duration: 60, // Default 60 minutes
      title: draggedTask.title,
      roleId: draggedTask.roleId,
    };
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: [...wd.scheduledTasks, scheduledTask] }));
    draggedTaskRef.current = null;
  };

  const updateScheduledTask = (taskId: string, updates: Partial<ScheduledTask>) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      scheduledTasks: wd.scheduledTasks.map(t => (t.id === taskId ? { ...t, ...updates } : t)),
    }));
  };

  const updateDayNotes = (day: number, notes: string) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      dayNotes: wd.dayNotes.map(dn => (dn.day === day ? { ...dn, notes } : dn)),
    }));
  };

  const updateSleepTime = (day: number, sleepStart: string, sleepEnd: string) => {
    updateCurrentWeekData(wd => ({
      ...wd,
      dayNotes: wd.dayNotes.map(dn => (dn.day === day ? { ...dn, sleepStart, sleepEnd } : dn)),
    }));
  };

  const updateWeeklyNotes = (notes: string) => {
    updateCurrentWeekData(wd => ({ ...wd, weeklyNotes: notes }));
  };

  const updateSharpenTheSawAreas = (areas: SharpenTheSawArea[]) => {
    setSharpenTheSawAreas(areas);
  };

  const updateRoleName = (roleId: string, newName: string) => {
    setRoles(prev => prev.map(role => (role.id === roleId ? { ...role, name: newName } : role)));
  };

  const updateRoleColor = (roleId: string, color: string) => {
    setRoles(prev => prev.map(role => (role.id === roleId ? { ...role, color } : role)));
  };

  const updateTaskTitle = (roleId: string, taskId: string, newTitle: string) => {
    const role = roles.find(r => r.id === roleId);
    const taskInRole = role?.tasks.find(t => t.id === taskId);
    if (taskInRole) {
      setRoles(prev => prev.map(r => (
        r.id === roleId ? { ...r, tasks: r.tasks.map(t => (t.id === taskId ? { ...t, title: newTitle } : t)) } : r
      )));
      return;
    }
    const tempTask = currentWeekData.temporaryTasks?.find(t => t.id === taskId);
    if (tempTask) {
      updateCurrentWeekData(wd => ({
        ...wd,
        temporaryTasks: (wd.temporaryTasks ?? []).map(t => (t.id === taskId ? { ...t, title: newTitle } : t)),
      }));
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
  };

  const toggleTaskPermanent = (roleId: string, taskId: string, currentlyPermanent: boolean) => {
    if (currentlyPermanent) {
      const role = roles.find(r => r.id === roleId);
      const task = role?.tasks.find(t => t.id === taskId);
      if (!task) return;
      const movedTask: Task = { ...task, isPermanent: false };
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: r.tasks.filter(t => t.id !== taskId) } : r)));
      updateCurrentWeekData(wd => ({ ...wd, temporaryTasks: [...(wd.temporaryTasks ?? []), movedTask] }));
    } else {
      const task = currentWeekData.temporaryTasks?.find(t => t.id === taskId);
      if (!task) return;
      const movedTask: Task = { ...task, isPermanent: true };
      updateCurrentWeekData(wd => ({
        ...wd,
        temporaryTasks: (wd.temporaryTasks ?? []).filter(t => t.id !== taskId),
      }));
      setRoles(prev => prev.map(r => (r.id === roleId ? { ...r, tasks: [...r.tasks, movedTask] } : r)));
    }
  };

  const toggleListMode = () => setIsListMode(prev => !prev);

  const reorderRoles = (newRoles: Role[]) => {
    // newRoles は LeftSidebar の roleList 由来で merged tasks（permanent + temporary）を含む。
    // roles（permanent タスクのみ）を元に順序だけ入れ替えることで二重追加を防ぐ。
    setRoles(prev => newRoles.map(r => prev.find(existing => existing.id === r.id) ?? r));
  };

  const handleTaskDeleted = (taskId: string) => {
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: wd.scheduledTasks.filter(t => t.id !== taskId) }));
  };

  const deleteTask = (roleId: string, taskId: string) => {
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
  };

  const addCopiedTask = (task: ScheduledTask) => {
    updateCurrentWeekData(wd => ({ ...wd, scheduledTasks: [...wd.scheduledTasks, task] }));
  };

  const downloadPdf = async () => {
    const root = dashboardRootRef.current;
    if (!root) return;

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

      const canvas = await html2canvas(root, {
        scale: 2,
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

      // ページ向きをコンテンツのアスペクト比で自動選択
      const isLandscape = canvas.width >= canvas.height;
      const pdf = new jsPDF({ orientation: isLandscape ? 'landscape' : 'portrait', unit: 'pt', format: 'a4' });
      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();

      const imgWidth = canvas.width;
      const imgHeight = canvas.height;
      const ratio = Math.min(pageWidth / imgWidth, pageHeight / imgHeight);
      const renderWidth = imgWidth * ratio;
      const renderHeight = imgHeight * ratio;
      const offsetX = (pageWidth - renderWidth) / 2;
      const offsetY = (pageHeight - renderHeight) / 2;

      pdf.addImage(imgData, 'PNG', offsetX, offsetY, renderWidth, renderHeight);
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

  return (
    <div className={styles.dashboard} ref={dashboardRootRef}>
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
        onUpdateWeeklyNotes={updateWeeklyNotes}
      />

      {showSettings && (
        <SharpenTheSawSettings
          areas={sharpenTheSawAreas}
          onClose={() => setShowSettings(false)}
          onUpdateAreas={updateSharpenTheSawAreas}
        />
      )}
    </div>
  );
}

export default Dashboard;
