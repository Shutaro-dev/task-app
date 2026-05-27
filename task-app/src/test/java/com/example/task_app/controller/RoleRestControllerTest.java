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
    private static final String API_PATH           = "/api/roles";
    private static final String ROLE_NAME_TEST     = "TestRole";
    private static final String ROLE_NAME_UPDATED  = "UpdatedRole";
    private static final String COLOR_BLUE         = "#4a90d9";
    private static final int    EXISTING_ROLE_ID   = 1;
    private static final int    NON_EXISTING_ROLE_ID = 9999;
    private static final int    TASK_ID            = 10;
    private static final String TASK_TITLE         = "Task 1";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private RoleService roleService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/roles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: roleNameのみの登録リクエストで201を返し、ServiceにDTOを渡す")
    void createRole_normal01() throws Exception {
        doNothing().when(roleService).createRole(any(RoleDto.class));
        ArgumentCaptor<RoleDto> captor = ArgumentCaptor.forClass(RoleDto.class);

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_TEST)))
                .andExpect(status().isCreated());

        verify(roleService).createRole(captor.capture());
        assertNull(captor.getValue().getRoleId());
        assertEquals(ROLE_NAME_TEST, captor.getValue().getRoleName());
    }

    @Test
    @DisplayName("normal02: roleId付き登録リクエストで201を返し、ServiceにDTOを渡す")
    void createRole_normal02() throws Exception {
        doNothing().when(roleService).createRole(any(RoleDto.class));
        ArgumentCaptor<RoleDto> captor = ArgumentCaptor.forClass(RoleDto.class);

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", EXISTING_ROLE_ID, "roleName", ROLE_NAME_UPDATED)))
                .andExpect(status().isCreated());

        verify(roleService).createRole(captor.capture());
        assertEquals(EXISTING_ROLE_ID, captor.getValue().getRoleId());
        assertEquals(ROLE_NAME_UPDATED, captor.getValue().getRoleName());
    }

    @Test
    @DisplayName("normal03: colorを含む登録リクエストでDTOのcolorが正しく渡される")
    void createRole_normal03() throws Exception {
        doNothing().when(roleService).createRole(any(RoleDto.class));
        ArgumentCaptor<RoleDto> captor = ArgumentCaptor.forClass(RoleDto.class);

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_TEST, "color", COLOR_BLUE)))
                .andExpect(status().isCreated());

        verify(roleService).createRole(captor.capture());
        assertEquals(COLOR_BLUE, captor.getValue().getColor());
    }

    @Test
    @DisplayName("error01: roleNameが空文字のとき400を返し、Serviceを呼ばない")
    void createRole_error01() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", "")))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).createRole(any());
    }

    @Test
    @DisplayName("error02: roleNameが未指定のとき400を返し、Serviceを呼ばない")
    void createRole_error02() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", EXISTING_ROLE_ID)))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).createRole(any());
    }

    @Test
    @DisplayName("error03: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void createRole_error03() throws Exception {
        mockMvc.perform(post(API_PATH).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).createRole(any());
    }

    @Test
    @DisplayName("error04: 存在しないroleIdを指定したとき404を返す")
    void createRole_error04() throws Exception {
        doThrow(new NoSuchElementException("Role not found: " + NON_EXISTING_ROLE_ID))
                .when(roleService).createRole(any(RoleDto.class));

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", NON_EXISTING_ROLE_ID, "roleName", ROLE_NAME_TEST)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("error05: Content-Type未指定のとき415を返し、Serviceを呼ばない")
    void createRole_error05() throws Exception {
        mockMvc.perform(post(API_PATH).content(toJsonBody("roleName", ROLE_NAME_TEST)))
                .andExpect(status().isUnsupportedMediaType());

        verify(roleService, never()).createRole(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/roles
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal04: ロールが存在する場合、200とtasks付きのロール一覧を返す")
    void getAllRoles_normal01() throws Exception {
        TaskResponse task = new TaskResponse(TASK_ID, EXISTING_ROLE_ID, TASK_TITLE, false);
        RoleResponse role = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_TEST, true, COLOR_BLUE, List.of(task));
        when(roleService.getAllRoles()).thenReturn(List.of(role));

        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleId").value(EXISTING_ROLE_ID))
                .andExpect(jsonPath("$[0].roleName").value(ROLE_NAME_TEST))
                .andExpect(jsonPath("$[0].isExpanded").value(true))
                .andExpect(jsonPath("$[0].color").value(COLOR_BLUE))
                .andExpect(jsonPath("$[0].tasks[0].taskId").value(TASK_ID))
                .andExpect(jsonPath("$[0].tasks[0].title").value(TASK_TITLE));
    }

    @Test
    @DisplayName("normal05: ロールが0件の場合、200と空配列を返す")
    void getAllRoles_normal02() throws Exception {
        when(roleService.getAllRoles()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("normal06: isExpanded=falseのロールが正しくシリアライズされる")
    void getAllRoles_normal03() throws Exception {
        RoleResponse collapsedRole = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_TEST, false, null, Collections.emptyList());
        when(roleService.getAllRoles()).thenReturn(List.of(collapsedRole));

        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isExpanded").value(false))
                .andExpect(jsonPath("$[0].tasks").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/roles/reorder
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal07: 正常なreorderリクエストで200を返す")
    void reorderRoles_normal01() throws Exception {
        doNothing().when(roleService).reorderRoles(any());
        String body = "[{\"id\":2,\"sortOrder\":0},{\"id\":1,\"sortOrder\":1}]";

        mockMvc.perform(put(API_PATH + "/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(roleService, times(1)).reorderRoles(any());
    }

    @Test
    @DisplayName("normal08: 空配列のreorderリクエストでも200を返す")
    void reorderRoles_normal02() throws Exception {
        doNothing().when(roleService).reorderRoles(any());

        mockMvc.perform(put(API_PATH + "/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/roles/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal09: 正常な更新リクエストで200と更新済みロールを返す")
    void updateRole_normal01() throws Exception {
        RoleResponse updated = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false, null, Collections.emptyList());
        when(roleService.updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false, null)).thenReturn(updated);

        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_UPDATED, "isExpanded", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(EXISTING_ROLE_ID))
                .andExpect(jsonPath("$.roleName").value(ROLE_NAME_UPDATED))
                .andExpect(jsonPath("$.isExpanded").value(false));

        verify(roleService).updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, false, null);
    }

    @Test
    @DisplayName("normal10: isExpandedを省略したリクエストで200を返し、nullをServiceに渡す")
    void updateRole_normal02() throws Exception {
        RoleResponse updated = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, true, null, Collections.emptyList());
        when(roleService.updateRole(eq(EXISTING_ROLE_ID), eq(ROLE_NAME_UPDATED), isNull(), isNull())).thenReturn(updated);

        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_UPDATED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isExpanded").value(true));

        verify(roleService).updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null, null);
    }

    @Test
    @DisplayName("normal11: colorを含む更新リクエストでcolorがServiceに渡される")
    void updateRole_normal03() throws Exception {
        RoleResponse updated = new RoleResponse(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, true, COLOR_BLUE, Collections.emptyList());
        when(roleService.updateRole(eq(EXISTING_ROLE_ID), eq(ROLE_NAME_UPDATED), isNull(), eq(COLOR_BLUE))).thenReturn(updated);

        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_UPDATED, "color", COLOR_BLUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value(COLOR_BLUE));

        verify(roleService).updateRole(EXISTING_ROLE_ID, ROLE_NAME_UPDATED, null, COLOR_BLUE);
    }

    @Test
    @DisplayName("error06: roleNameが空文字のとき400を返し、Serviceを呼ばない")
    void updateRole_error01() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", "", "isExpanded", true)))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).updateRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("error07: roleNameが未指定のとき400を返し、Serviceを呼ばない")
    void updateRole_error02() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("isExpanded", true)))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).updateRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("error08: 存在しないIDを指定したとき404を返す")
    void updateRole_error03() throws Exception {
        when(roleService.updateRole(eq(NON_EXISTING_ROLE_ID), any(), any(), any()))
                .thenThrow(new NoSuchElementException("Role not found: " + NON_EXISTING_ROLE_ID));

        mockMvc.perform(put(API_PATH + "/" + NON_EXISTING_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleName", ROLE_NAME_UPDATED, "isExpanded", true)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("error09: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void updateRole_error04() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).updateRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("error10: Content-Type未指定のとき415を返し、Serviceを呼ばない")
    void updateRole_error05() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + EXISTING_ROLE_ID)
                        .content(toJsonBody("roleName", ROLE_NAME_UPDATED, "isExpanded", true)))
                .andExpect(status().isUnsupportedMediaType());

        verify(roleService, never()).updateRole(any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/roles/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal12: 存在するIDの削除リクエストで204を返す")
    void deleteRole_normal01() throws Exception {
        doNothing().when(roleService).deleteRole(EXISTING_ROLE_ID);

        mockMvc.perform(delete(API_PATH + "/" + EXISTING_ROLE_ID))
                .andExpect(status().isNoContent());

        verify(roleService, times(1)).deleteRole(EXISTING_ROLE_ID);
    }

    @Test
    @DisplayName("error11: 存在しないIDの削除リクエストで404を返す")
    void deleteRole_error01() throws Exception {
        doThrow(new NoSuchElementException("Role not found: " + NON_EXISTING_ROLE_ID))
                .when(roleService).deleteRole(NON_EXISTING_ROLE_ID);

        mockMvc.perform(delete(API_PATH + "/" + NON_EXISTING_ROLE_ID))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private String toJsonBody(Object... keyValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) body.put((String) keyValues[i], keyValues[i + 1]);
        return objectMapper.writeValueAsString(body);
    }
}
