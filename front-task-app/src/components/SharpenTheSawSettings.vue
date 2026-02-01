<template>
  <div class="settings-overlay" @click="handleOverlayClick">
    <div class="settings-modal" @click.stop>
      <div class="modal-header">
        <h2>Sharpen the Saw Settings</h2>
        <button @click="$emit('close')" class="close-btn">×</button>
      </div>
      
      <div class="modal-content">
        <p class="description">
          Set your renewal goals and permanent tasks for each area of personal development.
        </p>
        
        <div class="areas-grid">
          <div 
            v-for="area in localAreas" 
            :key="area.id"
            class="area-section"
          >
            <div class="area-header">
              <span class="area-icon">{{ area.icon }}</span>
              <h3>{{ area.name }}</h3>
            </div>
            
            <div class="area-content">
              <div class="tasks-section">
                <h4>Renewal Tasks</h4>
                <div 
                  v-for="(task, index) in area.tasks" 
                  :key="task.id"
                  class="task-item"
                >
                  <input 
                    v-model="task.title"
                    placeholder="Task title"
                    class="task-input"
                  />
                  <button 
                    @click="removeTask(area.id, index)"
                    class="remove-task-btn"
                  >
                    ×
                  </button>
                </div>
                
                <button 
                  @click="addTask(area.id)"
                  class="add-task-btn"
                >
                  + Add Task
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <button @click="handleSave" class="save-btn">Save Settings</button>
        <button @click="$emit('close')" class="cancel-btn">Cancel</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import type { PropType } from 'vue';
import type { SharpenTheSawArea, Task } from '../types';

export default defineComponent({
  name: 'SharpenTheSawSettings',
  props: {
    areas: {
      type: Array as PropType<SharpenTheSawArea[]>,
      required: true
    }
  },
  emits: ['close', 'update-areas'],
  data() {
    return {
      localAreas: JSON.parse(JSON.stringify(this.areas)) as SharpenTheSawArea[]
    };
  },
  methods: {
    handleOverlayClick(event: Event) {
      if (event.target === event.currentTarget) {
        this.$emit('close');
      }
    },
    
    addTask(areaId: string) {
      const area = this.localAreas.find(a => a.id === areaId);
      if (area) {
        const newTask: Task = {
          id: Date.now().toString(),
          title: '',
          roleId: 'renewal',
          isPermanent: true
        };
        area.tasks.push(newTask);
      }
    },
    
    removeTask(areaId: string, taskIndex: number) {
      const area = this.localAreas.find(a => a.id === areaId);
      if (area) {
        area.tasks.splice(taskIndex, 1);
      }
    },
    
    handleSave() {
      // Filter out empty tasks
      this.localAreas.forEach(area => {
        area.tasks = area.tasks.filter(task => task.title.trim() !== '');
      });
      
      this.$emit('update-areas', this.localAreas);
      this.$emit('close');
    }
  }
});
</script>

<style scoped>
.settings-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.settings-modal {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  max-width: 800px;
  max-height: 90vh;
  width: 90%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: #f0f0f0;
}

.modal-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.description {
  margin: 0 0 24px 0;
  color: #666;
  line-height: 1.5;
  font-size: 14px;
}

.areas-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.area-section {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.area-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.area-icon {
  font-size: 20px;
}

.area-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.area-content {
  padding: 20px;
}



.tasks-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.task-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.task-input {
  flex: 1;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 13px;
}

.task-input:focus {
  outline: none;
  border-color: #007bff;
}

.remove-task-btn {
  background: none;
  border: 1px solid #dc3545;
  color: #dc3545;
  width: 28px;
  height: 28px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.remove-task-btn:hover {
  background-color: #dc3545;
  color: white;
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

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.save-btn {
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-btn:hover {
  background-color: #0056b3;
}

.cancel-btn {
  padding: 10px 20px;
  background: white;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background-color: #f0f0f0;
  border-color: #999;
}

@media (max-width: 768px) {
  .areas-grid {
    grid-template-columns: 1fr;
  }
}
</style>