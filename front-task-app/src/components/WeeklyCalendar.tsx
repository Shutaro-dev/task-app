import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent, CSSProperties, DragEvent as ReactDragEvent, MouseEvent as ReactMouseEvent } from 'react';
import type { DayNotes, ScheduledTask } from '../types/index';
import { cx } from '../utils/cx';
import styles from './WeeklyCalendar.module.css';

const DISPLAY_HOURS = [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24];
const DAY_NAMES = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function pad(n: number): string {
  return n.toString().padStart(2, '0');
}

interface WeeklyCalendarProps {
  currentWeek: Date;
  scheduledTasks: ScheduledTask[];
  dayNotes: DayNotes[];
  roleColors: Record<string, string>;
  isListMode: boolean;
  onWeekChange: (newWeek: Date) => void;
  onTaskDrop: (day: number, startTime: string) => void;
  onUpdateDayNotes: (day: number, notes: string) => void;
  onUpdateSleepTime: (day: number, sleepStart: string, sleepEnd: string) => void;
  onUpdateTask: (taskId: string, updates: Partial<ScheduledTask>) => void;
  onTaskDeleted: (taskId: string) => void;
  onAddCopiedTask: (task: ScheduledTask) => void;
  onDownloadPdf: () => void;
  onToggleListMode: () => void;
}

function WeeklyCalendar({
  currentWeek,
  scheduledTasks,
  dayNotes,
  roleColors,
  isListMode,
  onWeekChange,
  onTaskDrop,
  onUpdateDayNotes,
  onUpdateSleepTime,
  onUpdateTask,
  onTaskDeleted,
  onAddCopiedTask,
  onDownloadPdf,
  onToggleListMode,
}: WeeklyCalendarProps) {
  const [showSleepDialog, setShowSleepDialog] = useState(false);
  const [selectedDay, setSelectedDay] = useState(0);
  const [bedTime, setBedTime] = useState('22:00');
  const [wakeTime, setWakeTime] = useState('07:00');
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [showContextMenu, setShowContextMenu] = useState(false);
  const [menuX, setMenuX] = useState(0);
  const [menuY, setMenuY] = useState(0);
  const [copiedTask, setCopiedTask] = useState<ScheduledTask | null>(null);
  const [pasteTarget, setPasteTarget] = useState<{ dayIndex: number; hour: number } | null>(null);
  const [contextMenuType, setContextMenuType] = useState<'task' | 'paste' | ''>('task');
  const [dragTaskId, setDragTaskId] = useState<string | null>(null);
  const [dragOverTaskId, setDragOverTaskId] = useState<string | null>(null);

  const rootRef = useRef<HTMLDivElement>(null);

  const days = useMemo(() => {
    const result = [];
    for (let i = 0; i < 7; i++) {
      const date = new Date(currentWeek);
      date.setDate(date.getDate() + i);
      result.push({ name: DAY_NAMES[i], date, dayIndex: i });
    }
    return result;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentWeek.getTime()]);

  // クリックアウトサイドで選択解除
  useEffect(() => {
    const handler = (event: globalThis.MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setSelectedTaskId(null);
      }
    };
    document.addEventListener('click', handler);
    return () => document.removeEventListener('click', handler);
  }, []);

  const previousWeek = () => {
    const newWeek = new Date(currentWeek);
    newWeek.setDate(newWeek.getDate() - 7);
    onWeekChange(newWeek);
  };

  const nextWeek = () => {
    const newWeek = new Date(currentWeek);
    newWeek.setDate(newWeek.getDate() + 7);
    onWeekChange(newWeek);
  };

  const formatWeekStart = (date: Date): string =>
    date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

  const formatDate = (date: Date): string => date.getDate().toString();
  const formatHour = (hour: number): string => hour.toString();

  const getTasksForDay = (dayIndex: number): ScheduledTask[] =>
    scheduledTasks.filter(task => task.day === dayIndex);

  const getSortedTasksForDay = (dayIndex: number): ScheduledTask[] =>
    [...getTasksForDay(dayIndex)].sort((a, b) => a.startTime.localeCompare(b.startTime));

  const slotTimeForIndex = (index: number): string => {
    const totalMinutes = Math.min(23 * 60 + 30, 6 * 60 + index * 30);
    const hour = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return `${pad(hour)}:${pad(minutes)}`;
  };

  const nextListSlotTime = (dayIndex: number): string => slotTimeForIndex(getTasksForDay(dayIndex).length);

  const addTaskToDayList = (dayIndex: number) => {
    onTaskDrop(dayIndex, nextListSlotTime(dayIndex));
  };

  const handleDrop = (dayIndex: number, event: ReactDragEvent<HTMLDivElement>) => {
    event.preventDefault();

    if (isListMode) {
      addTaskToDayList(dayIndex);
      return;
    }

    const rect = event.currentTarget.getBoundingClientRect();
    const y = event.clientY - rect.top;
    const hourHeight = rect.height / DISPLAY_HOURS.length;
    const hourIndex = Math.floor(y / hourHeight);
    const minutes = Math.floor(((y % hourHeight) / hourHeight) * 60);

    const actualHour = DISPLAY_HOURS[hourIndex];
    const startTime = `${pad(actualHour)}:${pad(minutes)}`;

    onTaskDrop(dayIndex, startTime);
  };

  const toggleCompleted = (task: ScheduledTask) => {
    onUpdateTask(task.id, { completed: !task.completed });
  };

  // ── リストモード：ドラッグによる並び替え ──────────
  const onListTaskDragStart = (task: ScheduledTask, event: ReactDragEvent<HTMLDivElement>) => {
    setDragTaskId(task.id);
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', task.id);
  };

  const onListTaskDragOver = (task: ScheduledTask) => {
    if (dragTaskId && dragTaskId !== task.id) {
      setDragOverTaskId(task.id);
    }
  };

  const onListTaskDragLeave = (task: ScheduledTask) => {
    if (dragOverTaskId === task.id) {
      setDragOverTaskId(null);
    }
  };

  const reorderListTasks = (dayIndex: number, draggedId: string, targetId: string) => {
    const ordered = getSortedTasksForDay(dayIndex);
    const fromIndex = ordered.findIndex(t => t.id === draggedId);
    const toIndex = ordered.findIndex(t => t.id === targetId);
    if (fromIndex === -1 || toIndex === -1) return;

    const [moved] = ordered.splice(fromIndex, 1);
    ordered.splice(toIndex, 0, moved);

    ordered.forEach((t, index) => {
      const newStartTime = slotTimeForIndex(index);
      if (t.startTime !== newStartTime) {
        onUpdateTask(t.id, { startTime: newStartTime });
      }
    });
  };

  const onListTaskDrop = (dayIndex: number, targetTask: ScheduledTask, event: ReactDragEvent<HTMLDivElement>) => {
    event.preventDefault();
    if (dragTaskId && dragTaskId !== targetTask.id) {
      reorderListTasks(dayIndex, dragTaskId, targetTask.id);
    } else if (!dragTaskId) {
      addTaskToDayList(dayIndex);
    }
    setDragTaskId(null);
    setDragOverTaskId(null);
  };

  const onListTaskDragEnd = () => {
    setDragTaskId(null);
    setDragOverTaskId(null);
  };

  const darkenColor = (hex: string): string => {
    const r = Math.max(0, parseInt(hex.slice(1, 3), 16) - 40);
    const g = Math.max(0, parseInt(hex.slice(3, 5), 16) - 40);
    const b = Math.max(0, parseInt(hex.slice(5, 7), 16) - 40);
    return `rgb(${r},${g},${b})`;
  };

  const getTaskStyle = (task: ScheduledTask): CSSProperties => {
    const [hours, minutes] = task.startTime.split(':').map(Number);

    const displayStartHour = hours === 0 ? 24 : hours;
    const displayStartMinutes = (displayStartHour - 5) * 60 + minutes;
    const totalDisplayMinutes = DISPLAY_HOURS.length * 60;

    const top = (displayStartMinutes / totalDisplayMinutes) * 100;
    const height = (task.duration / totalDisplayMinutes) * 100;

    const bg = roleColors[task.roleId] || '#4a90d9';
    return {
      top: `${Math.max(0, Math.min(100, top))}%`,
      height: `${Math.max(1, Math.min(100 - top, height))}%`,
      left: '2px',
      right: '2px',
      backgroundColor: bg,
      borderColor: darkenColor(bg),
    };
  };

  const getDayNotes = (dayIndex: number): string => dayNotes.find(dn => dn.day === dayIndex)?.notes || '';

  const updateNotes = (dayIndex: number, event: ChangeEvent<HTMLTextAreaElement>) => {
    onUpdateDayNotes(dayIndex, event.target.value);
  };

  const openSleepDialog = (dayIndex: number) => {
    setSelectedDay(dayIndex);
    const dayNote = dayNotes.find(dn => dn.day === dayIndex);
    setBedTime(dayNote?.sleepStart || '22:00');
    setWakeTime(dayNote?.sleepEnd || '07:00');
    setShowSleepDialog(true);
  };

  const closeSleepDialog = () => setShowSleepDialog(false);

  const snapTo30 = (time: string): string => {
    const [h, m] = time.split(':').map(Number);
    const snapped = m < 15 ? 0 : m < 45 ? 30 : 0;
    const hour = m >= 45 ? (h + 1) % 24 : h;
    return `${pad(hour)}:${pad(snapped)}`;
  };

  const saveSleepTime = () => {
    onUpdateSleepTime(selectedDay, snapTo30(bedTime), snapTo30(wakeTime));
    closeSleepDialog();
  };

  const getSleepInfo = (dayIndex: number): string => {
    const dayNote = dayNotes.find(dn => dn.day === dayIndex);
    if (dayNote?.sleepStart && dayNote?.sleepEnd) {
      return `${dayNote.sleepEnd}-${dayNote.sleepStart}`;
    }
    return '';
  };

  const selectTask = (task: ScheduledTask) => setSelectedTaskId(task.id);

  const startTaskResize = (task: ScheduledTask, event: ReactMouseEvent) => {
    if ((event.target as HTMLElement).classList.contains(styles['resize-handle'])) return;

    setSelectedTaskId(task.id);
    setIsDragging(true);
    const startY = event.clientY;
    const original = { startTime: task.startTime, duration: task.duration };

    const handleMove = (e: globalThis.MouseEvent) => {
      const deltaY = e.clientY - startY;
      const hourHeight = 26;
      const minutesDelta = Math.round((deltaY / hourHeight) * 60);

      const [originalHours, originalMinutes] = original.startTime.split(':').map(Number);
      const originalTotalMinutes = originalHours * 60 + originalMinutes;
      const newTotalMinutes = Math.max(
        5 * 60,
        Math.min(24 * 60 - original.duration, originalTotalMinutes + minutesDelta)
      );

      const newHours = Math.floor(newTotalMinutes / 60);
      const newMinutes = newTotalMinutes % 60;
      onUpdateTask(task.id, { startTime: `${pad(newHours)}:${pad(newMinutes)}` });
    };

    const handleUp = () => {
      setIsDragging(false);
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleUp);
    };

    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleUp);
  };

  const startResize = (task: ScheduledTask, type: 'top' | 'bottom', event: ReactMouseEvent) => {
    event.preventDefault();
    setSelectedTaskId(task.id);
    const startY = event.clientY;
    const original = { startTime: task.startTime, duration: task.duration };

    const handleMove = (e: globalThis.MouseEvent) => {
      const deltaY = e.clientY - startY;
      const hourHeight = 26;
      const minutesDelta = Math.round((deltaY / hourHeight) * 60);

      if (type === 'bottom') {
        const newDuration = Math.max(15, original.duration + minutesDelta);
        onUpdateTask(task.id, { duration: newDuration });
      } else {
        const [originalHours, originalMinutes] = original.startTime.split(':').map(Number);
        const originalTotalMinutes = originalHours * 60 + originalMinutes;

        const newStartMinutes = Math.max(5 * 60, originalTotalMinutes + minutesDelta);
        const newDuration = Math.max(15, original.duration - minutesDelta);

        const newHours = Math.floor(newStartMinutes / 60);
        const newMinutes = newStartMinutes % 60;

        onUpdateTask(task.id, { startTime: `${pad(newHours)}:${pad(newMinutes)}`, duration: newDuration });
      }
    };

    const handleUp = () => {
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', handleUp);
    };

    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', handleUp);
  };

  const closeContextMenu = () => {
    setShowContextMenu(false);
    document.removeEventListener('click', closeContextMenu);
  };

  const onTaskRightClick = (task: ScheduledTask, event: ReactMouseEvent) => {
    setSelectedTaskId(task.id);
    setMenuX(event.clientX);
    setMenuY(event.clientY);
    setShowContextMenu(true);
    setContextMenuType('task');
    document.addEventListener('click', closeContextMenu);
  };

  const onEmptySlotRightClick = (dayIndex: number, hour: number, event: ReactMouseEvent) => {
    if (!copiedTask) return;
    setPasteTarget({ dayIndex, hour });
    setMenuX(event.clientX);
    setMenuY(event.clientY);
    setShowContextMenu(true);
    setContextMenuType('paste');
    document.addEventListener('click', closeContextMenu);
  };

  const copyTask = (task: ScheduledTask | null) => {
    if (!task) return;
    setCopiedTask({ ...task });
    setShowContextMenu(false);
  };

  const pasteTask = () => {
    if (!copiedTask || !pasteTarget) return;
    const newTask: ScheduledTask = {
      ...copiedTask,
      id: Date.now().toString(),
      day: pasteTarget.dayIndex,
      startTime: `${pad(pasteTarget.hour)}:00`,
    };
    onAddCopiedTask(newTask);
    setShowContextMenu(false);
    setPasteTarget(null);
  };

  const deleteTask = (task: ScheduledTask | null) => {
    if (!task) return;
    onTaskDeleted(task.id);
    setShowContextMenu(false);
  };

  const emitDownload = () => onDownloadPdf();

  const selectedTask = scheduledTasks.find(t => t.id === selectedTaskId) ?? null;

  return (
    <div ref={rootRef} className={cx(styles['weekly-calendar'], isDragging && styles['is-dragging-task'])}>
      <div className={styles['calendar-header']}>
        <div className={styles['week-navigation']}>
          <button onClick={previousWeek} className={styles['nav-btn']}>‹</button>
          <div className={styles['week-display']}>
            <span className={styles['week-text']}>Week of {formatWeekStart(currentWeek)}</span>
          </div>
          <button onClick={nextWeek} className={styles['nav-btn']}>›</button>
        </div>
        <label className={styles['list-mode-toggle']}>
          <input type="checkbox" checked={isListMode} onChange={() => onToggleListMode()} />
          <span className={styles['toggle-slider']}></span>
          <span className={styles['toggle-label']}>リスト表示</span>
        </label>
        <button className={styles['pdf-btn']} onClick={(e) => { e.stopPropagation(); emitDownload(); }}>PDF Download</button>
      </div>

      <div className={styles['calendar-content']}>
        {/* 常に固定表示されるヘッダー行 */}
        <div className={styles['calendar-day-headers']}>
          {!isListMode && <div className={styles['time-column-header']}></div>}
          {days.map((day, dayIndex) => (
            <div key={dayIndex} className={styles['day-header']} onClick={() => openSleepDialog(dayIndex)}>
              <div className={styles['day-name']}>{day.name}</div>
              <div className={styles['day-date']}>{formatDate(day.date)}</div>
              {getSleepInfo(dayIndex) && (
                <div className={styles['sleep-indicator']}>💤 {getSleepInfo(dayIndex)}</div>
              )}
            </div>
          ))}
        </div>

        {/* スクロール可能なタイムライン本体 */}
        <div className={styles['calendar-timeline-body']}>
          {!isListMode && (
            <div className={styles['time-slots-column']}>
              {DISPLAY_HOURS.map(hour => (
                <div key={hour} className={styles['time-slot']}>{formatHour(hour)}</div>
              ))}
            </div>
          )}

          {days.map((_day, dayIndex) => (
            <div
              key={dayIndex}
              className={cx(styles['day-timeline'], isListMode && styles['day-list'])}
              onDrop={(e) => handleDrop(dayIndex, e)}
              onDragOver={(e) => e.preventDefault()}
            >
              {!isListMode ? (
                <>
                  {DISPLAY_HOURS.map(hour => (
                    <div
                      key={hour}
                      className={styles['hour-slot']}
                      data-hour={hour}
                      onContextMenu={(e) => { e.preventDefault(); onEmptySlotRightClick(dayIndex, hour, e); }}
                    ></div>
                  ))}

                  {getTasksForDay(dayIndex).map(task => (
                    <div
                      key={task.id}
                      className={cx(styles['scheduled-task'], selectedTask?.id === task.id && styles.selected)}
                      style={getTaskStyle(task)}
                      onMouseDown={(e) => startTaskResize(task, e)}
                      onClick={() => selectTask(task)}
                      onContextMenu={(e) => { e.preventDefault(); onTaskRightClick(task, e); }}
                    >
                      <div className={styles['task-content']}>{task.title}</div>
                      <div
                        className={cx(styles['resize-handle'], styles['resize-handle-top'])}
                        onMouseDown={(e) => { e.stopPropagation(); startResize(task, 'top', e); }}
                      ></div>
                      <div
                        className={cx(styles['resize-handle'], styles['resize-handle-bottom'])}
                        onMouseDown={(e) => { e.stopPropagation(); startResize(task, 'bottom', e); }}
                      ></div>
                    </div>
                  ))}
                </>
              ) : (
                <>
                  {getSortedTasksForDay(dayIndex).map(task => (
                    <div
                      key={task.id}
                      className={cx(
                        styles['list-task'],
                        selectedTask?.id === task.id && styles.selected,
                        task.completed && styles.completed,
                        dragOverTaskId === task.id && styles['drag-over']
                      )}
                      draggable
                      onClick={() => selectTask(task)}
                      onContextMenu={(e) => { e.preventDefault(); onTaskRightClick(task, e); }}
                      onDragStart={(e) => onListTaskDragStart(task, e)}
                      onDragOver={(e) => { e.preventDefault(); onListTaskDragOver(task); }}
                      onDragLeave={() => onListTaskDragLeave(task)}
                      onDrop={(e) => { e.stopPropagation(); onListTaskDrop(dayIndex, task, e); }}
                      onDragEnd={onListTaskDragEnd}
                    >
                      <input
                        type="checkbox"
                        className={styles['list-task-checkbox']}
                        checked={!!task.completed}
                        onClick={(e) => e.stopPropagation()}
                        onChange={() => toggleCompleted(task)}
                      />
                      <span className={styles['list-task-title']}>{task.title}</span>
                    </div>
                  ))}
                  {getSortedTasksForDay(dayIndex).length === 0 && (
                    <div className={styles['list-empty-hint']}>ドラッグしてタスクを追加</div>
                  )}
                </>
              )}
            </div>
          ))}
        </div>

        {/* 常に固定表示されるノートセクション */}
        <div className={styles['calendar-notes-section']}>
          {/* リストモードでは時間軸列が無いため、他の行と列幅を揃えるためにこのラベル列も非表示にする */}
          {!isListMode && <div className={styles['notes-time-label']}>Notes</div>}
          {days.map((_day, dayIndex) => (
            <div key={dayIndex} className={styles['day-notes']}>
              <textarea
                value={getDayNotes(dayIndex)}
                onChange={(e) => updateNotes(dayIndex, e)}
                placeholder="Daily notes..."
                className={styles['notes-input']}
              ></textarea>
            </div>
          ))}
        </div>
      </div>

      {showSleepDialog && (
        <div className={styles['sleep-dialog-overlay']} onClick={closeSleepDialog}>
          <div className={styles['sleep-dialog']} onClick={(e) => e.stopPropagation()}>
            <h3>睡眠時間の入力（{days[selectedDay]?.name}）</h3>
            <div className={styles['sleep-inputs']}>
              <div className={styles['sleep-input-group']}>
                <label>起床時間:</label>
                <input
                  type="time"
                  value={wakeTime}
                  step={1800}
                  onChange={(e) => setWakeTime(e.target.value)}
                  onKeyUp={(e) => { if (e.key === 'Enter') saveSleepTime(); }}
                />
              </div>
              <div className={styles['sleep-input-group']}>
                <label>就寝時間:</label>
                <input
                  type="time"
                  value={bedTime}
                  step={1800}
                  onChange={(e) => setBedTime(e.target.value)}
                  onKeyUp={(e) => { if (e.key === 'Enter') saveSleepTime(); }}
                />
              </div>
            </div>
            <div className={styles['dialog-actions']}>
              <button onClick={saveSleepTime} className={styles['confirm-btn']}>保存</button>
              <button onClick={closeSleepDialog} className={styles['cancel-btn']}>キャンセル</button>
            </div>
          </div>
        </div>
      )}

      {showContextMenu && (
        <div
          style={{ position: 'absolute', top: menuY, left: menuX, zIndex: 1000 }}
          className={styles['context-menu']}
        >
          {contextMenuType === 'task' && (
            <ul>
              <li onClick={() => deleteTask(selectedTask)}>削除</li>
              <li onClick={() => copyTask(selectedTask)}>コピー</li>
            </ul>
          )}
          {contextMenuType === 'paste' && (
            <ul>
              <li onClick={pasteTask}>貼り付け</li>
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

export default WeeklyCalendar;
