<template>
  <div class="dashboard" ref="dashboardRoot">
    <LeftSidebar
      :roles="roles"
      :sharpenTheSawAreas="sharpenTheSawAreas"
      :temporaryTasks="currentWeekData.temporaryTasks || []"
      @add-role="addRole"
      @add-task="addTask"
      @toggle-role="toggleRole"
      @task-drag-start="handleTaskDragStart"
      @open-settings="showSettings = true"
      @delete-role="deleteRole"
      @update-role-name="updateRoleName"
      @delete-task="deleteTask"
      @update-role-color="updateRoleColor"
      @update-task-title="updateTaskTitle"
      @reorder-tasks="reorderTasks"
      @toggle-task-permanent="toggleTaskPermanent"
      @reorder-roles="reorderRoles"
    />

    <WeeklyCalendar
      :current-week="currentWeek"
      :scheduled-tasks="currentWeekData.scheduledTasks"
      :day-notes="currentWeekData.dayNotes"
      :role-colors="roleColorMap"
      @week-change="changeWeek"
      @task-drop="handleTaskDrop"
      @update-day-notes="updateDayNotes"
      @update-sleep-time="updateSleepTime"
      @update-task="updateScheduledTask"
      @task-deleted="handleTaskDeleted"
      @add-copied-task="addCopiedTask"
      @download-pdf="downloadPdf"
    />
    
    <RightSidebar 
      :weekly-notes="currentWeekData.weeklyNotes"
      @update-weekly-notes="updateWeeklyNotes"
    />
    
    <SharpenTheSawSettings 
      v-if="showSettings"
      :areas="sharpenTheSawAreas"
      @close="showSettings = false"
      @update-areas="updateSharpenTheSawAreas"
    />
    
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import type { Role, Task, SharpenTheSawArea, WeekData, ScheduledTask } from '../types';
import LeftSidebar from './LeftSidebar.vue';
import WeeklyCalendar from './WeeklyCalendar.vue';
import RightSidebar from './RightSidebar.vue';
import SharpenTheSawSettings from './SharpenTheSawSettings.vue';

const STORAGE_KEY = 'fourth-gen-time-management';
const ROLE_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085'];

export default defineComponent({
  name: 'Dashboard',
  components: {
    LeftSidebar,
    WeeklyCalendar,
    RightSidebar,
    SharpenTheSawSettings
  },
  data() {
    return {
      showSettings: false,
      currentWeek: this.getStartOfWeek(new Date()),
      draggedTask: null as Task | null,
      roles: [
        {
          id: '1',
          name: 'Professional',
          isExpanded: false,
          tasks: [
            { id: 't1', title: 'Review quarterly goals', roleId: '1', isPermanent: true },
            { id: 't2', title: 'Team meeting preparation', roleId: '1', isPermanent: false }
          ]
        },
        {
          id: '2',
          name: 'Family',
          isExpanded: false,
          tasks: [
            { id: 't3', title: 'Quality time with children', roleId: '2', isPermanent: true },
            { id: 't4', title: 'Plan weekend activities', roleId: '2', isPermanent: false }
          ]
        }
      ] as Role[],
      sharpenTheSawAreas: [
        {
          id: 'physical',
          name: 'Physical',
          icon: '💪',
          tasks: []
        },
        {
          id: 'mental',
          name: 'Intellectual',
          icon: '🧠',
          tasks: []
        },
        {
          id: 'social-emotional',
          name: 'Social/Emotional',
          icon: '❤️',
          tasks: []
        },
        {
          id: 'spiritual',
          name: 'Spiritual', 
          icon: '🙏',
          tasks: []
        }
      ] as SharpenTheSawArea[],
      weekData: new Map<string, WeekData>()
    };
  },
  computed: {
    roleColorMap(): Record<string, string> {
      const map: Record<string, string> = {};
      this.roles.forEach(r => { map[r.id] = r.color || '#4a90d9'; });
      return map;
    },
    currentWeekData(): WeekData {
      const weekKey = this.getWeekKey(this.currentWeek);
      if (!this.weekData.has(weekKey)) {
        this.weekData.set(weekKey, {
          weekStart: this.currentWeek,
          scheduledTasks: [],
          dayNotes: Array.from({ length: 7 }, (_, i) => ({ day: i, notes: '' })),
          weeklyNotes: '',
          temporaryTasks: []
        });
      }
      const data = this.weekData.get(weekKey)!;
      if (!data.temporaryTasks) data.temporaryTasks = [];
      return data;
    }
  },
  methods: {
    getStartOfWeek(date: Date): Date {
      const d = new Date(date);
      const day = d.getDay();
      const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Monday start
      return new Date(d.setDate(diff));
    },
    
    getWeekKey(date: Date): string {
      return date.toISOString().split('T')[0];
    },
    
    changeWeek(newWeek: Date) {
      // 週切り替え時に全Role.tasksからisPermanent: falseのタスクを除外
      this.roles.forEach(role => {
        role.tasks = role.tasks.filter(task => task.isPermanent);
      });
      this.currentWeek = newWeek;
      this.saveData();
    },
    
    addRole(name: string) {
      const newRole: Role = {
        id: Date.now().toString(),
        name,
        tasks: [],
        isExpanded: false,
        color: ROLE_COLORS[this.roles.length % ROLE_COLORS.length]
      };
      this.roles.push(newRole);
      this.saveData();
    },
    
    deleteRole(roleId: string) {
      this.roles = this.roles.filter(role => role.id !== roleId);
      // Also remove any scheduled tasks for this role
      this.weekData.forEach(weekData => {
        weekData.scheduledTasks = weekData.scheduledTasks.filter(task => task.roleId !== roleId);
      });
      this.saveData();
    },
    
    addTask(roleId: string, taskTitle: string, isPermanent: boolean) {
      const role = this.roles.find(r => r.id === roleId);
      if (isPermanent) {
        if (role) {
          const newTask: Task = {
            id: Date.now().toString(),
            title: taskTitle,
            roleId,
            isPermanent
          };
          role.tasks.push(newTask);
          this.saveData();
        }
      } else {
        // 一時タスクは今週のWeekDataにTask型で追加
        const newTask: Task = {
          id: Date.now().toString(),
          title: taskTitle,
          roleId,
          isPermanent
        };
        // scheduledTasksはScheduledTask型なので、別途temporaryTasks配列をWeekDataに追加する必要がある
        // ここでは一時的にcurrentWeekData.temporaryTasksに追加する想定で記述
        if (!('temporaryTasks' in this.currentWeekData)) {
          // @ts-ignore
          this.currentWeekData.temporaryTasks = [];
        }
        // @ts-ignore
        this.currentWeekData.temporaryTasks.push(newTask);
        this.saveData();
      }
    },
    
    toggleRole(roleId: string) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        role.isExpanded = !role.isExpanded;
        // 役割を閉じる際にshowAddTaskをリセット
        if (!role.isExpanded) {
          role.showAddTask = false;
        }
        this.saveData();
      }
    },
    
    handleTaskDragStart(task: Task) {
      this.draggedTask = task;
    },
    
    handleTaskDrop(day: number, startTime: string) {
      if (this.draggedTask) {
        const scheduledTask: ScheduledTask = {
          id: Date.now().toString(),
          taskId: this.draggedTask.id,
          day,
          startTime,
          duration: 60, // Default 60 minutes
          title: this.draggedTask.title,
          roleId: this.draggedTask.roleId
        };
        
        this.currentWeekData.scheduledTasks.push(scheduledTask);
        this.draggedTask = null;
        this.saveData();
      }
    },
    
    updateScheduledTask(taskId: string, updates: Partial<ScheduledTask>) {
      const task = this.currentWeekData.scheduledTasks.find(t => t.id === taskId);
      if (task) {
        Object.assign(task, updates);
        this.saveData();
      }
    },
    
    updateDayNotes(day: number, notes: string) {
      const dayNote = this.currentWeekData.dayNotes.find(dn => dn.day === day);
      if (dayNote) {
        dayNote.notes = notes;
        this.saveData();
      }
    },
    
    updateSleepTime(day: number, sleepStart: string, sleepEnd: string) {
      const dayNote = this.currentWeekData.dayNotes.find(dn => dn.day === day);
      if (dayNote) {
        dayNote.sleepStart = sleepStart;
        dayNote.sleepEnd = sleepEnd;
        this.saveData();
      }
    },
    
    updateWeeklyNotes(notes: string) {
      this.currentWeekData.weeklyNotes = notes;
      this.saveData();
    },
    
    updateSharpenTheSawAreas(areas: SharpenTheSawArea[]) {
      this.sharpenTheSawAreas = areas;
      this.saveData();
    },
    
    updateRoleName(roleId: string, newName: string) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        role.name = newName;
        this.saveData();
      }
    },

    updateRoleColor(roleId: string, color: string) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        role.color = color;
        this.saveData();
      }
    },

    updateTaskTitle(roleId: string, taskId: string, newTitle: string) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        const task = role.tasks.find(t => t.id === taskId);
        if (task) { task.title = newTitle; this.saveData(); return; }
      }
      const tempTask = this.currentWeekData.temporaryTasks?.find(t => t.id === taskId);
      if (tempTask) { tempTask.title = newTitle; this.saveData(); }
    },

    reorderTasks(roleId: string, reorderedTasks: Task[]) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        role.tasks = reorderedTasks.filter(t => t.isPermanent);
      }
      if (this.currentWeekData.temporaryTasks) {
        const tempForRole = reorderedTasks.filter(t => !t.isPermanent && t.roleId === roleId);
        const otherTemp = this.currentWeekData.temporaryTasks.filter(t => t.roleId !== roleId);
        this.currentWeekData.temporaryTasks = [...otherTemp, ...tempForRole];
      }
      this.saveData();
    },

    toggleTaskPermanent(roleId: string, taskId: string, currentlyPermanent: boolean) {
      if (currentlyPermanent) {
        const role = this.roles.find(r => r.id === roleId);
        if (!role) return;
        const idx = role.tasks.findIndex(t => t.id === taskId);
        if (idx === -1) return;
        const [task] = role.tasks.splice(idx, 1);
        task.isPermanent = false;
        if (!this.currentWeekData.temporaryTasks) this.currentWeekData.temporaryTasks = [];
        this.currentWeekData.temporaryTasks.push(task);
      } else {
        const tempIdx = this.currentWeekData.temporaryTasks?.findIndex(t => t.id === taskId) ?? -1;
        if (tempIdx === -1) return;
        const [task] = this.currentWeekData.temporaryTasks!.splice(tempIdx, 1);
        task.isPermanent = true;
        const role = this.roles.find(r => r.id === roleId);
        if (role) role.tasks.push(task);
      }
      this.saveData();
    },

    reorderRoles(newRoles: Role[]) {
      // newRoles は LeftSidebar の roleList 由来で merged tasks（permanent + temporary）を含む。
      // this.roles（permanent タスクのみ）を元に順序だけ入れ替えることで二重追加を防ぐ。
      this.roles = newRoles.map(r => this.roles.find(existing => existing.id === r.id) ?? r);
      this.saveData();
    },
    
    saveData() {
      try {
        const dataToSave = {
          currentWeek: this.currentWeek.toISOString(),
          roles: this.roles,
          sharpenTheSawAreas: this.sharpenTheSawAreas,
          weekData: Array.from(this.weekData.entries()).map(([key, value]) => [
            key,
            {
              ...value,
              weekStart: value.weekStart.toISOString()
            }
          ])
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(dataToSave));
      } catch (error) {
        console.warn('Failed to save data to localStorage:', error);
      }
    },
    
    loadData() {
      try {
        const savedData = localStorage.getItem(STORAGE_KEY);
        if (savedData) {
          const parsed = JSON.parse(savedData);
          
          if (parsed.currentWeek) {
            this.currentWeek = new Date(parsed.currentWeek);
          }
          
          if (parsed.roles) {
            this.roles = parsed.roles.map((r: any) => {
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
            this.sharpenTheSawAreas = parsed.sharpenTheSawAreas;
          }
          
          if (parsed.weekData) {
            this.weekData = new Map(
              parsed.weekData.map(([key, value]: [string, any]) => [
                key,
                {
                  ...value,
                  weekStart: new Date(value.weekStart)
                }
              ])
            );
          }
        }
      } catch (error) {
        console.warn('Failed to load data from localStorage:', error);
      }
    },

    handleTaskDeleted(taskId: string) {
      this.currentWeekData.scheduledTasks = this.currentWeekData.scheduledTasks.filter(t => t.id !== taskId);
      this.saveData();
    },

    deleteTask(roleId: string, taskId: string) {
      const role = this.roles.find(r => r.id === roleId);
      if (role) {
        // まずrole.tasksから削除を試みる
        const originalLength = role.tasks.length;
        role.tasks = role.tasks.filter(t => t.id !== taskId);
        
        // role.tasksから削除されなかった場合、temporaryTasksから削除
        if (role.tasks.length === originalLength) {
          if (this.currentWeekData.temporaryTasks) {
            this.currentWeekData.temporaryTasks = this.currentWeekData.temporaryTasks.filter(t => t.id !== taskId);
          }
        }
        this.saveData();
      }
    },

    addCopiedTask(task: ScheduledTask) {
      this.currentWeekData.scheduledTasks.push(task);
      this.saveData();
    },

    async downloadPdf() {
      const root = this.$refs.dashboardRoot as HTMLElement | undefined;
      if (!root) return;

      // 現在のスタイル・スクロール位置を保存
      const scrollX = window.scrollX;
      const scrollY = window.scrollY;
      const prevHeight = root.style.height;
      const prevOverflow = root.style.overflow as string;

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
        
        // 元の要素と置き換え要素を保存
        replacements.push({
          original: textarea,
          replacement: div
        });
        
        // textareaをdivに置き換え
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
          scrollY: 0
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
    }
  },
  
  mounted() {
    this.loadData();
    this.saveData(); // loadData のクリーンアップ結果を即座に永続化
  }
});
</script>

<style scoped>
.dashboard {
  display: flex;
  height: 100vh;
  background-color: #fafafa;
  overflow: hidden;
}
</style>