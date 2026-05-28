<template>
  <div class="left-sidebar" @click="closeColorPicker">
    <!-- ── Sharpen the Saw ────────────────────────────── -->
    <div class="sharpen-summary">
      <div class="sharpen-header">
        <h3>Sharpen the Saw</h3>
        <i class="bi bi-gear settings-btn-small" @click="$emit('open-settings')"></i>
      </div>
      <div class="saw-areas">
        <div v-for="area in sharpenTheSawAreas" :key="area.id" class="saw-area">
          <span class="saw-icon">{{ area.icon }}</span>
          <div class="saw-content">
            <span class="saw-name">{{ area.name }}</span>
            <div v-if="area.tasks && area.tasks.length > 0" class="saw-tasks">
              <div v-for="task in area.tasks.slice(0, 4)" :key="task.id" class="saw-task">
                {{ task.title }}
              </div>
              <div v-if="area.tasks.length > 4" class="saw-task-more">
                +{{ area.tasks.length - 4 }} 他
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Roles section ─────────────────────────────── -->
    <div class="roles-section">
      <div class="section-header">
        <h3>Roles and Goals</h3>
        <button @click="openAddRole" class="add-btn">Add Role</button>
      </div>

      <div v-if="showAddRole" class="add-role-form">
        <input
          v-model="newRoleName"
          @keyup.enter="onRoleInputEnter"
          @compositionstart="isComposing = true"
          @compositionend="isComposing = false"
          placeholder="Role name"
          class="role-input"
          ref="roleInput"
        />
        <div class="form-actions">
          <button @click="submitAddRole" class="confirm-btn">Add</button>
          <button @click="cancelAddRole" class="cancel-btn">Cancel</button>
        </div>
      </div>

      <!-- ロール並び替え: SortableJS (vue-draggable-next) -->
      <VueDraggableNext
        :list="roleList"
        @start="onRoleDragStart"
        @end="onRoleDragEnd"
        handle=".role-drag-handle"
        item-key="id"
        :animation="200"
        ghost-class="role-ghost"
        tag="div"
        class="roles-list"
      >
        <div
          v-for="role in roleList"
          :key="role.id"
          class="role-item"
          :class="{ 'is-dragging': draggingRoleId === role.id }"
        >
          <div class="role-header" @click="$emit('toggle-role', role.id)">
            <!-- ロール並び替えハンドル -->
            <span
              class="drag-handle role-drag-handle"
              @click.stop
              title="ドラッグして並び替え"
            >⠿</span>
            <!-- 開閉シェブロン -->
            <span class="expand-chevron" :class="{ expanded: role.isExpanded }">›</span>

            <!-- カラーピッカー -->
            <span class="color-picker-wrapper" @click.stop>
              <span
                class="role-color-swatch"
                :style="{ backgroundColor: role.color || '#4a90d9' }"
                @click="toggleColorPicker(role.id)"
              ></span>
              <div v-if="colorPickerOpenId === role.id" class="color-popover">
                <span
                  v-for="c in presetColors"
                  :key="c"
                  class="preset-color"
                  :style="{ backgroundColor: c }"
                  :class="{ selected: (role.color || '#4a90d9') === c }"
                  @click="selectColor(role.id, c)"
                ></span>
              </div>
            </span>

            <span v-if="editingRoleId !== role.id" class="role-name">{{ role.name }}</span>
            <input
              v-else
              v-model="editingRoleName"
              @keyup.enter="confirmEditRole(role)"
              @keyup.esc="cancelEditRole"
              @blur="confirmEditRole(role)"
              class="edit-role-input"
            />
            <span class="task-count">({{ role.tasks.length }})</span>
            <button @click.stop="deleteRole(role.id)" class="delete-role-btn">×</button>
            <button @click.stop="startEditRole(role)" class="edit-role-btn">✏️</button>
          </div>

          <div v-if="role.isExpanded" class="role-content">
            <!-- タスク並び替え: ロールと同じ native DnD -->
            <VueDraggableNext
              :list="taskListsByRole[role.id] || []"
              @start="(e) => onTaskDragStart(e, role.id)"
              @end="() => onTaskDragEnd(role.id)"
              handle=".task-drag-handle"
              item-key="id"
              :animation="200"
              ghost-class="task-ghost"
              tag="div"
              class="tasks-list"
            >
              <div
                v-for="task in taskListsByRole[role.id] || []"
                :key="task.id"
                class="task-item"
                :class="{ 'is-dragging': draggingTaskId === task.id }"
                draggable="true"
                @dragstart="onTaskCalendarDragStart(task)"
              >
                <!-- タスク並び替えハンドル (mousedown で calendar drag と分離) -->
                <span
                  class="drag-handle task-drag-handle"
                  @click.stop
                  @mousedown.stop="taskHandlePending = true"
                  title="ドラッグして並び替え"
                >⠿</span>

                <template v-if="editingTaskId === task.id">
                  <input
                    v-model="editingTaskTitle"
                    class="edit-task-input"
                    @keyup.enter="confirmEditTask(task, role.id)"
                    @keyup.esc="cancelEditTask"
                    @blur="confirmEditTask(task, role.id)"
                    @click.stop
                  />
                </template>
                <template v-else>
                  <span class="task-title" @dblclick.stop="startEditTask(task)">{{ task.title }}</span>
                </template>

                <span
                  class="task-type"
                  :class="task.isPermanent ? 'badge-permanent' : 'badge-temporary'"
                  @click.stop="toggleTaskPermanent(task, role.id)"
                  title="クリックで P/T 切り替え"
                >{{ task.isPermanent ? 'P' : 'T' }}</span>
                <button
                  class="edit-task-btn"
                  @click.stop="startEditTask(task)"
                  title="タスク名を編集"
                >✏️</button>
                <button @click.stop="deleteTask(role.id, task.id)" class="delete-task-btn">×</button>
              </div>
            </VueDraggableNext>

            <div class="add-task-section">
              <div v-if="role.showAddTask" class="add-task-form">
                <input
                  v-model="newTaskTitle"
                  @keyup.enter="handleAddTask(role.id)"
                  @input="resetEnterCount"
                  placeholder="Task title"
                  class="task-input"
                />
                <div class="task-type-toggle">
                  <label>
                    <input type="checkbox" v-model="newTaskIsPermanent" />
                    Permanent
                  </label>
                </div>
                <div class="form-actions">
                  <button @click="handleAddTask(role.id)" class="confirm-btn">Add</button>
                  <button @click="cancelAddTask(role.id)" class="cancel-btn">Cancel</button>
                </div>
              </div>
              <button v-else @click="startAddTask(role.id)" class="add-task-btn">
                + Add Task
              </button>
            </div>
          </div>
        </div>
      </VueDraggableNext>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import type { PropType } from 'vue';
import { VueDraggableNext } from 'vue-draggable-next';
import type { Role, Task, SharpenTheSawArea } from '../types';

const PRESET_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085', '#f39c12', '#2c3e50'];

export default defineComponent({
  name: 'LeftSidebar',
  components: { VueDraggableNext },
  props: {
    roles:              { type: Array as PropType<Role[]>,              required: true },
    sharpenTheSawAreas: { type: Array as PropType<SharpenTheSawArea[]>, required: true },
    temporaryTasks:     { type: Array as PropType<Task[]>,              required: false, default: () => [] },
  },
  emits: [
    'add-role', 'add-task', 'toggle-role', 'task-drag-start', 'open-settings',
    'delete-role', 'update-role-name', 'delete-task',
    'update-role-color', 'update-task-title', 'reorder-tasks',
    'toggle-task-permanent', 'reorder-roles',
  ],
  data() {
    return {
      showAddRole:        false,
      newRoleName:        '',
      newTaskTitle:       '',
      newTaskIsPermanent: false,
      editingRoleId:      null as string | null,
      editingRoleName:    '',
      editingTaskId:      null as string | null,
      editingTaskTitle:   '',
      addRoleEnterCount:  0,
      editRoleEnterCount: 0,
      addTaskEnterCount:  0,
      // IME composition state
      isComposing:        false,
      // カラーピッカー
      colorPickerOpenId:  null as string | null,
      // ── SortableJS ドラッグ状態 ──────────────────────
      draggingRoleId:      null as string | null,
      draggingTaskId:      null as string | null,
      isDraggingRole:      false,
      isDraggingTask:      false,
      taskHandlePending:   false,  // handle mousedown → calendar drag をブロック
      // ライブ配列 (SortableJS が直接変更する)
      roleList:           [] as any[],
      taskListsByRole:    {} as Record<string, Task[]>,
    };
  },
  computed: {
    presetColors(): string[] { return PRESET_COLORS; },

    mergedRoles(): any[] {
      return this.roles.map(role => ({
        ...role,
        tasks: [
          ...role.tasks,
          ...this.temporaryTasks.filter(t => t.roleId === role.id),
        ],
        showAddTask: role.showAddTask ?? false,
      }));
    },
  },
  watch: {
    mergedRoles: {
      deep: true,
      handler() {
        if (!this.isDraggingRole && !this.isDraggingTask) {
          this.syncFromProps();
        }
      },
    },
  },
  created() {
    this.syncFromProps();
  },
  methods: {
    syncFromProps() {
      this.roleList = this.mergedRoles.map((r: any) => ({ ...r, tasks: [...r.tasks] }));
      const newTaskLists: Record<string, Task[]> = {};
      this.mergedRoles.forEach((r: any) => { newTaskLists[r.id] = [...r.tasks]; });
      this.taskListsByRole = newTaskLists;
    },

    // ── ロール追加 ──────────────────────────────────────
    openAddRole() {
      this.newRoleName = '';
      this.addRoleEnterCount = 0;
      this.showAddRole = true;
      this.$nextTick(() => {
        (this.$refs.roleInput as HTMLInputElement | null)?.focus();
      });
    },
    onRoleInputEnter() {
      if (this.isComposing) return;
      if (++this.addRoleEnterCount < 2) return;
      this.submitAddRole();
    },
    submitAddRole() {
      if (!this.newRoleName.trim()) return;
      this.$emit('add-role', this.newRoleName.trim());
      this.newRoleName = ''; this.showAddRole = false; this.addRoleEnterCount = 0;
    },
    cancelAddRole() { this.newRoleName = ''; this.showAddRole = false; this.addRoleEnterCount = 0; },
    deleteRole(roleId: string) {
      if (confirm('Are you sure you want to delete this role and all its tasks?'))
        this.$emit('delete-role', roleId);
    },

    // ── タスク追加 ──────────────────────────────────────
    startAddTask(roleId: string) {
      this.roles.forEach(r => { if (r.id !== roleId) r.showAddTask = false; });
      const r = this.roles.find(r => r.id === roleId);
      if (r) r.showAddTask = true;
    },
    handleAddTask(roleId: string) {
      if (!this.newTaskTitle.trim()) return;
      if (++this.addTaskEnterCount < 2) return;
      this.$emit('add-task', roleId, this.newTaskTitle.trim(), this.newTaskIsPermanent);
      this.newTaskTitle = ''; this.newTaskIsPermanent = false;
      const r = this.roles.find(r => r.id === roleId);
      if (r) r.showAddTask = false;
      this.addTaskEnterCount = 0;
    },
    cancelAddTask(roleId: string) {
      this.newTaskTitle = ''; this.newTaskIsPermanent = false;
      const r = this.roles.find(r => r.id === roleId);
      if (r) r.showAddTask = false;
      this.addTaskEnterCount = 0;
    },
    deleteTask(roleId: string, taskId: string) {
      this.newTaskTitle = ''; this.newTaskIsPermanent = false; this.addTaskEnterCount = 0;
      this.$emit('delete-task', roleId, taskId);
    },
    resetEnterCount() { this.addTaskEnterCount = 0; },

    // ── カレンダードラッグ (タスク本体 → WeeklyCalendar) ──
    onTaskCalendarDragStart(task: Task) {
      if (this.taskHandlePending) { this.taskHandlePending = false; return; }
      this.$emit('task-drag-start', task);
    },

    // ── ロール名編集 ────────────────────────────────────
    startEditRole(role: any) {
      this.editingRoleId = role.id; this.editingRoleName = role.name; this.editRoleEnterCount = 0;
      this.$nextTick(() => {
        (this.$el.querySelector('.edit-role-input') as HTMLInputElement | null)?.focus();
      });
    },
    confirmEditRole(role: any) {
      if (!this.editingRoleName.trim() || this.editingRoleName === role.name) {
        this.cancelEditRole(); return;
      }
      if (++this.editRoleEnterCount < 2) return;
      this.$emit('update-role-name', role.id, this.editingRoleName.trim());
      this.cancelEditRole();
    },
    cancelEditRole() {
      this.editingRoleId = null; this.editingRoleName = ''; this.editRoleEnterCount = 0;
    },

    // ── タスク名編集 ────────────────────────────────────
    startEditTask(task: Task) {
      this.editingTaskId = task.id; this.editingTaskTitle = task.title;
      this.$nextTick(() => {
        (this.$el.querySelector('.edit-task-input') as HTMLInputElement | null)?.focus();
      });
    },
    confirmEditTask(task: Task, roleId: string) {
      if (this.editingTaskTitle.trim() && this.editingTaskTitle !== task.title)
        this.$emit('update-task-title', roleId, task.id, this.editingTaskTitle.trim());
      this.editingTaskId = null; this.editingTaskTitle = '';
    },
    cancelEditTask() { this.editingTaskId = null; this.editingTaskTitle = ''; },

    // ── P/T 切り替え ────────────────────────────────────
    toggleTaskPermanent(task: Task, roleId: string) {
      const label = task.isPermanent ? '一時タスク(T)' : '永続タスク(P)';
      if (confirm(`このタスクを${label}に変更しますか？`))
        this.$emit('toggle-task-permanent', roleId, task.id, task.isPermanent);
    },

    // ── カラーピッカー ──────────────────────────────────
    toggleColorPicker(roleId: string) {
      this.colorPickerOpenId = this.colorPickerOpenId === roleId ? null : roleId;
    },
    closeColorPicker() { this.colorPickerOpenId = null; },
    selectColor(roleId: string, color: string) {
      this.$emit('update-role-color', roleId, color); this.colorPickerOpenId = null;
    },

    // ── ロール ドラッグ並び替え (SortableJS) ───────────
    onRoleDragStart(event: any) {
      this.isDraggingRole = true;
      this.draggingRoleId = this.roleList[event.oldIndex]?.id ?? null;
    },
    onRoleDragEnd() {
      this.isDraggingRole = false;
      this.$emit('reorder-roles', this.roleList);
      this.draggingRoleId = null;
    },

    // ── タスク ドラッグ並び替え (SortableJS) ───────────
    onTaskDragStart(event: any, roleId: string) {
      this.isDraggingTask = true;
      this.draggingTaskId = this.taskListsByRole[roleId]?.[event.oldIndex]?.id ?? null;
    },
    onTaskDragEnd(roleId: string) {
      this.isDraggingTask = false;
      this.taskHandlePending = false;
      this.$emit('reorder-tasks', roleId, this.taskListsByRole[roleId] ?? []);
      this.draggingTaskId = null;
    },
  },
  mounted() {
    this.$nextTick(() => {
      if (this.showAddRole && this.$refs.roleInput)
        (this.$refs.roleInput as HTMLInputElement).focus();
    });
  },
});
</script>

<style scoped>
/* ── Layout ───────────────────────────────────── */
.left-sidebar {
  width: 320px; background-color: white;
  border-right: 1px solid #e0e0e0;
  display: flex; flex-direction: column; overflow: hidden;
}

/* ── Sharpen the Saw ──────────────────────────── */
.sharpen-summary { padding: 16px; border-bottom: 1px solid #e0e0e0; background-color: #f8f9fa; }
.sharpen-header  { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.sharpen-summary h3 { margin: 0; font-size: 14px; font-weight: 600; color: #333; }
.settings-btn-small { padding: 4px 6px; font-size: 12px; cursor: pointer; color: #666; }
.settings-btn-small:hover { color: #333; }
.saw-areas { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.saw-area  { display: flex; align-items: flex-start; gap: 6px; padding: 8px; border-radius: 4px; background: white; cursor: pointer; min-height: 50px; }
.saw-area:hover { background-color: #f0f0f0; }
.saw-icon  { font-size: 16px; flex-shrink: 0; margin-top: 1px; }
.saw-content { flex: 1; min-width: 0; }
.saw-name  { font-size: 12px; color: #333; font-weight: 600; }
.saw-tasks { margin-top: 2px; }
.saw-task  { font-size: 9px; color: #888; line-height: 1.2; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.saw-task-more { font-size: 8px; color: #aaa; font-style: italic; margin-top: 2px; }

/* ── Roles section ────────────────────────────── */
.roles-section { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.section-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid #e0e0e0; }
.section-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #333; }
.add-btn { background: none; border: 1px solid #ccc; padding: 4px 8px; border-radius: 3px; font-size: 12px; cursor: pointer; }
.add-btn:hover { background-color: #f0f0f0; border-color: #999; }
.add-role-form { padding: 16px; border-bottom: 1px solid #e0e0e0; background-color: #f8f9fa; }
.role-input, .task-input {
  width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 3px;
  font-size: 14px; margin-bottom: 8px; box-sizing: border-box;
}
.form-actions { display: flex; gap: 8px; }
.confirm-btn, .cancel-btn { padding: 4px 12px; border: 1px solid #ccc; border-radius: 3px; font-size: 12px; cursor: pointer; }
.confirm-btn { background-color: #007bff; color: white; border-color: #007bff; }
.confirm-btn:hover { background-color: #0056b3; }
.cancel-btn { background: white; }
.cancel-btn:hover { background-color: #f0f0f0; }

/* ── ロールリスト ─────────────────────────────── */
.roles-list { flex: 1; overflow-y: auto; }

.role-item {
  border-bottom: 1px solid #e0e0e0;
}
.role-item.is-dragging { opacity: 0.4; }

/* SortableJS ghost (ドラッグ中のプレースホルダー) */
:global(.role-ghost) {
  opacity: 0.25;
  background: #e3eeff;
  border: 1px dashed #4a90d9;
}

/* ── ロールヘッダー ────────────────────────────── */
.role-header {
  display: flex; align-items: center; padding: 10px 10px;
  cursor: pointer; gap: 4px; transition: background-color 0.15s;
}
.role-header:hover { background-color: #f8f9fa; }

/* 開閉シェブロン */
.expand-chevron {
  font-size: 14px; color: #aaa; flex-shrink: 0;
  display: inline-block; transition: transform 0.18s ease;
  transform: rotate(0deg); line-height: 1; user-select: none;
}
.expand-chevron.expanded { transform: rotate(90deg); }

.role-name { flex: 1; font-weight: 500; color: #333; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-count { font-size: 11px; color: #999; flex-shrink: 0; }
.delete-role-btn {
  background: none; border: 1px solid #dc3545; color: #dc3545;
  width: 16px; height: 16px; border-radius: 50%; cursor: pointer; font-size: 10px;
  flex-shrink: 0; display: flex; align-items: center; justify-content: center; padding: 0;
  line-height: 1;
}
.delete-role-btn:hover { background-color: #dc3545; color: white; }
.edit-role-btn { background: none; border: none; cursor: pointer; font-size: 13px; flex-shrink: 0; padding: 0; line-height: 1; }
.edit-role-btn:hover { opacity: 0.65; }
.edit-role-input { font-size: 0.9em; padding: 2px 6px; flex: 1; min-width: 0; border: 1px solid #007bff; border-radius: 3px; }

/* ── ドラッグハンドル ─────────────────────────── */
.drag-handle {
  color: #ccc; font-size: 14px; cursor: grab; flex-shrink: 0;
  user-select: none; padding: 0 1px; letter-spacing: -1px;
}
.drag-handle:hover  { color: #999; }
.drag-handle:active { cursor: grabbing; }
.task-drag-handle   { font-size: 12px; }

/* ── カラーピッカー ───────────────────────────── */
.color-picker-wrapper { position: relative; flex-shrink: 0; }
.role-color-swatch {
  display: block; width: 14px; height: 14px; border-radius: 50%;
  border: 1.5px solid rgba(0,0,0,0.12); cursor: pointer;
  transition: transform 0.15s;
}
.role-color-swatch:hover { transform: scale(1.28); }
.color-popover {
  position: absolute; top: 22px; left: -4px; z-index: 200;
  background: white; border: 1px solid #ddd; border-radius: 8px;
  padding: 7px; display: flex; flex-wrap: wrap; gap: 5px; width: 121px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.14);
}
.preset-color {
  display: block; width: 22px; height: 22px; border-radius: 50%;
  cursor: pointer; border: 2px solid transparent;
  transition: transform 0.12s, border-color 0.12s;
}
.preset-color:hover    { transform: scale(1.18); }
.preset-color.selected { border-color: #333; }

/* ── タスクリスト ─────────────────────────────── */
.role-content { background-color: #fafafa; }
.tasks-list   { padding: 4px 12px 0; }

.task-item {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px; margin-bottom: 4px;
  background-color: white; border: 1px solid #e0e0e0; border-radius: 4px;
  transition: box-shadow 0.15s;
}
.task-item:hover { border-color: #bbb; box-shadow: 0 1px 4px rgba(0,0,0,0.07); }
.task-item.is-dragging { opacity: 0.4; }

/* SortableJS ghost (タスク) */
:global(.task-ghost) {
  opacity: 0.2;
  background: #e3eeff;
  border: 1px dashed #4a90d9 !important;
}

.task-title {
  flex: 1; font-size: 13px; color: #333; cursor: text;
  min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.task-type {
  width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;
  border-radius: 50%; font-size: 10px; font-weight: bold; flex-shrink: 0;
}
.badge-permanent { background-color: #cce5ff; color: #004085; cursor: pointer; }
.badge-temporary  { background-color: #fff3cd; color: #856404; cursor: pointer; }

/* タスク編集ボタン (ホバー時に表示) */
.edit-task-btn {
  background: none; border: none; font-size: 11px; line-height: 1;
  cursor: pointer; flex-shrink: 0; padding: 0 1px;
  opacity: 0; transition: opacity 0.15s;
}
.task-item:hover .edit-task-btn { opacity: 1; }

.delete-task-btn {
  background: none; border: none; color: #ccc; font-size: 14px;
  cursor: pointer; flex-shrink: 0; padding: 0 2px; line-height: 1;
}
.delete-task-btn:hover { color: #dc3545; }

.edit-task-input {
  flex: 1; font-size: 13px; padding: 2px 4px; min-width: 0;
  border: 1px solid #007bff; border-radius: 3px; outline: none;
}

/* ── タスク追加 ──────────────────────────────── */
.add-task-section { padding: 6px 12px 10px; }
.add-task-form { background: white; padding: 10px; border-radius: 4px; border: 1px solid #e0e0e0; }
.task-type-toggle { margin-bottom: 8px; }
.task-type-toggle label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #666; cursor: pointer; }
.add-task-btn { width: 100%; padding: 6px; background: none; border: 1px dashed #ccc; border-radius: 3px; font-size: 12px; color: #999; cursor: pointer; }
.add-task-btn:hover { border-color: #007bff; color: #007bff; }
</style>
