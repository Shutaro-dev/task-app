package com.example.task_app.service;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.mapper.RoleMapper;
import com.example.task_app.mapper.TaskMapper;
import com.example.task_app.model.Role;
import com.example.task_app.model.Task;
import com.example.task_app.response.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.NoSuchElementException;

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
    private static final int NON_EXISTING_TASK_ID = 9998;

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

    // ─────────────────────────────────────────────────────────────────
    // getTasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal07: タスクが0件のとき空のリストを返す")
    void getTasks_normal01() {
        // Act
        List<TaskResponse> result = taskService.getTasks();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("normal08: タスクが1件のとき1件のリストを返す")
    void getTasks_normal02() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));

        // Act
        List<TaskResponse> result = taskService.getTasks();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo(TITLE_TEMP);
        assertThat(result.getFirst().getRoleId()).isEqualTo(baseRoleId);
        assertThat(result.getFirst().isPermanent()).isFalse();
    }

    @Test
    @DisplayName("normal09: 複数タスクが存在するとき全件を返す")
    void getTasks_normal03() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_PERMANENT, true));

        // Act
        List<TaskResponse> result = taskService.getTasks();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TaskResponse::getTitle)
                .containsExactlyInAnyOrder(TITLE_TEMP, TITLE_PERMANENT);
    }

    // ─────────────────────────────────────────────────────────────────
    // updateTask
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal10: titleを変更すると更新されたタスクが返る")
    void updateTask_normal01() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();
        String updatedTitle = "Updated Title";

        // Act
        TaskResponse result = taskService.updateTask(taskId, updatedTitle, false);

        // Assert
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getTitle()).isEqualTo(updatedTitle);
        assertThat(result.isPermanent()).isFalse();
        assertThat(taskMapper.findById(taskId).getTitle()).isEqualTo(updatedTitle);
    }

    @Test
    @DisplayName("normal11: isPermanentをtrueに変更するとDBに反映される")
    void updateTask_normal02() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        // Act
        TaskResponse result = taskService.updateTask(taskId, TITLE_TEMP, true);

        // Assert
        assertThat(result.isPermanent()).isTrue();
        assertThat(taskMapper.findById(taskId).isPermanent()).isTrue();
    }

    @Test
    @DisplayName("normal12: isPermanentをfalseに変更するとDBに反映される")
    void updateTask_normal03() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_PERMANENT, true));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        // Act
        TaskResponse result = taskService.updateTask(taskId, TITLE_PERMANENT, false);

        // Assert
        assertThat(result.isPermanent()).isFalse();
        assertThat(taskMapper.findById(taskId).isPermanent()).isFalse();
    }

    @Test
    @DisplayName("normal13: 更新後のTaskResponseにroleIdが正しく含まれ、DBにも永続化される")
    void updateTask_normal04() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        // Act
        TaskResponse result = taskService.updateTask(taskId, "New Title", false);

        // Assert
        assertThat(result.getRoleId()).isEqualTo(baseRoleId);
        Task persisted = taskMapper.findById(taskId);
        assertThat(persisted.getRoleId()).isEqualTo(baseRoleId);
        assertThat(persisted.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("error03: 存在しないIDで更新するとNoSuchElementExceptionが発生する")
    void updateTask_error01() {
        // Act / Assert
        assertThatThrownBy(() -> taskService.updateTask(NON_EXISTING_TASK_ID, TITLE_TEMP, false))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteTask
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal14: 存在するタスクを削除するとDBから消える")
    void deleteTask_normal01() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        // Act
        taskService.deleteTask(taskId);

        // Assert
        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error04: 存在しないIDで削除するとNoSuchElementExceptionが発生し、DBは変化しない")
    void deleteTask_error01() {
        // Arrange
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));

        // Act / Assert
        assertThatThrownBy(() -> taskService.deleteTask(NON_EXISTING_TASK_ID))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(taskMapper.findAll()).hasSize(1);
    }
}
