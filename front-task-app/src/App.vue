<template>
  <div id="app">
    <Dashboard />
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import Dashboard from './components/Dashboard.vue';

export default defineComponent({
  name: 'App',
  components: {
    Dashboard
  }
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background-color: #fafafa;
  color: #333;
  line-height: 1.5;
}

#app {
  height: 100vh;
  overflow: hidden;
}

/* Global button styles */
button {
  font-family: inherit;
  transition: all 0.2s ease;
}

button:focus {
  outline: 2px solid #007bff;
  outline-offset: 2px;
}

/* Global input styles */
input, textarea {
  font-family: inherit;
}

input:focus, textarea:focus {
  outline: 2px solid #007bff;
  outline-offset: 1px;
}

/* Scrollbar styles */
::-webkit-scrollbar {
  width: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Utility classes */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>