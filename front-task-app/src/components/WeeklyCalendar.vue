<template>
  <div class="weekly-calendar" :class="{ 'is-dragging-task': isDragging }">
    <div class="calendar-header">
      <div class="week-navigation">
        <button @click="previousWeek" class="nav-btn">‹</button>
        <div class="week-display">
          <span class="week-text">Week of {{ formatWeekStart(currentWeek) }}</span>
        </div>
        <button @click="nextWeek" class="nav-btn">›</button>
      </div>
      <label class="list-mode-toggle">
        <input
          type="checkbox"
          :checked="isListMode"
          @change="$emit('toggle-list-mode')"
        />
        <span class="toggle-slider"></span>
        <span class="toggle-label">リスト表示</span>
      </label>
      <button class="pdf-btn" @click.stop="emitDownload">PDF Download</button>
    </div>
    
    <div class="calendar-content">
      <!-- 常に固定表示されるヘッダー行 -->
      <div class="calendar-day-headers">
        <div v-if="!isListMode" class="time-column-header"></div>
        <div
          v-for="(_day, dayIndex) in days"
          :key="dayIndex"
          class="day-header"
          @click="openSleepDialog(dayIndex)"
        >
          <div class="day-name">{{ _day.name }}</div>
          <div class="day-date">{{ formatDate(_day.date) }}</div>
          <div v-if="getSleepInfo(dayIndex)" class="sleep-indicator">
            💤 {{ getSleepInfo(dayIndex) }}
          </div>
        </div>
      </div>

      <!-- スクロール可能なタイムライン本体（リストモード時は時間軸を出さずチェックリスト表示） -->
      <div class="calendar-timeline-body">
        <div v-if="!isListMode" class="time-slots-column">
          <div
            v-for="hour in displayHours"
            :key="hour"
            class="time-slot"
          >
            {{ formatHour(hour) }}
          </div>
        </div>

        <div
          v-for="(_day, dayIndex) in days"
          :key="dayIndex"
          class="day-timeline"
          :class="{ 'day-list': isListMode }"
          @drop="handleDrop(dayIndex, $event)"
          @dragover.prevent
        >
          <template v-if="!isListMode">
            <div
              v-for="hour in displayHours"
              :key="hour"
              class="hour-slot"
              :data-hour="hour"
              @contextmenu.prevent="onEmptySlotRightClick(dayIndex, hour, $event)"
            ></div>

            <div
              v-for="task in getTasksForDay(dayIndex)"
              :key="task.id"
              class="scheduled-task"
              :style="getTaskStyle(task)"
              @mousedown="startTaskResize(task, $event)"
              @click="selectTask(task)"
              @contextmenu.prevent="onTaskRightClick(task, $event)"
              :class="{ 'selected': selectedTask?.id === task.id }"
            >
              <div class="task-content">
                {{ task.title }}
              </div>
              <div class="resize-handle resize-handle-top" @mousedown.stop="startResize(task, 'top', $event)"></div>
              <div class="resize-handle resize-handle-bottom" @mousedown.stop="startResize(task, 'bottom', $event)"></div>
            </div>
          </template>

          <template v-else>
            <div
              v-for="task in getSortedTasksForDay(dayIndex)"
              :key="task.id"
              class="list-task"
              :class="{ 'selected': selectedTask?.id === task.id, 'completed': task.completed, 'drag-over': dragOverTaskId === task.id }"
              draggable="true"
              @click="selectTask(task)"
              @contextmenu.prevent="onTaskRightClick(task, $event)"
              @dragstart="onListTaskDragStart(task, $event)"
              @dragover.prevent="onListTaskDragOver(task)"
              @dragleave="onListTaskDragLeave(task)"
              @drop.stop="onListTaskDrop(dayIndex, task, $event)"
              @dragend="onListTaskDragEnd"
            >
              <input
                type="checkbox"
                class="list-task-checkbox"
                :checked="!!task.completed"
                @click.stop
                @change="toggleCompleted(task)"
              />
              <span class="list-task-title">{{ task.title }}</span>
            </div>
            <div v-if="getSortedTasksForDay(dayIndex).length === 0" class="list-empty-hint">
              ドラッグしてタスクを追加
            </div>
          </template>
        </div>
      </div>

      <!-- 常に固定表示されるノートセクション -->
      <div class="calendar-notes-section">
        <div class="notes-time-label">Notes</div>
        <div
          v-for="(_day, dayIndex) in days"
          :key="dayIndex"
          class="day-notes"
        >
          <textarea
            :value="getDayNotes(dayIndex)"
            @input="updateNotes(dayIndex, $event)"
            placeholder="Daily notes..."
            class="notes-input"
          ></textarea>
        </div>
      </div>
    </div>
    
    <div v-if="showSleepDialog" class="sleep-dialog-overlay" @click="closeSleepDialog">
      <div class="sleep-dialog" @click.stop>
        <h3>睡眠時間の入力（{{ days[selectedDay]?.name }}）</h3>
        <div class="sleep-inputs">
          <div class="sleep-input-group">
            <label>起床時間:</label>
            <input type="time" v-model="wakeTime" step="1800" @keyup.enter="saveSleepTime" />
          </div>
          <div class="sleep-input-group">
            <label>就寝時間:</label>
            <input type="time" v-model="bedTime" step="1800" @keyup.enter="saveSleepTime" />
          </div>
        </div>
        <div class="dialog-actions">
          <button @click="saveSleepTime" class="confirm-btn">保存</button>
          <button @click="closeSleepDialog" class="cancel-btn">キャンセル</button>
        </div>
      </div>
    </div>
    <div v-if="showContextMenu"
         :style="{ position: 'absolute', top: menuY + 'px', left: menuX + 'px', zIndex: 1000 }"
         class="context-menu">
      <ul v-if="contextMenuType === 'task'">
        <li @click="deleteTask(selectedTask)">削除</li>
        <li @click="copyTask(selectedTask)">コピー</li>
      </ul>
      <ul v-else-if="contextMenuType === 'paste'">
        <li @click="pasteTask">貼り付け</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import type { PropType } from 'vue';
import type { ScheduledTask, DayNotes } from '../types/index';

export default defineComponent({
  name: 'WeeklyCalendar',
  props: {
    currentWeek: {
      type: Date,
      required: true
    },
    scheduledTasks: {
      type: Array as PropType<ScheduledTask[]>,
      required: true
    },
    dayNotes: {
      type: Array as PropType<DayNotes[]>,
      required: true
    },
    roleColors: {
      type: Object as PropType<Record<string, string>>,
      default: () => ({})
    },
    isListMode: {
      type: Boolean,
      default: false
    }
  },
  emits: ['week-change', 'task-drop', 'update-day-notes', 'update-sleep-time', 'update-task', 'task-deleted', 'add-copied-task', 'download-pdf', 'toggle-list-mode'],
  data() {
    return {
      // 5:00 AM to 24:00 (next day 0:00) - 20 hours total
      displayHours: [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
      showSleepDialog: false,
      selectedDay: 0,
      bedTime: '22:00', // 就寝時間
      wakeTime: '07:00', // 起床時間
      selectedTask: null as ScheduledTask | null,
      isResizing: false,
      isDragging: false,
      resizeType: '' as 'top' | 'bottom' | '',
      dragStartY: 0,
      originalTaskData: null as any,
      showContextMenu: false,
      menuX: 0,
      menuY: 0,
      copiedTask: null as ScheduledTask | null,
      pasteTarget: null as { dayIndex: number, hour: number } | null,
      contextMenuType: 'task' as 'task' | 'paste' | '',
      // リストモードでの並び替えドラッグ状態
      dragTaskId: null as string | null,
      dragOverTaskId: null as string | null
    };
  },
  computed: {
    days() {
      const days = [];
      const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
      
      for (let i = 0; i < 7; i++) {
        const date = new Date(this.currentWeek);
        date.setDate(date.getDate() + i);
        
        days.push({
          name: dayNames[i],
          date: date,
          dayIndex: i
        });
      }
      
      return days;
    }
  },
  methods: {
    previousWeek() {
      const newWeek = new Date(this.currentWeek);
      newWeek.setDate(newWeek.getDate() - 7);
      this.$emit('week-change', newWeek);
    },
    
    nextWeek() {
      const newWeek = new Date(this.currentWeek);
      newWeek.setDate(newWeek.getDate() + 7);
      this.$emit('week-change', newWeek);
    },
    
    formatWeekStart(date: Date): string {
      return date.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric',
        year: 'numeric'
      });
    },
    
    formatDate(date: Date): string {
      return date.getDate().toString();
    },
    
    formatHour(hour: number): string {
      return hour.toString();
    },
    
    handleDrop(dayIndex: number, event: DragEvent) {
      event.preventDefault();

      if (this.isListMode) {
        // リストモードではY座標を使わず、その日の末尾に追加する
        this.addTaskToDayList(dayIndex);
        return;
      }

      const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
      const y = event.clientY - rect.top;
      const hourHeight = rect.height / this.displayHours.length;
      const hourIndex = Math.floor(y / hourHeight);
      const minutes = Math.floor((y % hourHeight) / hourHeight * 60);

      // Convert display hour index to actual hour (5 AM = index 0)
      const actualHour = this.displayHours[hourIndex];
      const startTime = `${actualHour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;

      this.$emit('task-drop', dayIndex, startTime);
    },

    slotTimeForIndex(index: number): string {
      // 時間軸を表示しないが、時間モードへ戻した時に重ならないよう30分刻みで割り当てる
      const totalMinutes = Math.min(23 * 60 + 30, 6 * 60 + index * 30);
      const hour = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;
      return `${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
    },

    nextListSlotTime(dayIndex: number): string {
      return this.slotTimeForIndex(this.getTasksForDay(dayIndex).length);
    },

    addTaskToDayList(dayIndex: number) {
      this.$emit('task-drop', dayIndex, this.nextListSlotTime(dayIndex));
    },

    getTasksForDay(dayIndex: number): ScheduledTask[] {
      return this.scheduledTasks.filter(task => task.day === dayIndex);
    },

    getSortedTasksForDay(dayIndex: number): ScheduledTask[] {
      return [...this.getTasksForDay(dayIndex)].sort((a, b) => a.startTime.localeCompare(b.startTime));
    },

    toggleCompleted(task: ScheduledTask) {
      this.$emit('update-task', task.id, { completed: !task.completed });
    },

    // ── リストモード：ドラッグによる並び替え ──────────
    onListTaskDragStart(task: ScheduledTask, event: DragEvent) {
      this.dragTaskId = task.id;
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', task.id);
      }
    },

    onListTaskDragOver(task: ScheduledTask) {
      if (this.dragTaskId && this.dragTaskId !== task.id) {
        this.dragOverTaskId = task.id;
      }
    },

    onListTaskDragLeave(task: ScheduledTask) {
      if (this.dragOverTaskId === task.id) {
        this.dragOverTaskId = null;
      }
    },

    onListTaskDrop(dayIndex: number, targetTask: ScheduledTask, event: DragEvent) {
      event.preventDefault();
      if (this.dragTaskId && this.dragTaskId !== targetTask.id) {
        this.reorderListTasks(dayIndex, this.dragTaskId, targetTask.id);
      } else if (!this.dragTaskId) {
        // サイドバーからの新規タスクが既存タスクの上にドロップされた場合も追加として扱う
        this.addTaskToDayList(dayIndex);
      }
      this.dragTaskId = null;
      this.dragOverTaskId = null;
    },

    onListTaskDragEnd() {
      this.dragTaskId = null;
      this.dragOverTaskId = null;
    },

    reorderListTasks(dayIndex: number, draggedId: string, targetId: string) {
      const ordered = this.getSortedTasksForDay(dayIndex);
      const fromIndex = ordered.findIndex(t => t.id === draggedId);
      const toIndex = ordered.findIndex(t => t.id === targetId);
      if (fromIndex === -1 || toIndex === -1) return;

      const [moved] = ordered.splice(fromIndex, 1);
      ordered.splice(toIndex, 0, moved);

      // 表示順を保つため、その日のタスクの startTime（非表示の内部ソートキー）を振り直す
      ordered.forEach((t, index) => {
        const newStartTime = this.slotTimeForIndex(index);
        if (t.startTime !== newStartTime) {
          this.$emit('update-task', t.id, { startTime: newStartTime });
        }
      });
    },
    
    getTaskStyle(task: ScheduledTask): import('vue').StyleValue {
      const [hours, minutes] = task.startTime.split(':').map(Number);

      const displayStartHour = hours === 0 ? 24 : hours;
      const displayStartMinutes = (displayStartHour - 5) * 60 + minutes;
      const totalDisplayMinutes = this.displayHours.length * 60;

      const top = (displayStartMinutes / totalDisplayMinutes) * 100;
      const height = (task.duration / totalDisplayMinutes) * 100;

      const bg = this.roleColors[task.roleId] || '#4a90d9';
      return {
        top: `${Math.max(0, Math.min(100, top))}%`,
        height: `${Math.max(1, Math.min(100 - top, height))}%`,
        left: '2px',
        right: '2px',
        backgroundColor: bg,
        borderColor: this.darkenColor(bg)
      } as import('vue').StyleValue;
    },

    darkenColor(hex: string): string {
      const r = Math.max(0, parseInt(hex.slice(1, 3), 16) - 40);
      const g = Math.max(0, parseInt(hex.slice(3, 5), 16) - 40);
      const b = Math.max(0, parseInt(hex.slice(5, 7), 16) - 40);
      return `rgb(${r},${g},${b})`;
    },
    
    getDayNotes(dayIndex: number): string {
      const dayNote = this.dayNotes.find(dn => dn.day === dayIndex);
      return dayNote?.notes || '';
    },
    
    updateNotes(dayIndex: number, event: Event) {
      const notes = (event.target as HTMLTextAreaElement).value;
      this.$emit('update-day-notes', dayIndex, notes);
    },
    
    openSleepDialog(dayIndex: number) {
      this.selectedDay = dayIndex;
      const dayNote = this.dayNotes.find(dn => dn.day === dayIndex);
      if (dayNote) {
        this.bedTime = dayNote.sleepStart || '22:00';
        this.wakeTime = dayNote.sleepEnd || '07:00';
      }
      this.showSleepDialog = true;
    },
    
    closeSleepDialog() {
      this.showSleepDialog = false;
    },
    
    snapTo30(time: string): string {
      const [h, m] = time.split(':').map(Number);
      const snapped = m < 15 ? 0 : m < 45 ? 30 : 0;
      const hour = m >= 45 ? (h + 1) % 24 : h;
      return `${hour.toString().padStart(2, '0')}:${snapped.toString().padStart(2, '0')}`;
    },

    saveSleepTime() {
      this.$emit('update-sleep-time', this.selectedDay, this.snapTo30(this.bedTime), this.snapTo30(this.wakeTime));
      this.closeSleepDialog();
    },
    
    getSleepInfo(dayIndex: number): string {
      const dayNote = this.dayNotes.find(dn => dn.day === dayIndex);
      if (dayNote?.sleepStart && dayNote?.sleepEnd) {
        return `${dayNote.sleepEnd}-${dayNote.sleepStart}`; // ここはpropsのまま
      }
      return '';
    },
    
    selectTask(task: ScheduledTask) {
      this.selectedTask = task;
    },
    
    startTaskResize(task: ScheduledTask, event: MouseEvent) {
      if ((event.target as HTMLElement).classList.contains('resize-handle')) {
        return; // Let resize handle take care of this
      }
      
      this.selectedTask = task;
      this.isDragging = true;
      this.dragStartY = event.clientY;
      this.originalTaskData = {
        startTime: task.startTime,
        duration: task.duration
      };
      
      document.addEventListener('mousemove', this.handleTaskDrag);
      document.addEventListener('mouseup', this.stopTaskDrag);
    },
    
    startResize(task: ScheduledTask, type: 'top' | 'bottom', event: MouseEvent) {
      event.preventDefault();
      this.selectedTask = task;
      this.isResizing = true;
      this.resizeType = type;
      this.dragStartY = event.clientY;
      this.originalTaskData = {
        startTime: task.startTime,
        duration: task.duration
      };
      
      document.addEventListener('mousemove', this.handleResize);
      document.addEventListener('mouseup', this.stopResize);
    },
    
    handleTaskDrag(event: MouseEvent) {
      if (!this.isDragging || !this.selectedTask) return;
      
      const deltaY = event.clientY - this.dragStartY;
      const hourHeight = 26; // Updated to match new hour height
      const minutesDelta = Math.round((deltaY / hourHeight) * 60);
      
      const [originalHours, originalMinutes] = this.originalTaskData.startTime.split(':').map(Number);
      const originalTotalMinutes = originalHours * 60 + originalMinutes;
      const newTotalMinutes = Math.max(5 * 60, Math.min(24 * 60 - this.selectedTask.duration, originalTotalMinutes + minutesDelta));
      
      const newHours = Math.floor(newTotalMinutes / 60);
      const newMinutes = newTotalMinutes % 60;
      const newStartTime = `${newHours.toString().padStart(2, '0')}:${newMinutes.toString().padStart(2, '0')}`;
      
      this.updateTask(this.selectedTask.id, { startTime: newStartTime });
    },
    
    handleResize(event: MouseEvent) {
      if (!this.isResizing || !this.selectedTask) return;
      
      const deltaY = event.clientY - this.dragStartY;
      const hourHeight = 26; // Updated to match new hour height
      const minutesDelta = Math.round((deltaY / hourHeight) * 60);
      
      if (this.resizeType === 'bottom') {
        // Bottom handle: increase duration when dragging down (intuitive)
        const newDuration = Math.max(15, this.originalTaskData.duration + minutesDelta);
        this.updateTask(this.selectedTask.id, { duration: newDuration });
      } else if (this.resizeType === 'top') {
        // Top handle: extend upward when dragging up (intuitive)
        // When dragging up (negative deltaY), we want to extend the task upward
        const [originalHours, originalMinutes] = this.originalTaskData.startTime.split(':').map(Number);
        const originalTotalMinutes = originalHours * 60 + originalMinutes;
        
        // Invert the delta for top handle to make it intuitive
        const newStartMinutes = Math.max(5 * 60, originalTotalMinutes + minutesDelta);
        const newDuration = Math.max(15, this.originalTaskData.duration - minutesDelta);
        
        const newHours = Math.floor(newStartMinutes / 60);
        const newMinutes = newStartMinutes % 60;
        const newStartTime = `${newHours.toString().padStart(2, '0')}:${newMinutes.toString().padStart(2, '0')}`;
        
        this.updateTask(this.selectedTask.id, { 
          startTime: newStartTime,
          duration: newDuration 
        });
      }
    },
    
    stopTaskDrag() {
      this.isDragging = false;
      document.removeEventListener('mousemove', this.handleTaskDrag);
      document.removeEventListener('mouseup', this.stopTaskDrag);
    },
    
    stopResize() {
      this.isResizing = false;
      this.resizeType = '';
      document.removeEventListener('mousemove', this.handleResize);
      document.removeEventListener('mouseup', this.stopResize);
    },
    
    updateTask(taskId: string, updates: Partial<ScheduledTask>) {
      this.$emit('update-task', taskId, updates);
    },

    onTaskRightClick(task: ScheduledTask, event: MouseEvent) {
      this.selectedTask = task;
      this.menuX = event.clientX;
      this.menuY = event.clientY;
      this.showContextMenu = true;
      this.contextMenuType = 'task';
      document.addEventListener('click', this.closeContextMenu);
    },
    onEmptySlotRightClick(dayIndex: number, hour: number, event: MouseEvent) {
      if (!this.copiedTask) return;
      this.pasteTarget = { dayIndex, hour };
      this.menuX = event.clientX;
      this.menuY = event.clientY;
      this.showContextMenu = true;
      this.contextMenuType = 'paste';
      document.addEventListener('click', this.closeContextMenu);
    },
    copyTask(task: ScheduledTask | null) {
      if (!task) return;
      this.copiedTask = { ...task };
      this.showContextMenu = false;
    },
    pasteTask() {
      if (!this.copiedTask || !this.pasteTarget) return;
      const newTask = {
        ...this.copiedTask,
        id: Date.now().toString(),
        day: this.pasteTarget.dayIndex,
        startTime: `${this.pasteTarget.hour.toString().padStart(2, '0')}:00`
      };
      this.$emit('add-copied-task', newTask);
      this.showContextMenu = false;
      this.pasteTarget = null;
    },
    closeContextMenu() {
      this.showContextMenu = false;
      document.removeEventListener('click', this.closeContextMenu);
    },
    async deleteTask(task: ScheduledTask | null) {
      if (!task) return;
      // await deleteScheduledTask(Number(task.id)); // API呼び出しを削除
      this.$emit('task-deleted', task.id);
      this.showContextMenu = false;
    },
    emitDownload() {
      this.$emit('download-pdf');
    }
  },
  
  mounted() {
    // Click outside to deselect task
    document.addEventListener('click', (event) => {
      if (!this.$el.contains(event.target)) {
        this.selectedTask = null;
      }
    });
  }
});
</script>

<style scoped>
.weekly-calendar {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: white;
  overflow: hidden;
  height: 100vh;
}

.calendar-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  flex-shrink: 0;
  position: relative;
}

.week-navigation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.nav-btn {
  background: none;
  border: 1px solid #ccc;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
}

.nav-btn:hover {
  background-color: #e9ecef;
  border-color: #999;
}

.week-display {
  text-align: center;
}

.week-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.pdf-btn {
  position: absolute;
  top: 20px;
  right: 12px;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 10px;
  color: #333;
  cursor: pointer;
}

.pdf-btn:hover {
  background-color: #f0f0f0;
}

.list-mode-toggle {
  position: absolute;
  top: 20px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.list-mode-toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: relative;
  width: 32px;
  height: 18px;
  background-color: #ccc;
  border-radius: 999px;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  background-color: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
}

.list-mode-toggle input:checked + .toggle-slider {
  background-color: #4a90d9;
}

.list-mode-toggle input:checked + .toggle-slider::before {
  transform: translateX(14px);
}

.toggle-label {
  font-size: 10px;
  color: #333;
}

.calendar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 固定ヘッダー行（スクロールしない） */
.calendar-day-headers {
  display: flex;
  flex-shrink: 0;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  scrollbar-gutter: stable;
  overflow: hidden;
}

.time-column-header {
  width: 70px;
  flex-shrink: 0;
  border-right: 1px solid #e0e0e0;
}

.day-header {
  flex: 1;
  height: 50px;
  padding: 4px 4px 2px;
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  cursor: pointer;
  transition: background-color 0.2s;
  text-align: center;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.day-header:hover {
  background-color: #e9ecef;
}

.day-name {
  font-weight: 600;
  font-size: 11px;
  color: #333;
  line-height: 1.1;
  margin-bottom: 1px;
}

.day-date {
  font-size: 14px;
  color: #666;
  line-height: 1.1;
  margin-bottom: 1px;
}

.sleep-indicator {
  font-size: 8px;
  color: #666;
  line-height: 1.1;
}

/* スクロール可能なタイムライン本体 */
.calendar-timeline-body {
  flex: 0 1 520px;
  display: flex;
  overflow-y: auto;
  align-items: flex-start;
  scrollbar-gutter: stable;
}

.time-slots-column {
  width: 70px;
  flex-shrink: 0;
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.time-slot {
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #666;
  border-bottom: 1px solid #e0e0e0;
}

.day-timeline {
  flex: 1;
  position: relative;
  overflow: hidden;
  height: 520px;
  border-right: 1px solid #e0e0e0;
  min-width: 0;
}

.hour-slot {
  height: 26px;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

/* リストモード：時間軸を使わず縦積みのチェックリストにする */
.day-timeline.day-list {
  height: auto;
  min-height: 520px;
  overflow: visible;
  padding: 6px 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.list-task {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #f8f9fa;
  cursor: grab;
}

.list-task:active {
  cursor: grabbing;
}

.list-task:hover {
  background-color: #eef2f7;
}

.list-task.selected {
  border-color: #ff6b35;
  box-shadow: 0 0 0 2px rgba(255, 107, 53, 0.3);
}

.list-task.drag-over {
  border-color: #4a90d9;
  box-shadow: 0 -2px 0 0 #4a90d9;
}

.list-task-checkbox {
  flex-shrink: 0;
  cursor: pointer;
}

.list-task-title {
  font-size: 11px;
  color: #333;
  word-break: break-word;
}

.list-task.completed .list-task-title {
  color: #999;
  text-decoration: line-through;
}

.list-empty-hint {
  font-size: 10px;
  color: #bbb;
  text-align: center;
  padding: 8px 0;
}

/* 固定ノートセクション（スクロールしない） */
.calendar-notes-section {
  display: flex;
  flex: 1;
  min-height: 80px;
  border-top: 2px solid #e0e0e0;
  background-color: white;
  scrollbar-gutter: stable;
  overflow: hidden;
}

.notes-time-label {
  width: 70px;
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #666;
  font-weight: 500;
}

.hour-slot:hover {
  background-color: #f8f9fa;
}

.scheduled-task {
  position: absolute;
  color: white;
  border-radius: 3px;
  padding: 2px 4px;
  font-size: 9px;
  font-weight: 500;
  overflow: hidden;
  cursor: grab;
  border: 1px solid transparent;
  user-select: none;
  transition: box-shadow 0.2s;
}

.is-dragging-task,
.is-dragging-task * {
  cursor: grabbing !important;
}

.scheduled-task:hover {
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.3);
}

.scheduled-task.selected {
  border-color: #ff6b35;
  box-shadow: 0 0 0 2px rgba(255, 107, 53, 0.3);
}

.task-content {
  pointer-events: none;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.resize-handle {
  position: absolute;
  left: 0;
  right: 0;
  height: 4px;
  background-color: transparent;
  cursor: ns-resize;
  z-index: 10;
}

.resize-handle-top {
  top: -2px;
}

.resize-handle-bottom {
  bottom: -2px;
}

.scheduled-task:hover .resize-handle {
  background-color: rgba(255, 255, 255, 0.3);
}

.day-notes {
  flex: 1;
  border-right: 1px solid #e0e0e0;
  display: flex;
  min-width: 0;
}

.notes-input {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  padding: 6px;
  resize: none;
  font-size: 10px;
  color: #666;
  line-height: 1.3;
}

.notes-input:focus {
  outline: none;
  background-color: white;
}

.sleep-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.sleep-dialog {
  background-color: white;
  padding: 24px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 300px;
}

.sleep-dialog h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
}

.sleep-inputs {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.sleep-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sleep-input-group label {
  font-size: 14px;
  color: #666;
  min-width: 80px;
}

.sleep-input-group input {
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 14px;
}

.dialog-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.dialog-actions .confirm-btn,
.dialog-actions .cancel-btn {
  padding: 8px 16px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.dialog-actions .confirm-btn {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

.dialog-actions .confirm-btn:hover {
  background-color: #0056b3;
}

.dialog-actions .cancel-btn {
  background: white;
}

.dialog-actions .cancel-btn:hover {
  background-color: #f0f0f0;
}

.context-menu {
  background: white;
  border: 1px solid #ccc;
  min-width: 100px;
  box-shadow: 2px 2px 6px rgba(0,0,0,0.15);
}
.context-menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.context-menu li {
  padding: 8px 16px;
  cursor: pointer;
}
.context-menu li:hover {
  background: #eee;
}
</style>