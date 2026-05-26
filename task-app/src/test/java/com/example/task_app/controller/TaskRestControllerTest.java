package com.example.task_app.controller;

import com.example.task_app.dto.TaskDto;
import com.example.task_app.response.TaskResponse;
import com.example.task_app.service.TaskService;
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

@WebMvcTest(TaskRestController.class)
class TaskRestControllerTest {
    private static final String API_PATH = "/api/tasks";
    private static final int ROLE_ID = 1;
    private static final int TASK_ID = 100;
    private static final String TITLE_TEMP = "Test Task";
    private static final String TITLE_PERMANENT = "Permanent Task";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    @DisplayName("normal01: isPermanent=falseの登録で200を返し、Serviceへ正しいDTOを渡す")
    void createTask_normal01() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "roleId", ROLE_ID,
                "title", TITLE_TEMP,
                "isPermanent", false
        );
        doNothing().when(taskService).createTask(any(TaskDto.class));
        ArgumentCaptor<TaskDto> taskDtoCaptor = ArgumentCaptor.forClass(TaskDto.class);

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Assert
        verify(taskService, times(1)).createTask(taskDtoCaptor.capture());
        TaskDto captured = taskDtoCaptor.getValue();
        assertNull(captured.getTaskId());
        assertEquals(ROLE_ID, captured.getRoleId());
        assertEquals(TITLE_TEMP, captured.getTitle());
        assertFalse(captured.isPermanent());
    }

    @Test
    @DisplayName("normal02: taskId付きかつisPermanent=trueの登録で201を返し、Serviceへ正しいDTOを渡す")
    void createTask_normal02() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "taskId", TASK_ID,
                "roleId", ROLE_ID,
                "title", TITLE_PERMANENT,
                "isPermanent", true
        );
        doNothing().when(taskService).createTask(any(TaskDto.class));
        ArgumentCaptor<TaskDto> taskDtoCaptor = ArgumentCaptor.forClass(TaskDto.class);

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Assert
        verify(taskService, times(1)).createTask(taskDtoCaptor.capture());
        TaskDto captured = taskDtoCaptor.getValue();
        assertEquals(TASK_ID, captured.getTaskId());
        assertEquals(ROLE_ID, captured.getRoleId());
        assertEquals(TITLE_PERMANENT, captured.getTitle());
        assertTrue(captured.isPermanent());
    }

    @Test
    @DisplayName("error01: titleが空文字のとき400を返し、Serviceを呼ばない")
    void createTask_error01() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "roleId", ROLE_ID,
                "title", "",
                "isPermanent", false
        );

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("error02: titleが未指定のとき400を返し、Serviceを呼ばない")
    void createTask_error02() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "roleId", ROLE_ID,
                "isPermanent", false
        );

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("error03: roleIdが未指定のとき400を返し、Serviceを呼ばない")
    void createTask_error03() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "title", TITLE_TEMP,
                "isPermanent", false
        );

        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("error04: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void createTask_error04() throws Exception {
        // Act
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("error05: Content-Type未指定のとき415を返し、Serviceを呼ばない")
    void createTask_error05() throws Exception {
        // Arrange
        String requestBody = toJsonBody(
                "roleId", ROLE_ID,
                "title", TITLE_TEMP,
                "isPermanent", false
        );

        // Act
        mockMvc.perform(post(API_PATH)
                        .content(requestBody))
                .andExpect(status().isUnsupportedMediaType());

        // Assert
        verify(taskService, never()).createTask(any(TaskDto.class));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/tasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal06: タスクが存在する場合、200とタスク一覧を返す")
    void getTasks_normal01() throws Exception {
        // Arrange
        TaskResponse task = new TaskResponse(TASK_ID, ROLE_ID, TITLE_TEMP, false);
        when(taskService.getTasks()).thenReturn(List.of(task));

        // Act & Assert
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].taskId").value(TASK_ID))
                .andExpect(jsonPath("$[0].roleId").value(ROLE_ID))
                .andExpect(jsonPath("$[0].title").value(TITLE_TEMP))
                .andExpect(jsonPath("$[0].isPermanent").value(false));

        verify(taskService, times(1)).getTasks();
    }

    @Test
    @DisplayName("normal07: タスクが0件の場合、200と空配列を返す")
    void getTasks_normal02() throws Exception {
        // Arrange
        when(taskService.getTasks()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(taskService, times(1)).getTasks();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/tasks/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal08: 正常な更新リクエストで200と更新済みタスクを返す")
    void updateTask_normal01() throws Exception {
        // Arrange
        TaskResponse updated = new TaskResponse(TASK_ID, ROLE_ID, TITLE_PERMANENT, true);
        when(taskService.updateTask(TASK_ID, TITLE_PERMANENT, true)).thenReturn(updated);
        String requestBody = toJsonBody("title", TITLE_PERMANENT, "isPermanent", true);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.title").value(TITLE_PERMANENT))
                .andExpect(jsonPath("$.isPermanent").value(true));

        verify(taskService, times(1)).updateTask(TASK_ID, TITLE_PERMANENT, true);
    }

    @Test
    @DisplayName("normal09: isPermanent=falseへの更新で200と更新済みタスクを返す")
    void updateTask_normal02() throws Exception {
        // Arrange
        TaskResponse updated = new TaskResponse(TASK_ID, ROLE_ID, TITLE_TEMP, false);
        when(taskService.updateTask(TASK_ID, TITLE_TEMP, false)).thenReturn(updated);
        String requestBody = toJsonBody("title", TITLE_TEMP, "isPermanent", false);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPermanent").value(false));

        verify(taskService, times(1)).updateTask(TASK_ID, TITLE_TEMP, false);
    }

    @Test
    @DisplayName("error06: titleが空文字のとき400を返し、Serviceを呼ばない")
    void updateTask_error01() throws Exception {
        // Arrange
        String requestBody = toJsonBody("title", "", "isPermanent", false);

        // Act
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error07: titleが未指定のとき400を返し、Serviceを呼ばない")
    void updateTask_error02() throws Exception {
        // Arrange
        String requestBody = toJsonBody("isPermanent", true);

        // Act
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error08: isPermanentが未指定のとき400を返し、Serviceを呼ばない")
    void updateTask_error03() throws Exception {
        // Arrange
        String requestBody = toJsonBody("title", TITLE_TEMP);

        // Act
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error09: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void updateTask_error04() throws Exception {
        // Act
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Assert
        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error10: 存在しないIDを指定したとき404を返す")
    void updateTask_error05() throws Exception {
        // Arrange
        int nonExistingTaskId = 9999;
        when(taskService.updateTask(eq(nonExistingTaskId), any(), any()))
                .thenThrow(new NoSuchElementException("Task not found: " + nonExistingTaskId));
        String requestBody = toJsonBody("title", TITLE_TEMP, "isPermanent", false);

        // Act & Assert
        mockMvc.perform(put(API_PATH + "/" + nonExistingTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/tasks/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal10: 存在するIDの削除リクエストで204を返す")
    void deleteTask_normal01() throws Exception {
        // Arrange
        doNothing().when(taskService).deleteTask(TASK_ID);

        // Act & Assert
        mockMvc.perform(delete(API_PATH + "/" + TASK_ID))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(TASK_ID);
    }

    @Test
    @DisplayName("error11: 存在しないIDの削除リクエストで404を返す")
    void deleteTask_error01() throws Exception {
        // Arrange
        int nonExistingTaskId = 9999;
        doThrow(new NoSuchElementException("Task not found: " + nonExistingTaskId))
                .when(taskService).deleteTask(nonExistingTaskId);

        // Act & Assert
        mockMvc.perform(delete(API_PATH + "/" + nonExistingTaskId))
                .andExpect(status().isNotFound());
    }

    private String toJsonBody(Object... keyValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }
}
