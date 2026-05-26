package com.example.task_app.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TaskResponse {
    private Integer taskId;
    private Integer roleId;
    private String title;
    @JsonProperty("isPermanent")
    private boolean isPermanent;
}