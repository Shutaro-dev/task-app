package com.example.task_app.service;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.form.ReorderItem;
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
    private static final String DEFAULT_ROLE_NAME  = "TestRole";
    private static final String SECOND_ROLE_NAME   = "SecondRole";
    private static final String TITLE_TEMP         = "Temporary Task";
    private static final String TITLE_PERMANENT    = "Permanent Task";
    private static final String TITLE_JA           = "タスクのテスト";
    private static final int    MAX_TITLE_LENGTH   = 255;
    private static final int    NON_EXISTING_ROLE_ID  = 9999;
    private static final int    NON_EXISTING_TASK_ID  = 9998;

    @Autowired private TaskService taskService;
    @Autowired private RoleService roleService;
    @Autowired private TaskMapper  taskMapper;
    @Autowired private RoleMapper  roleMapper;

    private Integer baseRoleId;

    @BeforeEach
    void setUp() {
        roleMapper.selectAll().forEach(r -> roleMapper.deleteById(r.getRoleId()));
        roleService.createRole(new RoleDto(null, DEFAULT_ROLE_NAME, null));
        baseRoleId = roleMapper.selectAll().getFirst().getRoleId();
    }

    // ─────────────────────────────────────────────────────────────────
    // createTask
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: isPermanent=falseのタスクを作成できる")
    void createTask_normal01() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));

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
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_PERMANENT, true));

        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(taskMapper.findById(tasks.getFirst().getTaskId()).isPermanent()).isTrue();
    }

    @Test
    @DisplayName("normal03: 複数タスクを作成できる")
    void createTask_normal03() {
        taskService.createTask(new TaskDto(null, baseRoleId, "Task 1", false));
        taskService.createTask(new TaskDto(null, baseRoleId, "Task 2", true));
        taskService.createTask(new TaskDto(null, baseRoleId, "Task 3", false));

        assertThat(taskMapper.findAll()).hasSize(3)
                .extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
    }

    @Test
    @DisplayName("normal04: 日本語タイトルで作成できる")
    void createTask_normal04() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_JA, false));

        assertThat(taskMapper.findAll().getFirst().getTitle()).isEqualTo(TITLE_JA);
    }

    @Test
    @DisplayName("normal05: 最大長タイトルで作成できる")
    void createTask_normal05() {
        String maxLengthTitle = "T".repeat(MAX_TITLE_LENGTH);
        taskService.createTask(new TaskDto(null, baseRoleId, maxLengthTitle, false));

        assertThat(taskMapper.findAll().getFirst().getTitle()).isEqualTo(maxLengthTitle);
    }

    @Test
    @DisplayName("normal06: 異なるroleIdに対してそれぞれ作成できる")
    void createTask_normal06() {
        roleService.createRole(new RoleDto(null, SECOND_ROLE_NAME, null));
        Integer secondRoleId = roleMapper.selectAll().stream()
                .filter(role -> SECOND_ROLE_NAME.equals(role.getRoleName()))
                .findFirst().map(Role::getRoleId).orElseThrow();

        taskService.createTask(new TaskDto(null, baseRoleId,    "Task for Role 1", false));
        taskService.createTask(new TaskDto(null, secondRoleId,  "Task for Role 2", false));

        assertThat(taskMapper.findAll()).hasSize(2)
                .extracting(Task::getRoleId)
                .containsExactlyInAnyOrder(baseRoleId, secondRoleId);
    }

    @Test
    @DisplayName("error01: titleがnullのときDB制約違反で例外が発生する")
    void createTask_error01() {
        assertThatThrownBy(() -> taskService.createTask(new TaskDto(null, baseRoleId, null, false)))
                .isInstanceOf(Exception.class);
        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error02: 存在しないroleIdのとき外部キー制約違反で例外が発生する")
    void createTask_error02() {
        assertThatThrownBy(() -> taskService.createTask(new TaskDto(null, NON_EXISTING_ROLE_ID, TITLE_TEMP, false)))
                .isInstanceOf(Exception.class);
        assertThat(taskMapper.findAll()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    // getTasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal07: タスクが0件のとき空のリストを返す")
    void getTasks_normal01() {
        assertThat(taskService.getTasks()).isEmpty();
    }

    @Test
    @DisplayName("normal08: タスクが1件のとき1件のリストを返す")
    void getTasks_normal02() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));

        List<TaskResponse> result = taskService.getTasks();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo(TITLE_TEMP);
        assertThat(result.getFirst().getRoleId()).isEqualTo(baseRoleId);
        assertThat(result.getFirst().isPermanent()).isFalse();
    }

    @Test
    @DisplayName("normal09: 複数タスクが存在するとき全件を返す")
    void getTasks_normal03() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_PERMANENT, true));

        assertThat(taskService.getTasks()).hasSize(2)
                .extracting(TaskResponse::getTitle)
                .containsExactlyInAnyOrder(TITLE_TEMP, TITLE_PERMANENT);
    }

    // ─────────────────────────────────────────────────────────────────
    // updateTask
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal10: titleを変更すると更新されたタスクが返る")
    void updateTask_normal01() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        TaskResponse result = taskService.updateTask(taskId, "Updated Title", false);

        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.isPermanent()).isFalse();
        assertThat(taskMapper.findById(taskId).getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("normal11: isPermanentをtrueに変更するとDBに反映される")
    void updateTask_normal02() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        TaskResponse result = taskService.updateTask(taskId, TITLE_TEMP, true);

        assertThat(result.isPermanent()).isTrue();
        assertThat(taskMapper.findById(taskId).isPermanent()).isTrue();
    }

    @Test
    @DisplayName("normal12: isPermanentをfalseに変更するとDBに反映される")
    void updateTask_normal03() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_PERMANENT, true));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        TaskResponse result = taskService.updateTask(taskId, TITLE_PERMANENT, false);

        assertThat(result.isPermanent()).isFalse();
        assertThat(taskMapper.findById(taskId).isPermanent()).isFalse();
    }

    @Test
    @DisplayName("normal13: 更新後のTaskResponseにroleIdが正しく含まれ、DBにも永続化される")
    void updateTask_normal04() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        TaskResponse result = taskService.updateTask(taskId, "New Title", false);

        assertThat(result.getRoleId()).isEqualTo(baseRoleId);
        Task persisted = taskMapper.findById(taskId);
        assertThat(persisted.getRoleId()).isEqualTo(baseRoleId);
        assertThat(persisted.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("error03: 存在しないIDで更新するとNoSuchElementExceptionが発生する")
    void updateTask_error01() {
        assertThatThrownBy(() -> taskService.updateTask(NON_EXISTING_TASK_ID, TITLE_TEMP, false))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // reorderTasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal14: reorderTasksでsort_orderがDBに反映される")
    void reorderTasks_normal01() {
        taskService.createTask(new TaskDto(null, baseRoleId, "Task A", false));
        taskService.createTask(new TaskDto(null, baseRoleId, "Task B", false));
        List<Task> tasks = taskMapper.findAll();
        Integer idA = tasks.get(0).getTaskId();
        Integer idB = tasks.get(1).getTaskId();

        // B→A の順に並び替え
        taskService.reorderTasks(List.of(
                makeReorderItem(idB, 0),
                makeReorderItem(idA, 1)
        ));

        assertThat(taskMapper.findById(idB).getSortOrder()).isEqualTo(0);
        assertThat(taskMapper.findById(idA).getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("normal15: 空リストでreorderTasksを呼び出しても例外が発生しない")
    void reorderTasks_normal02() {
        taskService.reorderTasks(List.of());
        // 例外なく完了すればOK
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteTask
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal16: 存在するタスクを削除するとDBから消える")
    void deleteTask_normal01() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));
        Integer taskId = taskMapper.findAll().getFirst().getTaskId();

        taskService.deleteTask(taskId);

        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error04: 存在しないIDで削除するとNoSuchElementExceptionが発生し、DBは変化しない")
    void deleteTask_error01() {
        taskService.createTask(new TaskDto(null, baseRoleId, TITLE_TEMP, false));

        assertThatThrownBy(() -> taskService.deleteTask(NON_EXISTING_TASK_ID))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(taskMapper.findAll()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private ReorderItem makeReorderItem(Integer id, Integer sortOrder) {
        try {
            ReorderItem item = new ReorderItem();
            var idField = ReorderItem.class.getDeclaredField("id");
            var soField = ReorderItem.class.getDeclaredField("sortOrder");
            idField.setAccessible(true);
            soField.setAccessible(true);
            idField.set(item, id);
            soField.set(item, sortOrder);
            return item;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
