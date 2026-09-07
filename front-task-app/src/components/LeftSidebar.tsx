import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent, KeyboardEvent, MouseEvent as ReactMouseEvent } from 'react';
import Sortable from 'sortablejs';
import type { Role, SharpenTheSawArea, Task } from '../types';
import { cx } from '../utils/cx';
import styles from './LeftSidebar.module.css';

const PRESET_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085', '#f39c12', '#2c3e50'];

function arrayMove<T>(arr: T[], from: number, to: number): T[] {
  const copy = [...arr];
  const [item] = copy.splice(from, 1);
  copy.splice(to, 0, item);
  return copy;
}

interface LeftSidebarProps {
  roles: Role[];
  sharpenTheSawAreas: SharpenTheSawArea[];
  temporaryTasks: Task[];
  onAddRole: (name: string) => void;
  onAddTask: (roleId: string, title: string, isPermanent: boolean) => void;
  onToggleRole: (roleId: string) => void;
  onTaskDragStart: (task: Task) => void;
  onOpenSettings: () => void;
  onDeleteRole: (roleId: string) => void;
  onUpdateRoleName: (roleId: string, newName: string) => void;
  onDeleteTask: (roleId: string, taskId: string) => void;
  onUpdateRoleColor: (roleId: string, color: string) => void;
  onUpdateTaskTitle: (roleId: string, taskId: string, newTitle: string) => void;
  onReorderTasks: (roleId: string, reorderedTasks: Task[]) => void;
  onToggleTaskPermanent: (roleId: string, taskId: string, currentlyPermanent: boolean) => void;
  onReorderRoles: (newRoles: Role[]) => void;
  userLabel?: string;
  onLogout?: () => void;
}

function LeftSidebar({
  roles,
  sharpenTheSawAreas,
  temporaryTasks,
  onAddRole,
  onAddTask,
  onToggleRole,
  onTaskDragStart,
  onOpenSettings,
  onDeleteRole,
  onUpdateRoleName,
  onDeleteTask,
  onUpdateRoleColor,
  onUpdateTaskTitle,
  onReorderTasks,
  onToggleTaskPermanent,
  onReorderRoles,
  userLabel,
  onLogout,
}: LeftSidebarProps) {
  const [showAddRole, setShowAddRole] = useState(false);
  const [newRoleName, setNewRoleName] = useState('');
  const [addTaskOpenRoleId, setAddTaskOpenRoleId] = useState<string | null>(null);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskIsPermanent, setNewTaskIsPermanent] = useState(false);
  const [editingRoleId, setEditingRoleId] = useState<string | null>(null);
  const [editingRoleName, setEditingRoleName] = useState('');
  const [editingTaskId, setEditingTaskId] = useState<string | null>(null);
  const [editingTaskTitle, setEditingTaskTitle] = useState('');
  const [colorPickerOpenId, setColorPickerOpenId] = useState<string | null>(null);
  const [draggingRoleId, setDraggingRoleId] = useState<string | null>(null);
  const [draggingTaskId, setDraggingTaskId] = useState<string | null>(null);
  const [roleList, setRoleList] = useState<Role[]>([]);
  const [taskListsByRole, setTaskListsByRole] = useState<Record<string, Task[]>>({});

  const addRoleEnterCount = useRef(0);
  const editRoleEnterCount = useRef(0);
  const addTaskEnterCount = useRef(0);
  const isComposing = useRef(false);
  const isDraggingRole = useRef(false);
  const isDraggingTask = useRef(false);
  const taskHandlePending = useRef(false);

  const roleInputRef = useRef<HTMLInputElement>(null);
  const editRoleInputRef = useRef<HTMLInputElement>(null);
  const editTaskInputRef = useRef<HTMLInputElement>(null);
  const rolesContainerRef = useRef<HTMLDivElement>(null);
  const taskSortableInstances = useRef<Record<string, Sortable>>({});

  const roleListRef = useRef(roleList);
  const taskListsByRoleRef = useRef(taskListsByRole);
  useEffect(() => { roleListRef.current = roleList; }, [roleList]);
  useEffect(() => { taskListsByRoleRef.current = taskListsByRole; }, [taskListsByRole]);

  const mergedRoles = useMemo<Role[]>(() => roles.map(role => ({
    ...role,
    tasks: [...role.tasks, ...temporaryTasks.filter(t => t.roleId === role.id)],
  })), [roles, temporaryTasks]);

  // props → ローカルの並び替え用配列に同期 (ドラッグ中は上書きしない)
  useEffect(() => {
    if (isDraggingRole.current || isDraggingTask.current) return;
    setRoleList(mergedRoles.map(r => ({ ...r, tasks: [...r.tasks] })));
    const newTaskLists: Record<string, Task[]> = {};
    mergedRoles.forEach(r => { newTaskLists[r.id] = [...r.tasks]; });
    setTaskListsByRole(newTaskLists);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mergedRoles]);

  useEffect(() => {
    if (showAddRole) roleInputRef.current?.focus();
  }, [showAddRole]);

  useEffect(() => {
    if (editingRoleId !== null) editRoleInputRef.current?.focus();
  }, [editingRoleId]);

  useEffect(() => {
    if (editingTaskId !== null) editTaskInputRef.current?.focus();
  }, [editingTaskId]);

  // ── ロール ドラッグ並び替え (SortableJS) ───────────
  useEffect(() => {
    if (!rolesContainerRef.current) return;
    const sortable = Sortable.create(rolesContainerRef.current, {
      handle: '.role-drag-handle',
      animation: 200,
      ghostClass: 'role-ghost',
      onStart: (evt) => {
        isDraggingRole.current = true;
        setDraggingRoleId(roleListRef.current[evt.oldIndex!]?.id ?? null);
      },
      onEnd: (evt) => {
        isDraggingRole.current = false;
        setDraggingRoleId(null);
        const { oldIndex, newIndex } = evt;
        if (oldIndex == null || newIndex == null || oldIndex === newIndex) {
          onReorderRoles(roleListRef.current);
          return;
        }
        const updated = arrayMove(roleListRef.current, oldIndex, newIndex);
        setRoleList(updated);
        onReorderRoles(updated);
      },
    });
    return () => sortable.destroy();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── タスク ドラッグ並び替え (SortableJS, ロール展開中のみ) ──
  // ロールIDごとにコールバックの参照を固定し、無関係な再レンダーで
  // Sortable インスタンスが破棄・再生成されない（=ドラッグ中断されない）ようにする
  const taskSortableCallbacks = useRef<Record<string, (el: HTMLDivElement | null) => void>>({});
  const getTaskSortableCallback = (roleId: string) => {
    if (taskSortableCallbacks.current[roleId]) return taskSortableCallbacks.current[roleId];
    const callback = (el: HTMLDivElement | null) => {
    const existing = taskSortableInstances.current[roleId];
    if (el) {
      if (existing) return;
      const instance = Sortable.create(el, {
        handle: '.task-drag-handle',
        animation: 200,
        ghostClass: 'task-ghost',
        onStart: (evt) => {
          isDraggingTask.current = true;
          setDraggingTaskId(taskListsByRoleRef.current[roleId]?.[evt.oldIndex!]?.id ?? null);
        },
        onEnd: (evt) => {
          isDraggingTask.current = false;
          taskHandlePending.current = false;
          setDraggingTaskId(null);
          const { oldIndex, newIndex } = evt;
          const current = taskListsByRoleRef.current[roleId] ?? [];
          if (oldIndex == null || newIndex == null || oldIndex === newIndex) {
            onReorderTasks(roleId, current);
            return;
          }
          const updated = arrayMove(current, oldIndex, newIndex);
          setTaskListsByRole(prev => ({ ...prev, [roleId]: updated }));
          onReorderTasks(roleId, updated);
        },
      });
      taskSortableInstances.current[roleId] = instance;
    } else if (existing) {
      existing.destroy();
      delete taskSortableInstances.current[roleId];
    }
    };
    taskSortableCallbacks.current[roleId] = callback;
    return callback;
  };

  // ── ロール追加 ──────────────────────────────────────
  const openAddRole = () => {
    setNewRoleName('');
    addRoleEnterCount.current = 0;
    setShowAddRole(true);
  };
  const submitAddRole = () => {
    if (!newRoleName.trim()) return;
    onAddRole(newRoleName.trim());
    setNewRoleName('');
    setShowAddRole(false);
    addRoleEnterCount.current = 0;
  };
  const onRoleInputEnter = () => {
    if (isComposing.current) return;
    addRoleEnterCount.current += 1;
    if (addRoleEnterCount.current < 2) return;
    submitAddRole();
  };
  const cancelAddRole = () => {
    setNewRoleName('');
    setShowAddRole(false);
    addRoleEnterCount.current = 0;
  };
  const deleteRole = (roleId: string) => {
    if (confirm('Are you sure you want to delete this role and all its tasks?')) onDeleteRole(roleId);
  };

  // ── タスク追加 ──────────────────────────────────────
  const startAddTask = (roleId: string) => {
    setAddTaskOpenRoleId(roleId);
  };
  const handleAddTask = (roleId: string) => {
    if (!newTaskTitle.trim()) return;
    addTaskEnterCount.current += 1;
    if (addTaskEnterCount.current < 2) return;
    onAddTask(roleId, newTaskTitle.trim(), newTaskIsPermanent);
    setNewTaskTitle('');
    setNewTaskIsPermanent(false);
    setAddTaskOpenRoleId(null);
    addTaskEnterCount.current = 0;
  };
  const cancelAddTask = () => {
    setNewTaskTitle('');
    setNewTaskIsPermanent(false);
    setAddTaskOpenRoleId(null);
    addTaskEnterCount.current = 0;
  };
  const deleteTask = (roleId: string, taskId: string) => {
    setNewTaskTitle('');
    setNewTaskIsPermanent(false);
    addTaskEnterCount.current = 0;
    onDeleteTask(roleId, taskId);
  };
  const resetEnterCount = () => { addTaskEnterCount.current = 0; };

  // ── カレンダードラッグ (タスク本体 → WeeklyCalendar) ──
  const onTaskCalendarDragStart = (task: Task) => {
    if (taskHandlePending.current) {
      taskHandlePending.current = false;
      return;
    }
    onTaskDragStart(task);
  };

  // ── ロール名編集 ────────────────────────────────────
  const startEditRole = (role: Role) => {
    setEditingRoleId(role.id);
    setEditingRoleName(role.name);
    editRoleEnterCount.current = 0;
  };
  const confirmEditRole = (role: Role) => {
    if (!editingRoleName.trim() || editingRoleName === role.name) {
      cancelEditRole();
      return;
    }
    editRoleEnterCount.current += 1;
    if (editRoleEnterCount.current < 2) return;
    onUpdateRoleName(role.id, editingRoleName.trim());
    cancelEditRole();
  };
  const cancelEditRole = () => {
    setEditingRoleId(null);
    setEditingRoleName('');
    editRoleEnterCount.current = 0;
  };

  // ── タスク名編集 ────────────────────────────────────
  const startEditTask = (task: Task) => {
    setEditingTaskId(task.id);
    setEditingTaskTitle(task.title);
  };
  const confirmEditTask = (task: Task, roleId: string) => {
    if (editingTaskTitle.trim() && editingTaskTitle !== task.title) {
      onUpdateTaskTitle(roleId, task.id, editingTaskTitle.trim());
    }
    setEditingTaskId(null);
    setEditingTaskTitle('');
  };
  const cancelEditTask = () => {
    setEditingTaskId(null);
    setEditingTaskTitle('');
  };

  // ── P/T 切り替え ────────────────────────────────────
  const toggleTaskPermanent = (task: Task, roleId: string) => {
    const label = task.isPermanent ? '一時タスク(T)' : '永続タスク(P)';
    if (confirm(`このタスクを${label}に変更しますか？`)) {
      onToggleTaskPermanent(roleId, task.id, task.isPermanent);
    }
  };

  // ── カラーピッカー ──────────────────────────────────
  const toggleColorPicker = (roleId: string) => {
    setColorPickerOpenId(prev => (prev === roleId ? null : roleId));
  };
  const closeColorPicker = () => setColorPickerOpenId(null);
  const selectColor = (roleId: string, color: string) => {
    onUpdateRoleColor(roleId, color);
    setColorPickerOpenId(null);
  };

  const handleToggleRole = (roleId: string) => {
    const role = roleList.find(r => r.id === roleId);
    if (role?.isExpanded && addTaskOpenRoleId === roleId) {
      setAddTaskOpenRoleId(null);
    }
    onToggleRole(roleId);
  };

  const stop = (e: ReactMouseEvent) => e.stopPropagation();

  return (
    <div className={styles['left-sidebar']} onClick={closeColorPicker}>
      {/* ── Sharpen the Saw ────────────────────────────── */}
      <div className={styles['sharpen-summary']}>
        <div className={styles['sharpen-header']}>
          <h3>Sharpen the Saw</h3>
          <i className={cx('bi', 'bi-gear', styles['settings-btn-small'])} onClick={onOpenSettings}></i>
        </div>
        <div className={styles['saw-areas']}>
          {sharpenTheSawAreas.map(area => (
            <div key={area.id} className={styles['saw-area']}>
              <span className={styles['saw-icon']}>{area.icon}</span>
              <div className={styles['saw-content']}>
                <span className={styles['saw-name']}>{area.name}</span>
                {area.tasks && area.tasks.length > 0 && (
                  <div className={styles['saw-tasks']}>
                    {area.tasks.slice(0, 4).map(task => (
                      <div key={task.id} className={styles['saw-task']}>{task.title}</div>
                    ))}
                    {area.tasks.length > 4 && (
                      <div className={styles['saw-task-more']}>+{area.tasks.length - 4} 他</div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Roles section ─────────────────────────────── */}
      <div className={styles['roles-section']}>
        <div className={styles['section-header']}>
          <h3>Roles and Goals</h3>
          <button onClick={openAddRole} className={styles['add-btn']}>Add Role</button>
        </div>

        {showAddRole && (
          <div className={styles['add-role-form']}>
            <input
              value={newRoleName}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setNewRoleName(e.target.value)}
              onKeyUp={(e: KeyboardEvent<HTMLInputElement>) => { if (e.key === 'Enter') onRoleInputEnter(); }}
              onCompositionStart={() => { isComposing.current = true; }}
              onCompositionEnd={() => { isComposing.current = false; }}
              placeholder="Role name"
              className={styles['role-input']}
              ref={roleInputRef}
            />
            <div className={styles['form-actions']}>
              <button onClick={submitAddRole} className={styles['confirm-btn']}>Add</button>
              <button onClick={cancelAddRole} className={styles['cancel-btn']}>Cancel</button>
            </div>
          </div>
        )}

        {/* ロール並び替え: SortableJS */}
        <div ref={rolesContainerRef} className={styles['roles-list']}>
          {roleList.map(role => (
            <div
              key={role.id}
              className={cx(styles['role-item'], draggingRoleId === role.id && styles['is-dragging'])}
            >
              <div className={styles['role-header']} onClick={() => handleToggleRole(role.id)}>
                {/* ロール並び替えハンドル */}
                <span
                  className={cx(styles['drag-handle'], styles['role-drag-handle'], 'role-drag-handle')}
                  onClick={stop}
                  title="ドラッグして並び替え"
                >⠿</span>
                {/* 開閉シェブロン */}
                <span className={cx(styles['expand-chevron'], role.isExpanded && styles.expanded)}>›</span>

                {/* カラーピッカー */}
                <span className={styles['color-picker-wrapper']} onClick={stop}>
                  <span
                    className={styles['role-color-swatch']}
                    style={{ backgroundColor: role.color || '#4a90d9' }}
                    onClick={() => toggleColorPicker(role.id)}
                  ></span>
                  {colorPickerOpenId === role.id && (
                    <div className={styles['color-popover']}>
                      {PRESET_COLORS.map(c => (
                        <span
                          key={c}
                          className={cx(styles['preset-color'], (role.color || '#4a90d9') === c && styles.selected)}
                          style={{ backgroundColor: c }}
                          onClick={() => selectColor(role.id, c)}
                        ></span>
                      ))}
                    </div>
                  )}
                </span>

                {editingRoleId !== role.id ? (
                  <span className={styles['role-name']}>{role.name}</span>
                ) : (
                  <input
                    value={editingRoleName}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setEditingRoleName(e.target.value)}
                    onKeyUp={(e: KeyboardEvent<HTMLInputElement>) => {
                      if (e.key === 'Enter') confirmEditRole(role);
                      else if (e.key === 'Escape') cancelEditRole();
                    }}
                    onBlur={() => confirmEditRole(role)}
                    className={styles['edit-role-input']}
                    ref={editRoleInputRef}
                  />
                )}
                <span className={styles['task-count']}>({role.tasks.length})</span>
                <button onClick={(e) => { stop(e); deleteRole(role.id); }} className={styles['delete-role-btn']}>×</button>
                <button onClick={(e) => { stop(e); startEditRole(role); }} className={styles['edit-role-btn']}>✏️</button>
              </div>

              {role.isExpanded && (
                <div className={styles['role-content']}>
                  {/* タスク並び替え: SortableJS */}
                  <div ref={getTaskSortableCallback(role.id)} className={styles['tasks-list']}>
                    {(taskListsByRole[role.id] || []).map(task => (
                      <div
                        key={task.id}
                        className={cx(styles['task-item'], draggingTaskId === task.id && styles['is-dragging'])}
                        draggable
                        onDragStart={() => onTaskCalendarDragStart(task)}
                      >
                        {/* タスク並び替えハンドル (mousedown で calendar drag と分離) */}
                        <span
                          className={cx(styles['drag-handle'], styles['task-drag-handle'], 'task-drag-handle')}
                          onClick={stop}
                          onMouseDown={(e) => { e.stopPropagation(); taskHandlePending.current = true; }}
                          title="ドラッグして並び替え"
                        >⠿</span>

                        {editingTaskId === task.id ? (
                          <input
                            value={editingTaskTitle}
                            onChange={(e: ChangeEvent<HTMLInputElement>) => setEditingTaskTitle(e.target.value)}
                            className={styles['edit-task-input']}
                            onKeyUp={(e: KeyboardEvent<HTMLInputElement>) => {
                              if (e.key === 'Enter') confirmEditTask(task, role.id);
                              else if (e.key === 'Escape') cancelEditTask();
                            }}
                            onBlur={() => confirmEditTask(task, role.id)}
                            onClick={stop}
                            ref={editTaskInputRef}
                          />
                        ) : (
                          <span
                            className={styles['task-title']}
                            onDoubleClick={(e) => { e.stopPropagation(); startEditTask(task); }}
                          >{task.title}</span>
                        )}

                        <span
                          className={cx(styles['task-type'], task.isPermanent ? styles['badge-permanent'] : styles['badge-temporary'])}
                          onClick={(e) => { stop(e); toggleTaskPermanent(task, role.id); }}
                          title="クリックで P/T 切り替え"
                        >{task.isPermanent ? 'P' : 'T'}</span>
                        <button
                          className={styles['edit-task-btn']}
                          onClick={(e) => { stop(e); startEditTask(task); }}
                          title="タスク名を編集"
                        >✏️</button>
                        <button onClick={(e) => { stop(e); deleteTask(role.id, task.id); }} className={styles['delete-task-btn']}>×</button>
                      </div>
                    ))}
                  </div>

                  <div className={styles['add-task-section']}>
                    {addTaskOpenRoleId === role.id ? (
                      <div className={styles['add-task-form']}>
                        <input
                          value={newTaskTitle}
                          onChange={(e: ChangeEvent<HTMLInputElement>) => { setNewTaskTitle(e.target.value); resetEnterCount(); }}
                          onKeyUp={(e: KeyboardEvent<HTMLInputElement>) => { if (e.key === 'Enter') handleAddTask(role.id); }}
                          placeholder="Task title"
                          className={styles['task-input']}
                        />
                        <div className={styles['task-type-toggle']}>
                          <label>
                            <input
                              type="checkbox"
                              checked={newTaskIsPermanent}
                              onChange={(e) => setNewTaskIsPermanent(e.target.checked)}
                            />
                            Permanent
                          </label>
                        </div>
                        <div className={styles['form-actions']}>
                          <button onClick={() => handleAddTask(role.id)} className={styles['confirm-btn']}>Add</button>
                          <button onClick={cancelAddTask} className={styles['cancel-btn']}>Cancel</button>
                        </div>
                      </div>
                    ) : (
                      <button onClick={() => startAddTask(role.id)} className={styles['add-task-btn']}>
                        + Add Task
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {userLabel && onLogout && (
        <div className={styles['account-bar']} onClick={stop}>
          <span className={styles['account-email']} title={userLabel}>{userLabel}</span>
          <button className={styles['logout-btn']} onClick={onLogout}>ログアウト</button>
        </div>
      )}
    </div>
  );
}

export default LeftSidebar;
