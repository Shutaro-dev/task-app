package com.example.task_app.service;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.mapper.RoleMapper;
import com.example.task_app.mapper.TaskMapper;
import com.example.task_app.model.Role;
import com.example.task_app.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MybatisTest
@Import({TaskService.class, RoleService.class})
class TaskServiceTest {
    private static final String DEFAULT_ROLE_NAME = "TestRole";
    private static final String SECOND_ROLE_NAME = "SecondRole";
    private static final String TITLE_TEMP = "Temporary Task";
    private static final String TITLE_PERMANENT = "Permanent Task";
    private static final String TITLE_JA = "タスクのテスト";
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int NON_EXISTING_ROLE_ID = 9999;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private RoleMapper roleMapper;

    private Integer baseRoleId;

    @BeforeEach
    void setUp() {
        List<Role> existingRoles = roleMapper.selectAll();
        for (Role role : existingRoles) {
            roleMapper.deleteById(role.getRoleId());
        }

        roleService.createRole(new RoleDto(null, DEFAULT_ROLE_NAME));
        baseRoleId = roleMapper.selectAll().getFirst().getRoleId();
    }

    @Test
    @DisplayName("normal01: isPermanent=falseのタスクを作成できる")
    void createTask_normal01() {
        // Arrange
        TaskDto createRequest = new TaskDto(null, baseRoleId, TITLE_TEMP, false);

        // Act
        taskService.createTask(createRequest);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        Task created = taskMapper.findById(tasks.getFirst().getTaskId());
        assertThat(created.getTitle()).isEqualTo(TITLE_TEMP);
        assertThat(created.getRoleId()).isEqualTo(baseRoleId);
        assertThat(created.isPermanent()).isFalse();
    }

    @Test
    @DisplayName("normal02: isPermanent=trueのタスクを作成できる")
    void createTask_normal02() {
        // Arrange
        TaskDto createRequest = new TaskDto(null, baseRoleId, TITLE_PERMANENT, true);

        // Act
        taskService.createTask(createRequest);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        Task created = taskMapper.findById(tasks.getFirst().getTaskId());
        assertThat(created.getTitle()).isEqualTo(TITLE_PERMANENT);
        assertThat(created.isPermanent()).isTrue();
    }

    @Test
    @DisplayName("normal03: 複数タスクを作成できる")
    void createTask_normal03() {
        // Arrange
        TaskDto request1 = new TaskDto(null, baseRoleId, "Task 1", false);
        TaskDto request2 = new TaskDto(null, baseRoleId, "Task 2", true);
        TaskDto request3 = new TaskDto(null, baseRoleId, "Task 3", false);

        // Act
        taskService.createTask(request1);
        taskService.createTask(request2);
        taskService.createTask(request3);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(3);
        assertThat(tasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
    }

    @Test
    @DisplayName("normal04: 日本語タイトルで作成できる")
    void createTask_normal04() {
        // Arrange
        TaskDto createRequest = new TaskDto(null, baseRoleId, TITLE_JA, false);

        // Act
        taskService.createTask(createRequest);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getTitle()).isEqualTo(TITLE_JA);
    }

    @Test
    @DisplayName("normal05: 最大長タイトルで作成できる")
    void createTask_normal05() {
        // Arrange
        String maxLengthTitle = "T".repeat(MAX_TITLE_LENGTH);
        TaskDto createRequest = new TaskDto(null, baseRoleId, maxLengthTitle, false);

        // Act
        taskService.createTask(createRequest);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getTitle()).isEqualTo(maxLengthTitle);
    }

    @Test
    @DisplayName("normal06: 異なるroleIdに対してそれぞれ作成できる")
    void createTask_normal06() {
        // Arrange
        roleService.createRole(new RoleDto(null, SECOND_ROLE_NAME));
        Integer secondRoleId = roleMapper.selectAll().stream()
                .filter(role -> SECOND_ROLE_NAME.equals(role.getRoleName()))
                .findFirst()
                .map(Role::getRoleId)
                .orElseThrow();

        TaskDto requestForRole1 = new TaskDto(null, baseRoleId, "Task for Role 1", false);
        TaskDto requestForRole2 = new TaskDto(null, secondRoleId, "Task for Role 2", false);

        // Act
        taskService.createTask(requestForRole1);
        taskService.createTask(requestForRole2);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::getRoleId)
                .containsExactlyInAnyOrder(baseRoleId, secondRoleId);
    }

    @Test
    @DisplayName("error01: titleがnullのときDB制約違反で例外が発生する")
    void createTask_error01() {
        // Arrange
        TaskDto invalidRequest = new TaskDto(null, baseRoleId, null, false);

        // Act / Assert
        assertThatThrownBy(() -> taskService.createTask(invalidRequest))
                .isInstanceOf(Exception.class);
        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error02: 存在しないroleIdのとき外部キー制約違反で例外が発生する")
    void createTask_error02() {
        // Arrange
        TaskDto invalidRequest = new TaskDto(null, NON_EXISTING_ROLE_ID, TITLE_TEMP, false);

        // Act / Assert
        assertThatThrownBy(() -> taskService.createTask(invalidRequest))
                .isInstanceOf(Exception.class);
        assertThat(taskMapper.findAll()).isEmpty();
    }
}
