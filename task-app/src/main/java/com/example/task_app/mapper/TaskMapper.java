package com.example.task_app.mapper;

import com.example.task_app.model.Task;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskMapper {
    void insert(Task task);

    List<Task> findAll();

    Task findById(Integer id);

    List<Task> findByRoleId(Integer roleId);

    void update(Task task);

    void updateSortOrder(@Param("id") Integer id, @Param("sortOrder") Integer sortOrder);

    void deleteById(Integer id);
}
