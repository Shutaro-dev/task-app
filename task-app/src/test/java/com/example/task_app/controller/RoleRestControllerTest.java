package com.example.task_app.controller;

import com.example.task_app.dto.RoleDto;
import com.example.task_app.form.RoleForm;
import com.example.task_app.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleRestController.class)
class RoleRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * RoleServiceのモック
     * 実際のサービス層は呼び出さず、モックで置き換えます
     */
    @MockitoBean
    private RoleService roleService;

    @Test
    @DisplayName("正常系: ロール作成APIが成功し、200 OKを返す")
    void createRole_Success() throws Exception {
        // Arrange: リクエストボディの準備
        RoleForm roleForm = new RoleForm();
        // RoleFormにはsetterがないため、JSONで直接送信
        String requestBody = """
            {
                "roleName": "TestRole"
            }
            """;

        // サービスのモック設定（何も返さない void メソッド）
        doNothing().when(roleService).createRole(any(RoleDto.class));

        // Act & Assert: POSTリクエストを実行し、結果を検証
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Verify: サービスメソッドが1回呼び出されたことを検証
        verify(roleService, times(1)).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("正常系: roleIdを指定してロール更新APIが成功し、200 OKを返す")
    void createRole_WithRoleId_Success() throws Exception {
        // Arrange: roleIdを含むリクエストボディ
        String requestBody = """
            {
                "roleId": 1,
                "roleName": "UpdatedRole"
            }
            """;

        doNothing().when(roleService).createRole(any(RoleDto.class));

        // Act & Assert
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(roleService, times(1)).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("異常系: roleNameが空の場合、400 Bad Requestを返す")
    void createRole_EmptyRoleName_BadRequest() throws Exception {
        // Arrange: roleNameが空のリクエスト
        String requestBody = """
            {
                "roleName": ""
            }
            """;

        // Act & Assert: バリデーションエラーで400を期待
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Verify: バリデーションエラーの場合、サービスは呼び出されない
        verify(roleService, never()).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("異常系: roleNameがnullの場合、400 Bad Requestを返す")
    void createRole_NullRoleName_BadRequest() throws Exception {
        // Arrange: roleNameがないリクエスト
        String requestBody = """
            {
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).createRole(any(RoleDto.class));
    }

    @Test
    @DisplayName("異常系: リクエストボディがない場合、400 Bad Requestを返す")
    void createRole_NoRequestBody_BadRequest() throws Exception {
        // Act & Assert: リクエストボディなしで送信
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(roleService, never()).createRole(any(RoleDto.class));
    }
}
