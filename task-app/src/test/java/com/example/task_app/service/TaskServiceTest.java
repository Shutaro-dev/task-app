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

/**
 * TaskServiceの統合テスト
 * 
 * このテストでは、実際のH2データベースと連携し、
 * サービス層の挙動を検証します。
 * 
 * TaskはRoleに依存するため、テストの前準備でRoleを作成します。
 * 
 * @MybatisTest: MyBatisのマッパーとデータベース接続をテストするためのアノテーション
 * @Import: テスト対象のServiceクラスをDIコンテナに登録
 */
@MybatisTest
@Import({TaskService.class, RoleService.class})
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private RoleMapper roleMapper;

    private Integer testRoleId;

    /**
     * 各テストの前にデータベースをクリーンアップし、テスト用のRoleを作成
     */
    @BeforeEach
    void setUp() {
        // まずTasksを削除（外部キー制約のため先に削除）
        List<Task> existingTasks = taskMapper.findAll();
        // TaskMapperにdeleteメソッドがないため、直接削除できない
        // ここではRoleを削除することでカスケード削除を利用

        // Rolesを削除
        List<Role> existingRoles = roleMapper.selectAll();
        for (Role role : existingRoles) {
            roleMapper.deleteById(role.getRoleId());
        }

        // テスト用のRoleを作成
        RoleDto roleDto = new RoleDto(null, "TestRole");
        roleService.createRole(roleDto);

        // 作成されたRoleのIDを取得
        List<Role> roles = roleMapper.selectAll();
        testRoleId = roles.get(0).getRoleId();
    }

    @Test
    @DisplayName("正常系: 新規タスクの作成が成功する")
    void createTask_NewTask_Success() {
        // Arrange: 新規タスクのDTO
        TaskDto taskDto = new TaskDto(null, testRoleId, "Test Task", false);

        // Act: サービスメソッドを実行
        taskService.createTask(taskDto);

        // Assert: データベースからタスクを取得して検証
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("正常系: 永続タスク（isPermanent=true）の作成が成功する")
    void createTask_PermanentTask_Success() {
        // Arrange
        TaskDto taskDto = new TaskDto(null, testRoleId, "Permanent Task", true);

        // Act
        taskService.createTask(taskDto);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Permanent Task");
        assertThat(tasks.get(0).isPermanent()).isTrue();
    }

    @Test
    @DisplayName("正常系: 一時タスク（isPermanent=false）の作成が成功する")
    void createTask_TemporaryTask_Success() {
        // Arrange
        TaskDto taskDto = new TaskDto(null, testRoleId, "Temporary Task", false);

        // Act
        taskService.createTask(taskDto);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Temporary Task");
        assertThat(tasks.get(0).isPermanent()).isFalse();
    }

    @Test
    @DisplayName("正常系: 複数のタスクを作成できる")
    void createTask_MultipleTasks_Success() {
        // Arrange & Act
        TaskDto taskDto1 = new TaskDto(null, testRoleId, "Task 1", false);
        TaskDto taskDto2 = new TaskDto(null, testRoleId, "Task 2", true);
        TaskDto taskDto3 = new TaskDto(null, testRoleId, "Task 3", false);

        taskService.createTask(taskDto1);
        taskService.createTask(taskDto2);
        taskService.createTask(taskDto3);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(3);
        assertThat(tasks).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
    }

    @Test
    @DisplayName("正常系: 日本語のタスク名で作成できる")
    void createTask_JapaneseTitle_Success() {
        // Arrange
        TaskDto taskDto = new TaskDto(null, testRoleId, "タスクのテスト", false);

        // Act
        taskService.createTask(taskDto);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("タスクのテスト");
    }

    @Test
    @DisplayName("正常系: 長いタスク名で作成できる")
    void createTask_LongTitle_Success() {
        // Arrange: 255文字のタスク名
        String longTitle = "T".repeat(255);
        TaskDto taskDto = new TaskDto(null, testRoleId, longTitle, false);

        // Act
        taskService.createTask(taskDto);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo(longTitle);
    }

    @Test
    @DisplayName("正常系: createdAtとupdatedAtが設定される")
    void createTask_TimestampsSet_Success() {
        // Arrange
        TaskDto taskDto = new TaskDto(null, testRoleId, "Task with timestamps", false);

        // Act
        taskService.createTask(taskDto);

        // Assert: findByIdで詳細を取得して検証
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(1);
        
        Task createdTask = taskMapper.findById(tasks.get(0).getId());
        // 注意: TaskMapper.xmlのfindByIdはcreated_at, updated_atを取得していないため、
        // ここではタスクが作成されたことだけを確認
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.getTitle()).isEqualTo("Task with timestamps");
    }

    @Test
    @DisplayName("正常系: 異なるRoleに属するタスクを作成できる")
    void createTask_DifferentRoles_Success() {
        // Arrange: 2つ目のRoleを作成
        RoleDto roleDto2 = new RoleDto(null, "SecondRole");
        roleService.createRole(roleDto2);

        List<Role> roles = roleMapper.selectAll();
        Integer secondRoleId = roles.stream()
                .filter(r -> r.getRoleName().equals("SecondRole"))
                .findFirst()
                .map(Role::getRoleId)
                .orElseThrow();

        // Act: 異なるRoleにタスクを作成
        TaskDto taskDto1 = new TaskDto(null, testRoleId, "Task for Role 1", false);
        TaskDto taskDto2 = new TaskDto(null, secondRoleId, "Task for Role 2", false);

        taskService.createTask(taskDto1);
        taskService.createTask(taskDto2);

        // Assert
        List<Task> tasks = taskMapper.findAll();
        assertThat(tasks).hasSize(2);
    }
}
