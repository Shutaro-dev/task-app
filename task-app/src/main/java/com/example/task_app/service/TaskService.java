package com.example.task_app.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.form.ReorderItem;
import com.example.task_app.model.Task;
import com.example.task_app.mapper.TaskMapper;
import com.example.task_app.response.TaskResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TaskService {

    private TaskMapper taskMapper;

    @Autowired
    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public void createTask(TaskDto taskDto) {
        Task task = new Task();
        task.setTaskId(taskDto.getTaskId() != null ? taskDto.getTaskId() : null);
        task.setRoleId(taskDto.getRoleId());
        task.setTitle(taskDto.getTitle());
        task.setPermanent(taskDto.isPermanent());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    public List<TaskResponse> getTasks() {
        return taskMapper.findAll().stream()
                .map(t -> new TaskResponse(t.getTaskId(), t.getRoleId(), t.getTitle(), t.isPermanent()))
                .toList();
    }

    public TaskResponse updateTask(Integer id, String title, Boolean isPermanent) {
        Task existing = taskMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Task not found: " + id);
        }
        existing.setTitle(title);
        existing.setPermanent(isPermanent);
        existing.setUpdatedAt(LocalDateTime.now());
        taskMapper.update(existing);
        return new TaskResponse(existing.getTaskId(), existing.getRoleId(), existing.getTitle(), existing.isPermanent());
    }

    public void reorderTasks(List<ReorderItem> items) {
        items.forEach(item -> taskMapper.updateSortOrder(item.getId(), item.getSortOrder()));
    }

    public void deleteTask(Integer id) {
        Task existing = taskMapper.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Task not found: " + id);
        }
        taskMapper.deleteById(id);
    }
}
