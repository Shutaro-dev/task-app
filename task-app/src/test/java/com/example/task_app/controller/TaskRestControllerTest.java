package com.example.task_app.controller;

import com.example.task_app.dto.TaskDto;
import com.example.task_app.service.TaskService;
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

/**
 * TaskRestControllerの単体テスト
 * 
 * このテストでは、TaskServiceをモック化し、
 * コントローラーの挙動のみを検証します。
 */
@WebMvcTest(TaskRestController.class)
class TaskRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * TaskServiceのモック
     * 実際のサービス層は呼び出さず、モックで置き換えます
     */
    @MockitoBean
    private TaskService taskService;

    @Test
    @DisplayName("正常系: タスク作成APIが成功し、200 OKを返す")
    void createTask_Success() throws Exception {
        // Arrange: リクエストボディの準備
        String requestBody = """
            {
                "roleId": 1,
                "title": "Test Task",
                "isPermanent": false
            }
            """;

        // サービスのモック設定（何も返さない void メソッド）
        doNothing().when(taskService).createTask(any(TaskDto.class));

        // Act & Assert: POSTリクエストを実行し、結果を検証
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Verify: サービスメソッドが1回呼び出されたことを検証
        verify(taskService, times(1)).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("正常系: 永続タスク（isPermanent=true）の作成が成功する")
    void createTask_PermanentTask_Success() throws Exception {
        // Arrange: 永続タスクのリクエストボディ
        String requestBody = """
            {
                "roleId": 1,
                "title": "Permanent Task",
                "isPermanent": true
            }
            """;

        doNothing().when(taskService).createTask(any(TaskDto.class));

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(taskService, times(1)).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("正常系: taskIdを指定してタスク作成が成功する")
    void createTask_WithTaskId_Success() throws Exception {
        // Arrange: taskIdを含むリクエストボディ
        String requestBody = """
            {
                "taskId": 100,
                "roleId": 1,
                "title": "Task with ID",
                "isPermanent": false
            }
            """;

        doNothing().when(taskService).createTask(any(TaskDto.class));

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(taskService, times(1)).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("異常系: titleが空の場合、400 Bad Requestを返す")
    void createTask_EmptyTitle_BadRequest() throws Exception {
        // Arrange: titleが空のリクエスト
        String requestBody = """
            {
                "roleId": 1,
                "title": "",
                "isPermanent": false
            }
            """;

        // Act & Assert: バリデーションエラーで400を期待
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Verify: バリデーションエラーの場合、サービスは呼び出されない
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("異常系: titleがnullの場合、400 Bad Requestを返す")
    void createTask_NullTitle_BadRequest() throws Exception {
        // Arrange: titleがないリクエスト
        String requestBody = """
            {
                "roleId": 1,
                "isPermanent": false
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("異常系: roleIdがnullの場合、400 Bad Requestを返す")
    void createTask_NullRoleId_BadRequest() throws Exception {
        // Arrange: roleIdがないリクエスト
        String requestBody = """
            {
                "title": "Test Task",
                "isPermanent": false
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("異常系: リクエストボディがない場合、400 Bad Requestを返す")
    void createTask_NoRequestBody_BadRequest() throws Exception {
        // Act & Assert: リクエストボディなしで送信
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("異常系: Content-Typeがない場合、415 Unsupported Media Typeを返す")
    void createTask_NoContentType_UnsupportedMediaType() throws Exception {
        // Arrange
        String requestBody = """
            {
                "roleId": 1,
                "title": "Test Task",
                "isPermanent": false
            }
            """;

        // Act & Assert: Content-Typeなしで送信
        mockMvc.perform(post("/api/tasks")
                        .content(requestBody))
                .andExpect(status().isUnsupportedMediaType());

        verify(taskService, never()).createTask(any(TaskDto.class));
    }
}
