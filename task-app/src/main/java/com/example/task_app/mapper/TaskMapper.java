package com.example.task_app.mapper;

import com.example.task_app.model.Task;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper {
    void insert(Task task);

    List<Task> findAll();

    Task findById(Integer id);
}
