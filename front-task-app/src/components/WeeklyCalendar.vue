<template>
  <div class="weekly-calendar">
    <div class="calendar-header">
      <div class="week-navigation">
        <button @click="previousWeek" class="nav-btn">‹</button>
        <div class="week-display">
          <span class="week-text">Week of {{ formatWeekStart(currentWeek) }}</span>
        </div>
        <button @click="nextWeek" class="nav-btn">›</button>
      </div>
      <button class="pdf-btn" @click.stop="emitDownload">PDF Download</button>
    </div>
    
    <div class="calendar-content">
      <div class="calendar-grid">
        <div class="time-column">
          <div class="time-header"></div>
          <div 
            v-for="hour in displayHours" 
            :key="hour"
            class="time-slot"
          >
            {{ formatHour(hour) }}
          </div>
          <div class="notes-time-header">Notes</div>
        </div>
        
        <div 
          v-for="(day, dayIndex) in days" 
          :key="dayIndex"
          class="day-column"
        >
          <div class="day-header" @click="openSleepDialog(dayIndex)">
            <div class="day-name">{{ day.name }}</div>
            <div class="day-date">{{ formatDate(day.date) }}</div>
            <div v-if="getSleepInfo(dayIndex)" class="sleep-indicator">
              💤 {{ getSleepInfo(dayIndex) }}
            </div>
          </div>
          
          <div class="day-timeline" @drop="handleDrop(dayIndex, $event)" @dragover.prevent>
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
          </div>
          
          <div class="day-notes">
            <textarea 
              :value="getDayNotes(dayIndex)"
              @input="updateNotes(dayIndex, $event)"
              placeholder="Daily notes..."
              class="notes-input"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="showSleepDialog" class="sleep-dialog-overlay" @click="closeSleepDialog">
      <div class="sleep-dialog" @click.stop>
        <h3>睡眠時間の入力（{{ days[selectedDay]?.name }}）</h3>
        <div class="sleep-inputs">
          <div class="sleep-input-group">
            <label>起床時間:</label>
            <input type="time" v-model="wakeTime" />
          </div>
          <div class="sleep-input-group">
            <label>就寝時間:</label>
            <input type="time" v-model="bedTime" />
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
    }
  },
  emits: ['week-change', 'task-drop', 'update-day-notes', 'update-sleep-time', 'update-task', 'task-deleted', 'add-copied-task', 'download-pdf'],
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
      contextMenuType: 'task' as 'task' | 'paste' | ''
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
    
    getTasksForDay(dayIndex: number): ScheduledTask[] {
      return this.scheduledTasks.filter(task => task.day === dayIndex);
    },
    
    getTaskStyle(task: ScheduledTask): import('vue').StyleValue {
      const [hours, minutes] = task.startTime.split(':').map(Number);
      
      // Convert to display time (5 AM = 0%, 24:00 = 100%)
      const displayStartHour = hours === 0 ? 24 : hours; // Handle 0:00 as 24:00
      const displayStartMinutes = (displayStartHour - 5) * 60 + minutes;
      const totalDisplayMinutes = this.displayHours.length * 60; // 20 hours * 60 minutes
      
      const top = (displayStartMinutes / totalDisplayMinutes) * 100;
      const height = (task.duration / totalDisplayMinutes) * 100;
      
      return {
        top: `${Math.max(0, Math.min(100, top))}%`,
        height: `${Math.max(1, Math.min(100 - top, height))}%`,
        left: '2px',
        right: '2px'
      } as import('vue').StyleValue;
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
    
    saveSleepTime() {
      this.$emit('update-sleep-time', this.selectedDay, this.bedTime, this.wakeTime);
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

.calendar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.calendar-grid {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.time-column {
  width: 70px;
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.time-header {
  height: 50px;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.time-slot {
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #666;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.notes-time-header {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #666;
  font-weight: 500;
  border-bottom: 1px solid #e0e0e0;
}

.day-column {
  flex: 1;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.day-header {
  height: 50px;
  padding: 4px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  cursor: pointer;
  transition: background-color 0.2s;
  text-align: center;
  flex-shrink: 0;
}

.day-header:hover {
  background-color: #e9ecef;
}

.day-name {
  font-weight: 600;
  font-size: 11px;
  color: #333;
  margin-bottom: 1px;
}

.day-date {
  font-size: 14px;
  color: #666;
  margin-bottom: 1px;
}

.sleep-indicator {
  font-size: 8px;
  color: #666;
}

.day-timeline {
  position: relative;
  overflow: hidden;
  min-height: 0;
  flex-shrink: 0;
  height: 520px; /* 20 hours * 26px per hour */
}

.hour-slot {
  height: 26px;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

.hour-slot:hover {
  background-color: #f8f9fa;
}

.scheduled-task {
  position: absolute;
  background-color: #007bff;
  color: white;
  border-radius: 3px;
  padding: 2px 4px;
  font-size: 9px;
  font-weight: 500;
  overflow: hidden;
  cursor: move;
  border: 1px solid #0056b3;
  user-select: none;
  transition: box-shadow 0.2s;
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
  border-bottom: 1px solid #e0e0e0;
  display: flex;
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