package com.example.task_app.service;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.mapper.RoleMapper;
import com.example.task_app.mapper.TaskMapper;
import com.example.task_app.model.Role;
import com.example.task_app.response.RoleResponse;
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
@Import({RoleService.class, TaskService.class})
class RoleServiceTest {
    private static final String ROLE_NAME_TEST = "TestRole";
    private static final String ROLE_NAME_UPDATED = "UpdatedRole";
    private static final String ROLE_NAME_ORIGINAL = "OriginalName";
    private static final String ROLE_NAME_JA = "開発者";
    private static final String ROLE_NAME_1 = "Role1";
    private static final String ROLE_NAME_2 = "Role2";
    private static final String ROLE_NAME_3 = "Role3";
    private static final int MAX_ROLE_NAME_LENGTH = 255;
    private static final int NON_EXISTING_ROLE_ID = 9999;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        List<Role> existingRoles = roleMapper.selectAll();
        for (Role role : existingRoles) {
            roleMapper.deleteById(role.getRoleId());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // createRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: roleIdなしで作成すると1件登録される")
    void createRole_normal01() {
        // Arrange
        RoleDto createRequest = new RoleDto(null, ROLE_NAME_TEST);

        // Act
        roleService.createRole(createRequest);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(ROLE_NAME_TEST);
        assertThat(roles.getFirst().getRoleId()).isNotNull();
    }

    @Test
    @DisplayName("normal02: roleIdありで更新すると件数を増やさず名前だけ更新する")
    void createRole_normal02() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        RoleDto updateRequest = new RoleDto(roleId, ROLE_NAME_UPDATED);

        // Act
        roleService.createRole(updateRequest);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleId()).isEqualTo(roleId);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
    }

    @Test
    @DisplayName("normal03: 複数作成すると全件が登録される")
    void createRole_normal03() {
        // Arrange
        RoleDto request1 = new RoleDto(null, ROLE_NAME_1);
        RoleDto request2 = new RoleDto(null, ROLE_NAME_2);
        RoleDto request3 = new RoleDto(null, ROLE_NAME_3);

        // Act
        roleService.createRole(request1);
        roleService.createRole(request2);
        roleService.createRole(request3);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(3);
        assertThat(roles)
                .extracting(Role::getRoleName)
                .containsExactlyInAnyOrder(ROLE_NAME_1, ROLE_NAME_2, ROLE_NAME_3);
    }

    @Test
    @DisplayName("normal04: 日本語ロール名でも作成できる")
    void createRole_normal04() {
        // Arrange
        RoleDto createRequest = new RoleDto(null, ROLE_NAME_JA);

        // Act
        roleService.createRole(createRequest);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(ROLE_NAME_JA);
    }

    @Test
    @DisplayName("normal05: 最大長のroleNameでも作成できる")
    void createRole_normal05() {
        // Arrange
        String maxLengthRoleName = "A".repeat(MAX_ROLE_NAME_LENGTH);
        RoleDto createRequest = new RoleDto(null, maxLengthRoleName);

        // Act
        roleService.createRole(createRequest);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(maxLengthRoleName);
    }

    @Test
    @DisplayName("normal06: 連続作成時にIDが増加する")
    void createRole_normal06() {
        // Arrange
        RoleDto request1 = new RoleDto(null, ROLE_NAME_1);
        RoleDto request2 = new RoleDto(null, ROLE_NAME_2);

        // Act
        roleService.createRole(request1);
        roleService.createRole(request2);

        // Assert
        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getRoleId()).isNotNull();
        assertThat(roles.get(1).getRoleId()).isNotNull();
        assertThat(roles.get(1).getRoleId()).isGreaterThan(roles.get(0).getRoleId());
    }

    @Test
    @DisplayName("error01: roleNameがnullのときDB制約違反で例外が発生する")
    void createRole_error01() {
        // Arrange
        RoleDto invalidRequest = new RoleDto(null, null);

        // Act / Assert
        assertThatThrownBy(() -> roleService.createRole(invalidRequest))
                .isInstanceOf(Exception.class);
        assertThat(roleMapper.selectAll()).isEmpty();
    }

    @Test
    @DisplayName("error02: 存在しないroleIdで更新を試みてもデータは作成されない")
    void createRole_error02() {
        // Arrange
        int nonExistingRoleId = 9999;
        RoleDto updateRequest = new RoleDto(nonExistingRoleId, ROLE_NAME_UPDATED);

        // Act
        roleService.createRole(updateRequest);

        // Assert
        assertThat(roleMapper.selectAll()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    // getAllRoles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal07: ロールが0件のとき空のリストを返す")
    void getAllRoles_normal01() {
        // Act
        List<RoleResponse> result = roleService.getAllRoles();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("normal08: ロールが存在するとき全件をtasks配列付きで返す")
    void getAllRoles_normal02() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));

        // Act
        List<RoleResponse> result = roleService.getAllRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRoleName()).isEqualTo(ROLE_NAME_TEST);
        assertThat(result.getFirst().getIsExpanded()).isTrue(); // DB DEFAULT = true
        assertThat(result.getFirst().getTasks()).isEmpty();
    }

    @Test
    @DisplayName("normal09: ロールに紐づくタスクがtasks配列に含まれる")
    void getAllRoles_normal03() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task 1", false));
        taskService.createTask(new TaskDto(null, roleId, "Task 2", true));

        // Act
        List<RoleResponse> result = roleService.getAllRoles();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTasks()).hasSize(2);
        assertThat(result.getFirst().getTasks())
                .extracting(TaskResponse::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    @DisplayName("normal10: 複数ロールにそれぞれのタスクが正しく紐づく")
    void getAllRoles_normal04() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_1));
        roleService.createRole(new RoleDto(null, ROLE_NAME_2));
        List<Role> roles = roleMapper.selectAll();
        Integer role1Id = roles.get(0).getRoleId();
        Integer role2Id = roles.get(1).getRoleId();
        taskService.createTask(new TaskDto(null, role1Id, "Task for Role1", false));
        taskService.createTask(new TaskDto(null, role2Id, "Task for Role2", false));

        // Act
        List<RoleResponse> result = roleService.getAllRoles();

        // Assert
        assertThat(result).hasSize(2);
        RoleResponse result1 = result.stream()
                .filter(r -> r.getRoleId().equals(role1Id)).findFirst().orElseThrow();
        RoleResponse result2 = result.stream()
                .filter(r -> r.getRoleId().equals(role2Id)).findFirst().orElseThrow();
        assertThat(result1.getTasks()).hasSize(1);
        assertThat(result1.getTasks().getFirst().getTitle()).isEqualTo("Task for Role1");
        assertThat(result2.getTasks()).hasSize(1);
        assertThat(result2.getTasks().getFirst().getTitle()).isEqualTo("Task for Role2");
    }

    // ─────────────────────────────────────────────────────────────────
    // updateRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal11: roleNameを変更すると名前が更新される")
    void updateRole_normal01() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        // Act
        roleService.updateRole(roleId, ROLE_NAME_UPDATED, null);

        // Assert
        Role updated = roleMapper.selectById(roleId);
        assertThat(updated.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
    }

    @Test
    @DisplayName("normal12: isExpandedをfalseに変更すると反映される")
    void updateRole_normal02() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        // Act
        roleService.updateRole(roleId, ROLE_NAME_TEST, false);

        // Assert
        Role updated = roleMapper.selectById(roleId);
        assertThat(updated.getIsExpanded()).isFalse();
    }

    @Test
    @DisplayName("normal13: isExpandedがnullのとき既存の値（DB DEFAULT=true）が保持される")
    void updateRole_normal03() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        // Act
        roleService.updateRole(roleId, ROLE_NAME_UPDATED, null);

        // Assert
        Role updated = roleMapper.selectById(roleId);
        assertThat(updated.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
        assertThat(updated.getIsExpanded()).isTrue();
    }

    @Test
    @DisplayName("normal14: 更新後のRoleResponseに紐づくタスクが含まれる")
    void updateRole_normal04() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task A", false));

        // Act
        RoleResponse result = roleService.updateRole(roleId, ROLE_NAME_UPDATED, true);

        // Assert
        assertThat(result.getRoleId()).isEqualTo(roleId);
        assertThat(result.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
        assertThat(result.getIsExpanded()).isTrue();
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().getFirst().getTitle()).isEqualTo("Task A");
    }

    @Test
    @DisplayName("error03: 存在しないroleIdで更新するとNoSuchElementExceptionが発生する")
    void updateRole_error01() {
        // Act / Assert
        assertThatThrownBy(() -> roleService.updateRole(NON_EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal15: 存在するロールを削除するとDBから消える")
    void deleteRole_normal01() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        // Act
        roleService.deleteRole(roleId);

        // Assert
        assertThat(roleMapper.selectAll()).isEmpty();
    }

    @Test
    @DisplayName("normal16: ロール削除時に紐づくタスクもCASCADE削除される")
    void deleteRole_normal02() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task 1", false));
        taskService.createTask(new TaskDto(null, roleId, "Task 2", false));
        assertThat(taskMapper.findAll()).hasSize(2);

        // Act
        roleService.deleteRole(roleId);

        // Assert
        assertThat(roleMapper.selectAll()).isEmpty();
        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error04: 存在しないroleIdで削除するとNoSuchElementExceptionが発生し、DBは変化しない")
    void deleteRole_error01() {
        // Arrange
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST));

        // Act / Assert
        assertThatThrownBy(() -> roleService.deleteRole(NON_EXISTING_ROLE_ID))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(roleMapper.selectAll()).hasSize(1);
    }
}
