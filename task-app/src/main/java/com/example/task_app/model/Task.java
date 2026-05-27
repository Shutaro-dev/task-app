package com.example.task_app.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Task {
    private Integer taskId;
    private Integer roleId;
    private String title;
    private boolean isPermanent;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 