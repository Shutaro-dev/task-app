package com.example.task_app.controller;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.response.RoleResponse;
import com.example.task_app.response.TaskResponse;
import com.example.task_app.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleRestController.class)
class RoleRestControllerTest {
    private static final String API_PATH = "/api/roles";
    private static final String ROLE_NAME_TEST = "TestRole";
    private static final String ROLE_NAME_UPDATED = "UpdatedRole";
    private static final int EXISTING_ROLE_ID = 1;
    private static final int NON_EXISTING_ROLE_ID = 9999;
    private static final int TASK_ID = 10;
    private static final String TASK_TITLE = "Task 1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleService roleService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/roles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: roleNameのみの登録リクエストで200を返し、Serviceに正しいDTOを渡す")
    void createRole_normal01() throws Exception {
        // Arrange
        String requestBody = toJsonBody("roleName", ROLE_NAME_TEST);
        doNothing().when(roleService).createRole(any(RoleDto.class));
        ArgumentCaptor<RoleDto> roleDtoCaptor = ArgumentCaptor.forClass(RoleDto.class);

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Assert
        verify(roleService, times(1)).createRole(roleDtoCaptor.capture());
        RoleDto captured = roleDtoCaptor.getValue();
        assertNull(captured.getRoleId());
        assertEquals(ROLE_NAME_TEST, captured.getRoleName());
    }

    @Test
    @DisplayName("normal02: roleId付き登録リクエストで200を返し、Serviceに正しいDTOを渡す")
    void createRole_normal02() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "roleId", EXISTING_ROLE_ID,
                "roleName", ROLE_NAME_UPDATED
        );
        doNothing().when(roleService).createRole(any(RoleDto.class));
        ArgumentCaptor<RoleDto> roleDtoCaptor = ArgumentCaptor.forClass(RoleDto.class);

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Assert
        verify(roleService, times(1)).createRole(roleDtoCaptor.capture());
        RoleDto captured = roleDtoCaptor.getValue();
        assertEquals(EXISTING_ROLE_ID, captured.getRoleId());
        assertEquals(ROLE_NAME_UPDATED, captured.getRoleName());
    }

    @Test
    @DisplayName("error01: roleNameが空文字のとき400を返し、Serviceを呼ばない")
    void createRole_error01() throws Exception {
        // Arrange
        String requestBody = toJsonBody("roleName", "");

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("error02: roleNameが未指定のとき400を返し、Serviceを呼ばない")
    void createRole_error02() throws Exception {
        // Arrange
        String requestBody = toJsonBody("roleId", EXISTING_ROLE_ID);

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("error03: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void createRole_error03() throws Exception {
        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).createRole(any(RoleDto.class));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/roles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal03: ロールが存在する場合、200とtasks付きのロール一覧を返す")
    void getAllRoles_normal01() throws Exception {
        // Arrange
        TaskResponse task = new TaskResponse(TASK_ID, EXISTING_ROLE_ID, TASK_TITLE, false);
        RoleResponse role = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_TEST, true, List.of(task));
        when(roleService.getAllRoles()).thenReturn(List.of(role));

        // Act & Assert
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].roleId").value(EXISTING_ROLE_ID))
                .andExpect(jsonPath("$[0].roleName").value(ROLE_NAME_TEST))
                .andExpect(jsonPath("$[0].isExpanded").value(true))
                .andExpect(jsonPath("$[0].tasks").isArray())
                .andExpect(jsonPath("$[0].tasks[0].taskId").value(TASK_ID))
                .andExpect(jsonPath("$[0].tasks[0].title").value(TASK_TITLE));

        verify(roleService, times(1)).getAllRoles();
    }

    @Test
    @DisplayName("normal04: ロールが0件の場合、200と空配列を返す")
    void getAllRoles_normal02() throws Exception {
        // Arrange
        when(roleService.getAllRoles()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("normal05: isExpanded=falseのロールが正しくシリアライズされる")
    void getAllRoles_normal03() throws Exception {
        // Arrange
        RoleResponse collapsedRole = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_TEST, false, Collections.emptyList());
        when(roleService.getAllRoles()).thenReturn(List.of(collapsedRole));

        // Act & Assert
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isExpanded").value(false))
                .andExpect(jsonPath("$[0].tasks").isArray())
                .andExpect(jsonPath("$[0].tasks").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/roles/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal06: 正常な更新リクエストで200と更新済みロールを返す")
    void updateRole_normal01() throws Exception {
        // Arrange
        RoleResponse updated = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false, Collections.emptyList());
        when(roleService.updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false)).thenReturn(updated);
        String requestBody = toJsonBody("roleName", ROLE_NAME_UPDATED, "isExpanded", false);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(EXISTING_ROLE_ID))
                .andExpect(jsonPath("$.roleName").value(ROLE_NAME_UPDATED))
                .andExpect(jsonPath("$.isExpanded").value(false));

        verify(roleService, times(1)).updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false);
    }

    @Test
    @DisplayName("normal07: isExpandedを省略したリクエストで200を返し、nullをServiceに渡す")
    void updateRole_normal02() throws Exception {
        // Arrange
        RoleResponse updated = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, true, Collections.emptyList());
        when(roleService.updateRole(eq(EXISTING_ROLE_ID), eq(ROLE_NAME_UPDATED), isNull())).thenReturn(updated);
        String requestBody = toJsonBody("roleName", ROLE_NAME_UPDATED);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isExpanded").value(true));

        verify(roleService, times(1)).updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null);
    }

    @Test
    @DisplayName("error04: roleNameが空文字のとき400を返し、Serviceを呼ばない")
    void updateRole_error01() throws Exception {
        // Arrange
        String requestBody = toJsonBody("roleName", "", "isExpanded", true);

        // Act
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).updateRole(any(), any(), any());
    }

    @Test
    @DisplayName("error05: roleNameが未指定のとき400を返し、Serviceを呼ばない")
    void updateRole_error02() throws Exception {
        // Arrange
        String requestBody = toJsonBody("isExpanded", true);

        // Act
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).updateRole(any(), any(), any());
    }

    @Test
    @DisplayName("error06: 存在しないIDを指定したとき404を返す")
    void updateRole_error03() throws Exception {
        // Arrange
        when(roleService.updateRole(eq(NON_EXISTING_ROLE_ID), any(), any()))
                .thenThrow(new NoSuchElementException("Role not found: " + NON_EXISTING_ROLE_ID));
        String requestBody = toJsonBody("roleName", ROLE_NAME_UPDATED, "isExpanded", true);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + NON_EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("error07: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void updateRole_error04() throws Exception {
        // Act
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(roleService, never()).updateRole(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/roles/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal08: 存在するIDの削除リクエストで204を返す")
    void deleteRole_normal01() throws Exception {
        // Arrange
        doNothing().when(roleService).deleteRole(EXISTING_ROLE_ID);

        // Act & Assert
        mockMvc.perform(delete(API_PATH + "/" + EXISTING_ROLE_ID))
                .andExpect(status().isNoContent());

        verify(roleService, times(1)).deleteRole(EXISTING_ROLE_ID);
    }

    @Test
    @DisplayName("error08: 存在しないIDの削除リクエストで404を返す")
    void deleteRole_error01() throws Exception {
        // Arrange
        doThrow(new NoSuchElementException("Role not found: " + NON_EXISTING_ROLE_ID))
                .when(roleService).deleteRole(NON_EXISTING_ROLE_ID);

        // Act & Assert
        mockMvc.perform(delete(API_PATH + "/" + NON_EXISTING_ROLE_ID))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private String toJsonBody(Object... keyValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }
}
