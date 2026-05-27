package com.example.task_app.service;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.dto.TaskDto;
import com.example.task_app.form.ReorderItem;
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
    private static final String ROLE_NAME_TEST    = "TestRole";
    private static final String ROLE_NAME_UPDATED = "UpdatedRole";
    private static final String ROLE_NAME_ORIGINAL = "OriginalName";
    private static final String ROLE_NAME_JA      = "開発者";
    private static final String ROLE_NAME_1       = "Role1";
    private static final String ROLE_NAME_2       = "Role2";
    private static final String ROLE_NAME_3       = "Role3";
    private static final String COLOR_BLUE        = "#4a90d9";
    private static final String COLOR_RED         = "#e74c3c";
    private static final int    MAX_ROLE_NAME_LENGTH  = 255;
    private static final int    NON_EXISTING_ROLE_ID  = 9999;

    @Autowired private RoleService roleService;
    @Autowired private TaskService taskService;
    @Autowired private RoleMapper  roleMapper;
    @Autowired private TaskMapper  taskMapper;

    @BeforeEach
    void setUp() {
        roleMapper.selectAll().forEach(r -> roleMapper.deleteById(r.getRoleId()));
    }

    // ─────────────────────────────────────────────────────────────────
    // createRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: roleIdなしで作成すると1件登録される")
    void createRole_normal01() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));

        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(ROLE_NAME_TEST);
        assertThat(roles.getFirst().getRoleId()).isNotNull();
    }

    @Test
    @DisplayName("normal02: roleIdありで更新すると件数を増やさず名前だけ更新する")
    void createRole_normal02() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        roleService.createRole(new RoleDto(roleId, ROLE_NAME_UPDATED, null));

        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRoleId()).isEqualTo(roleId);
        assertThat(roles.getFirst().getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
    }

    @Test
    @DisplayName("normal03: 複数作成すると全件が登録される")
    void createRole_normal03() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_1, null));
        roleService.createRole(new RoleDto(null, ROLE_NAME_2, null));
        roleService.createRole(new RoleDto(null, ROLE_NAME_3, null));

        assertThat(roleMapper.selectAll())
                .hasSize(3)
                .extracting(Role::getRoleName)
                .containsExactlyInAnyOrder(ROLE_NAME_1, ROLE_NAME_2, ROLE_NAME_3);
    }

    @Test
    @DisplayName("normal04: 日本語ロール名でも作成できる")
    void createRole_normal04() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_JA, null));

        assertThat(roleMapper.selectAll()).hasSize(1);
        assertThat(roleMapper.selectAll().getFirst().getRoleName()).isEqualTo(ROLE_NAME_JA);
    }

    @Test
    @DisplayName("normal05: 最大長のroleNameでも作成できる")
    void createRole_normal05() {
        String maxLengthRoleName = "A".repeat(MAX_ROLE_NAME_LENGTH);
        roleService.createRole(new RoleDto(null, maxLengthRoleName, null));

        assertThat(roleMapper.selectAll().getFirst().getRoleName()).isEqualTo(maxLengthRoleName);
    }

    @Test
    @DisplayName("normal06: 連続作成時にIDが増加する")
    void createRole_normal06() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_1, null));
        roleService.createRole(new RoleDto(null, ROLE_NAME_2, null));

        List<Role> roles = roleMapper.selectAll();
        assertThat(roles).hasSize(2);
        assertThat(roles.get(1).getRoleId()).isGreaterThan(roles.get(0).getRoleId());
    }

    @Test
    @DisplayName("normal07: colorを指定して作成するとDBに保存される")
    void createRole_normal07() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, COLOR_BLUE));

        Role created = roleMapper.selectAll().getFirst();
        assertThat(created.getColor()).isEqualTo(COLOR_BLUE);
    }

    @Test
    @DisplayName("error01: roleNameがnullのときDB制約違反で例外が発生する")
    void createRole_error01() {
        assertThatThrownBy(() -> roleService.createRole(new RoleDto(null, null, null)))
                .isInstanceOf(Exception.class);
        assertThat(roleMapper.selectAll()).isEmpty();
    }

    @Test
    @DisplayName("error02: 存在しないroleIdで更新を試みるとNoSuchElementExceptionが発生しDBは変化しない")
    void createRole_error02() {
        assertThatThrownBy(() -> roleService.createRole(new RoleDto(NON_EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null)))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(roleMapper.selectAll()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────
    // getAllRoles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal08: ロールが0件のとき空のリストを返す")
    void getAllRoles_normal01() {
        assertThat(roleService.getAllRoles()).isEmpty();
    }

    @Test
    @DisplayName("normal09: ロールが存在するとき全件をtasks配列付きで返す")
    void getAllRoles_normal02() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));

        List<RoleResponse> result = roleService.getAllRoles();
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRoleName()).isEqualTo(ROLE_NAME_TEST);
        assertThat(result.getFirst().getIsExpanded()).isTrue();
        assertThat(result.getFirst().getTasks()).isEmpty();
    }

    @Test
    @DisplayName("normal10: ロールに紐づくタスクがtasks配列に含まれる")
    void getAllRoles_normal03() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task 1", false));
        taskService.createTask(new TaskDto(null, roleId, "Task 2", true));

        List<RoleResponse> result = roleService.getAllRoles();
        assertThat(result.getFirst().getTasks()).hasSize(2)
                .extracting(TaskResponse::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    @DisplayName("normal11: 複数ロールにそれぞれのタスクが正しく紐づく")
    void getAllRoles_normal04() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_1, null));
        roleService.createRole(new RoleDto(null, ROLE_NAME_2, null));
        List<Role> roles = roleMapper.selectAll();
        Integer role1Id = roles.get(0).getRoleId();
        Integer role2Id = roles.get(1).getRoleId();
        taskService.createTask(new TaskDto(null, role1Id, "Task for Role1", false));
        taskService.createTask(new TaskDto(null, role2Id, "Task for Role2", false));

        List<RoleResponse> result = roleService.getAllRoles();
        RoleResponse r1 = result.stream().filter(r -> r.getRoleId().equals(role1Id)).findFirst().orElseThrow();
        RoleResponse r2 = result.stream().filter(r -> r.getRoleId().equals(role2Id)).findFirst().orElseThrow();
        assertThat(r1.getTasks()).hasSize(1);
        assertThat(r1.getTasks().getFirst().getTitle()).isEqualTo("Task for Role1");
        assertThat(r2.getTasks()).hasSize(1);
        assertThat(r2.getTasks().getFirst().getTitle()).isEqualTo("Task for Role2");
    }

    @Test
    @DisplayName("normal12: colorが設定されたロールのgetAllRolesでcolorが返る")
    void getAllRoles_normal05() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, COLOR_BLUE));

        RoleResponse result = roleService.getAllRoles().getFirst();
        assertThat(result.getColor()).isEqualTo(COLOR_BLUE);
    }

    // ─────────────────────────────────────────────────────────────────
    // updateRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal13: roleNameを変更すると名前が更新される")
    void updateRole_normal01() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        roleService.updateRole(roleId, ROLE_NAME_UPDATED, null, null);

        assertThat(roleMapper.selectById(roleId).getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
    }

    @Test
    @DisplayName("normal14: isExpandedをfalseに変更すると反映される")
    void updateRole_normal02() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        roleService.updateRole(roleId, ROLE_NAME_TEST, false, null);

        assertThat(roleMapper.selectById(roleId).getIsExpanded()).isFalse();
    }

    @Test
    @DisplayName("normal15: isExpandedがnullのとき既存の値（DB DEFAULT=true）が保持される")
    void updateRole_normal03() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        roleService.updateRole(roleId, ROLE_NAME_UPDATED, null, null);

        Role updated = roleMapper.selectById(roleId);
        assertThat(updated.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
        assertThat(updated.getIsExpanded()).isTrue();
    }

    @Test
    @DisplayName("normal16: 更新後のRoleResponseに紐づくタスクが含まれ、DBにも永続化される")
    void updateRole_normal04() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_ORIGINAL, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task A", false));

        RoleResponse result = roleService.updateRole(roleId, ROLE_NAME_UPDATED, true, null);

        assertThat(result.getRoleId()).isEqualTo(roleId);
        assertThat(result.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
        assertThat(result.getIsExpanded()).isTrue();
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().getFirst().getTitle()).isEqualTo("Task A");
        Role persisted = roleMapper.selectById(roleId);
        assertThat(persisted.getRoleName()).isEqualTo(ROLE_NAME_UPDATED);
        assertThat(persisted.getIsExpanded()).isTrue();
    }

    @Test
    @DisplayName("normal17: colorを変更するとDBに反映されレスポンスに含まれる")
    void updateRole_normal05() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, COLOR_BLUE));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        RoleResponse result = roleService.updateRole(roleId, ROLE_NAME_TEST, null, COLOR_RED);

        assertThat(result.getColor()).isEqualTo(COLOR_RED);
        assertThat(roleMapper.selectById(roleId).getColor()).isEqualTo(COLOR_RED);
    }

    @Test
    @DisplayName("error03: 存在しないroleIdで更新するとNoSuchElementExceptionが発生する")
    void updateRole_error01() {
        assertThatThrownBy(() -> roleService.updateRole(NON_EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // reorderRoles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal18: reorderRolesでsort_orderがDBに反映される")
    void reorderRoles_normal01() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_1, null));
        roleService.createRole(new RoleDto(null, ROLE_NAME_2, null));
        List<Role> roles = roleMapper.selectAll();
        Integer id1 = roles.get(0).getRoleId();
        Integer id2 = roles.get(1).getRoleId();

        // 2→1 の順に並び替え
        roleService.reorderRoles(List.of(
                makeReorderItem(id2, 0),
                makeReorderItem(id1, 1)
        ));

        assertThat(roleMapper.selectById(id2).getSortOrder()).isEqualTo(0);
        assertThat(roleMapper.selectById(id1).getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("normal19: 空リストでreorderRolesを呼び出しても例外が発生しない")
    void reorderRoles_normal02() {
        roleService.reorderRoles(List.of());
        // 例外なく完了すればOK
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteRole
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal20: 存在するロールを削除するとDBから消える")
    void deleteRole_normal01() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

        roleService.deleteRole(roleId);

        assertThat(roleMapper.selectAll()).isEmpty();
    }

    @Test
    @DisplayName("normal21: ロール削除時に紐づくタスクもCASCADE削除される")
    void deleteRole_normal02() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));
        Integer roleId = roleMapper.selectAll().getFirst().getRoleId();
        taskService.createTask(new TaskDto(null, roleId, "Task 1", false));
        taskService.createTask(new TaskDto(null, roleId, "Task 2", false));
        assertThat(taskMapper.findAll()).hasSize(2);

        roleService.deleteRole(roleId);

        assertThat(roleMapper.selectAll()).isEmpty();
        assertThat(taskMapper.findAll()).isEmpty();
    }

    @Test
    @DisplayName("error04: 存在しないroleIdで削除するとNoSuchElementExceptionが発生し、DBは変化しない")
    void deleteRole_error01() {
        roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));

        assertThatThrownBy(() -> roleService.deleteRole(NON_EXISTING_ROLE_ID))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(roleMapper.selectAll()).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private ReorderItem makeReorderItem(Integer id, Integer sortOrder) {
        // ReorderItem は @Getter のみ（@Setter なし）のため、JSON デシリアライズ相当の設定を行う
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
