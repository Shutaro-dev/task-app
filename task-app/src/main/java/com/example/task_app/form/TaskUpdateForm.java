package com.example.task_app.form;

import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
public class TaskUpdateForm {
    @NotBlank
    private String title;
    @NotNull
    private Boolean isPermanent;
}
