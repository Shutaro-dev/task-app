<template>
  <div class="left-sidebar">
    <div class="sharpen-summary">
      <div class="sharpen-header">
        <h3>Sharpen the Saw</h3>
        <i class="bi bi-gear settings-btn-small" @click="$emit('open-settings')"></i>
      </div>
      <div class="saw-areas">
        <div 
          v-for="area in sharpenTheSawAreas" 
          :key="area.id"
          class="saw-area"
        >
          <span class="saw-icon">{{ area.icon }}</span>
          <div class="saw-content">
            <span class="saw-name">{{ area.name }}</span>
            <div v-if="area.tasks && area.tasks.length > 0" class="saw-tasks">
              <div 
                v-for="task in area.tasks.slice(0, 4)" 
                :key="task.id" 
                class="saw-task"
              >
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
    
    <div class="roles-section">
      <div class="section-header">
        <h3>Roles and Goals</h3>
        <button @click="showAddRole = true" class="add-btn">Add Role</button>
      </div>
      
      <div v-if="showAddRole" class="add-role-form">
        <input 
          v-model="newRoleName"
          @keyup.enter="handleAddRole"
          placeholder="Role name"
          class="role-input"
          ref="roleInput"
        />
        <div class="form-actions">
          <button @click="handleAddRole" class="confirm-btn">Add</button>
          <button @click="cancelAddRole" class="cancel-btn">Cancel</button>
        </div>
      </div>
      
      <div class="roles-list">
        <div v-for="role in mergedRoles" :key="role.id" class="role-item">
          <div class="role-header" @click="$emit('toggle-role', role.id)">
            <span class="expand-icon">{{ role.isExpanded ? '▼' : '▶' }}</span>
            <span v-if="editingRoleId !== role.id" class="role-name">{{ role.name }}</span>
            <input v-else v-model="editingRoleName" @keyup.enter="confirmEditRole(role)" @keyup.esc="cancelEditRole" @blur="confirmEditRole(role)" class="edit-role-input" />
            <span class="task-count">({{ role.tasks.length }})</span>
            <button @click.stop="deleteRole(role.id)" class="delete-role-btn">×</button>
            <button @click.stop="startEditRole(role)" class="edit-role-btn" title="編集"><span>✏️</span></button>
          </div>
          
          <div v-if="role.isExpanded" class="role-content">
            <div class="tasks-list">
              <div 
                v-for="task in role.tasks" 
                :key="task.id"
                class="task-item"
                draggable="true"
                @dragstart="handleDragStart(task)"
              >
                <span class="task-title">{{ task.title }}</span>
                <span class="task-type">{{ task.isPermanent ? 'P' : 'T' }}</span>
                <button @click.stop="deleteTask(role.id, task.id)" class="delete-task-btn">×</button>
              </div>
            </div>
            
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
              <button 
                v-else 
                @click="startAddTask(role.id)"
                class="add-task-btn"
              >
                + Add Task
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import type { PropType } from 'vue';
import type { Role, Task, SharpenTheSawArea } from '../types';
// import { createRole } from '../services/roleService'; // 一時停止: 役割はローカルストレージで管理

export default defineComponent({
  name: 'LeftSidebar',
  props: {
    roles: {
      type: Array as PropType<Role[]>,
      required: true
    },
    sharpenTheSawAreas: {
      type: Array as PropType<SharpenTheSawArea[]>,
      required: true
    },
    temporaryTasks: {
      type: Array as PropType<Task[]>,
      required: false,
      default: () => []
    }
  },
  emits: ['add-role', 'add-task', 'toggle-role', 'task-drag-start', 'open-settings', 'delete-role', 'update-role-name', 'delete-task'],
  data() {
    return {
      showAddRole: false,
      newRoleName: '',
      newTaskTitle: '',
      newTaskIsPermanent: false,
      editingRoleId: null as string | null,
      editingRoleName: '',
      addRoleEnterCount: 0,
      editRoleEnterCount: 0,
      addTaskEnterCount: 0
    };
  },
  computed: {
    mergedRoles() {
      // 各Roleごとに、isPermanent: trueのtasksとtemporaryTasksを合成
      return this.roles.map(role => {
        const tempTasks = this.temporaryTasks.filter(t => t.roleId === role.id);
        return {
          ...role,
          tasks: [...role.tasks, ...tempTasks],
          showAddTask: role.showAddTask ?? false,
          isExpanded: role.isExpanded,
        };
      });
    }
  },
  methods: {
    async handleAddRole() {
      if (this.newRoleName.trim()) {
        this.addRoleEnterCount++;
        if (this.addRoleEnterCount < 2) return;
        
        // バックエンド呼び出しは一旦停止し、親(Dashboard)でローカルストレージ保存に委譲
        this.$emit('add-role', this.newRoleName.trim());
        this.newRoleName = '';
        this.showAddRole = false;
        this.addRoleEnterCount = 0;
      }
    },
    
    cancelAddRole() {
      this.newRoleName = '';
      this.showAddRole = false;
    },
    
    deleteRole(roleId: string) {
      if (confirm('Are you sure you want to delete this role and all its tasks?')) {
        this.$emit('delete-role', roleId);
      }
    },
    
    startAddTask(roleId: string) {
      this.roles.forEach(r => {
        if (r.id !== roleId) {
          r.showAddTask = false;
        }
      });
      const role = this.roles.find(r => r.id === roleId);
      if (role) role.showAddTask = true;
    },
    
    handleAddTask(roleId: string) {
      if (this.newTaskTitle.trim()) {
        this.addTaskEnterCount++;
        if (this.addTaskEnterCount < 2) return;
        this.$emit('add-task', roleId, this.newTaskTitle.trim(), this.newTaskIsPermanent);
        this.newTaskTitle = '';
        this.newTaskIsPermanent = false;
        const role = this.roles.find(r => r.id === roleId);
        if (role) {
          role.showAddTask = false;
        }
        this.addTaskEnterCount = 0;
      }
    },
    
    cancelAddTask(roleId: string) {
      this.newTaskTitle = '';
      this.newTaskIsPermanent = false;
      const role = this.roles.find(r => r.id === roleId);
      if (role) role.showAddTask = false;
      this.addTaskEnterCount = 0;
    },
    
    handleDragStart(task: Task) {
      this.$emit('task-drag-start', task);
    },

    startEditRole(role: Role) {
      this.editingRoleId = role.id;
      this.editingRoleName = role.name;
      this.editRoleEnterCount = 0;
      this.$nextTick(() => {
        const input = this.$el.querySelector('.edit-role-input');
        if (input) (input as HTMLInputElement).focus();
      });
    },
    confirmEditRole(role: Role) {
      if (this.editingRoleName.trim() && this.editingRoleName !== role.name) {
        this.editRoleEnterCount++;
        if (this.editRoleEnterCount < 2) return;
        this.$emit('update-role-name', role.id, this.editingRoleName.trim());
        this.editingRoleId = null;
        this.editingRoleName = '';
        this.editRoleEnterCount = 0;
      }
    },
    cancelEditRole() {
      this.editingRoleId = null;
      this.editingRoleName = '';
      this.editRoleEnterCount = 0;
    },
    deleteTask(roleId: string, taskId: string) {
      // タスク削除時に入力内容をリセット
      this.newTaskTitle = '';
      this.newTaskIsPermanent = false;
      this.addTaskEnterCount = 0;
      this.$emit('delete-task', roleId, taskId);
    },
    resetEnterCount() {
      this.addTaskEnterCount = 0;
    }
  },
  mounted() {
    this.$nextTick(() => {
      if (this.showAddRole && this.$refs.roleInput) {
        (this.$refs.roleInput as HTMLInputElement).focus();
      }
    });
  }
});
</script>

<style scoped>
.left-sidebar {
  width: 320px;
  background-color: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sharpen-summary {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.sharpen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sharpen-summary h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.settings-btn-small {
  padding: 4px 6px;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
  color: #666;
}

.settings-btn-small:hover {
  color: #333;
}

.saw-areas {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.saw-area {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  transition: background-color 0.2s;
  min-height: 50px;
}

.saw-area:hover {
  background-color: #f0f0f0;
}

.saw-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.saw-content {
  flex: 1;
  min-width: 0;
}

.saw-name {
  font-size: 12px;
  color: #333;
  font-weight: 600;
}

.saw-tasks {
  margin-top: 2px;
}

.saw-task {
  font-size: 9px;
  color: #888;
  line-height: 1.2;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.saw-task-more {
  font-size: 8px;
  color: #aaa;
  font-style: italic;
  margin-top: 2px;
}



.roles-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.add-btn {
  background: none;
  border: 1px solid #ccc;
  padding: 4px 8px;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.add-btn:hover {
  background-color: #f0f0f0;
  border-color: #999;
}

.add-role-form {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.role-input,
.task-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 14px;
  margin-bottom: 8px;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.confirm-btn,
.cancel-btn {
  padding: 4px 12px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.confirm-btn {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

.confirm-btn:hover {
  background-color: #0056b3;
}

.cancel-btn {
  background: white;
}

.cancel-btn:hover {
  background-color: #f0f0f0;
}

.roles-list {
  flex: 1;
  overflow-y: auto;
}

.role-item {
  border-bottom: 1px solid #e0e0e0;
}

.role-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.role-header:hover {
  background-color: #f8f9fa;
}

.expand-icon {
  width: 20px;
  font-size: 12px;
  color: #666;
}

.role-name {
  flex: 1;
  font-weight: 500;
  color: #333;
}

.task-count {
  font-size: 12px;
  color: #666;
  margin-right: 8px;
}

.delete-role-btn {
  background: none;
  border: 1px solid #dc3545;
  color: #dc3545;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  padding-bottom: 1px;
  transition: all 0.2s;
}

.delete-role-btn:hover {
  background-color: #dc3545;
  color: white;
}

.role-content {
  background-color: #fafafa;
}

.tasks-list {
  padding: 0 16px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin: 4px 0;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 3px;
  cursor: grab;
  transition: all 0.2s;
}

.task-item:hover {
  border-color: #007bff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.task-item:active {
  cursor: grabbing;
}

.task-title {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.task-type {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e9ecef;
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
  color: #666;
}

.add-task-section {
  padding: 12px 16px;
}

.add-task-form {
  background-color: white;
  padding: 12px;
  border-radius: 3px;
  border: 1px solid #e0e0e0;
}

.task-type-toggle {
  margin-bottom: 8px;
}

.task-type-toggle label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
}

.add-task-btn {
  width: 100%;
  padding: 8px;
  background: none;
  border: 1px dashed #ccc;
  border-radius: 3px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.add-task-btn:hover {
  border-color: #007bff;
  color: #007bff;
}

.edit-role-input {
  font-size: 1em;
  padding: 2px 6px;
  margin-left: 4px;
  width: 120px;
}
.edit-role-btn {
  background: none;
  border: none;
  cursor: pointer;
  margin-left: 4px;
  font-size: 1em;
}
.edit-role-btn:hover {
  color: #007bff;
}
.delete-task-btn {
  background: none;
  border: none;
  color: #d00;
  font-size: 1em;
  margin-left: 6px;
  cursor: pointer;
}
.delete-task-btn:hover {
  color: #ff3333;
}
</style>