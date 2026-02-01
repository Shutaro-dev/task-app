package com.example.task_app.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import com.example.task_app.form.TaskForm;
import com.example.task_app.service.TaskService;
import com.example.task_app.dto.TaskDto;

@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;

    @Autowired
    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@RequestBody @Valid TaskForm taskForm) {
        TaskDto taskDto = new TaskDto(taskForm.getTaskId(), taskForm.getRoleId(), taskForm.getTitle(),
                taskForm.isPermanent());
        taskService.createTask(taskDto);
        return ResponseEntity.ok().build();
    }

    // @GetMapping
    // public ResponseEntity<List<TaskResponse>> getTasks() {
    // List<TaskDto> dtoList = taskService.getTasks();
    // List<TaskResponse> responseList = dtoList.stream()
    // .map(dto -> new TaskResponse(dto.getTaskId(), dto.getRoleId(),
    // dto.getTitle(), dto.isPermanent()))
    // .toList();
    // return ResponseEntity.ok(responseList);
    // }
}