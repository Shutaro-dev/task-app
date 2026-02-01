package com.example.task_app.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.model.Task;
import com.example.task_app.mapper.TaskMapper;
import java.time.LocalDateTime;

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

    // public List<TaskDto> getTasks() {
    //     List<Task> taskList = taskMapper.findAll();
    //     return taskList.stream()
    //             .map(task -> new TaskDto(task.getId(), task.getTitle()))
    //             .toList();
    // }
}
