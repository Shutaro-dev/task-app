package com.example.task_app.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.example.task_app.model.Role;
import java.util.List;

@Mapper
public interface RoleMapper {

    List<Role> selectAll();

    Role selectById(Integer roleId);

    int insert(Role role);

    int update(Role role);

    int updateWithExpanded(Role role);

    int deleteById(Integer roleId);
}