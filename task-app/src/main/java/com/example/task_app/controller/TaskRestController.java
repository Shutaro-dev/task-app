package com.example.task_app.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import com.example.task_app.form.TaskForm;
import com.example.task_app.form.TaskUpdateForm;
import com.example.task_app.service.TaskService;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.response.TaskResponse;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/tasks")
public class TaskRestController {

    private final TaskService taskService;

    @Autowired
    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks() {
        return ResponseEntity.ok(taskService.getTasks());
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@RequestBody @Valid TaskForm taskForm) {
        TaskDto taskDto = new TaskDto(taskForm.getTaskId(), taskForm.getRoleId(), taskForm.getTitle(),
                taskForm.getIsPermanent());
        taskService.createTask(taskDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Integer id,
            @RequestBody @Valid TaskUpdateForm form) {
        try {
            TaskResponse response = taskService.updateTask(id, form.getTitle(), form.getIsPermanent());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
