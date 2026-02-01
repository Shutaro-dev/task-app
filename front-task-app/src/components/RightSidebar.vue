<template>
  <div class="right-sidebar">
    <div class="notes-section">
      <h3>Weekly Notes</h3>
      <textarea 
        :value="weeklyNotes"
        @input="updateNotes"
        placeholder="Weekly reflections, goals, insights..."
        class="weekly-notes-input"
      ></textarea>
    </div>
    
    <div class="info-section">
      <h4>Quick Tips</h4>
      <ul class="tips-list">
        <li>Drag tasks from roles to calendar</li>
        <li>Click day headers to set sleep times</li>
        <li>Use daily notes for reflection</li>
        <li>Balance roles with renewal activities</li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'RightSidebar',
  props: {
    weeklyNotes: {
      type: String,
      required: true
    }
  },
  emits: ['update-weekly-notes'],
  methods: {
    updateNotes(event: Event) {
      const notes = (event.target as HTMLTextAreaElement).value;
      this.$emit('update-weekly-notes', notes);
    }
  }
});
</script>

<style scoped>
.right-sidebar {
  width: 280px;
  background-color: white;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notes-section {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.notes-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.weekly-notes-input {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 12px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  line-height: 1.5;
}

.weekly-notes-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.info-section {
  padding: 16px;
  background-color: #f8f9fa;
  border-top: 1px solid #e0e0e0;
}

.info-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.tips-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.tips-list li {
  margin-bottom: 4px;
}
</style>