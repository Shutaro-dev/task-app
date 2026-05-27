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
    private static final String API_PATH       = "/api/tasks";
    private static final int    ROLE_ID        = 1;
    private static final int    TASK_ID        = 100;
    private static final String TITLE_TEMP     = "Test Task";
    private static final String TITLE_PERM     = "Permanent Task";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean  private TaskService taskService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/tasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal01: isPermanent=falseの登録で201を返し、Serviceへ正しいDTOを渡す")
    void createTask_normal01() throws Exception {
        doNothing().when(taskService).createTask(any(TaskDto.class));
        ArgumentCaptor<TaskDto> captor = ArgumentCaptor.forClass(TaskDto.class);

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", ROLE_ID, "title", TITLE_TEMP, "isPermanent", false)))
                .andExpect(status().isCreated());

        verify(taskService).createTask(captor.capture());
        assertNull(captor.getValue().getTaskId());
        assertEquals(ROLE_ID,    captor.getValue().getRoleId());
        assertEquals(TITLE_TEMP, captor.getValue().getTitle());
        assertFalse(captor.getValue().isPermanent());
    }

    @Test
    @DisplayName("normal02: taskId付きかつisPermanent=trueの登録で201を返し、Serviceへ正しいDTOを渡す")
    void createTask_normal02() throws Exception {
        doNothing().when(taskService).createTask(any(TaskDto.class));
        ArgumentCaptor<TaskDto> captor = ArgumentCaptor.forClass(TaskDto.class);

        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("taskId", TASK_ID, "roleId", ROLE_ID, "title", TITLE_PERM, "isPermanent", true)))
                .andExpect(status().isCreated());

        verify(taskService).createTask(captor.capture());
        assertEquals(TASK_ID,   captor.getValue().getTaskId());
        assertEquals(ROLE_ID,   captor.getValue().getRoleId());
        assertEquals(TITLE_PERM, captor.getValue().getTitle());
        assertTrue(captor.getValue().isPermanent());
    }

    @Test
    @DisplayName("error01: titleが空文字のとき400を返し、Serviceを呼ばない")
    void createTask_error01() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", ROLE_ID, "title", "", "isPermanent", false)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("error02: titleが未指定のとき400を返し、Serviceを呼ばない")
    void createTask_error02() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("roleId", ROLE_ID, "isPermanent", false)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("error03: roleIdが未指定のとき400を返し、Serviceを呼ばない")
    void createTask_error03() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", TITLE_TEMP, "isPermanent", false)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("error04: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void createTask_error04() throws Exception {
        mockMvc.perform(post(API_PATH).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any());
    }

    @Test
    @DisplayName("error05: Content-Type未指定のとき415を返し、Serviceを呼ばない")
    void createTask_error05() throws Exception {
        mockMvc.perform(post(API_PATH)
                        .content(toJsonBody("roleId", ROLE_ID, "title", TITLE_TEMP, "isPermanent", false)))
                .andExpect(status().isUnsupportedMediaType());

        verify(taskService, never()).createTask(any());
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/tasks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal03: タスクが存在する場合、200とタスク一覧を返す")
    void getTasks_normal01() throws Exception {
        TaskResponse task = new TaskResponse(TASK_ID, ROLE_ID, TITLE_TEMP, false);
        when(taskService.getTasks()).thenReturn(List.of(task));

        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(TASK_ID))
                .andExpect(jsonPath("$[0].roleId").value(ROLE_ID))
                .andExpect(jsonPath("$[0].title").value(TITLE_TEMP))
                .andExpect(jsonPath("$[0].isPermanent").value(false));

        verify(taskService, times(1)).getTasks();
    }

    @Test
    @DisplayName("normal04: タスクが0件の場合、200と空配列を返す")
    void getTasks_normal02() throws Exception {
        when(taskService.getTasks()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(API_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(taskService, times(1)).getTasks();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/tasks/reorder
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal05: 正常なreorderリクエストで200を返す")
    void reorderTasks_normal01() throws Exception {
        doNothing().when(taskService).reorderTasks(any());
        String body = "[{\"id\":2,\"sortOrder\":0},{\"id\":1,\"sortOrder\":1}]";

        mockMvc.perform(put(API_PATH + "/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(taskService, times(1)).reorderTasks(any());
    }

    @Test
    @DisplayName("normal06: 空配列のreorderリクエストでも200を返す")
    void reorderTasks_normal02() throws Exception {
        doNothing().when(taskService).reorderTasks(any());

        mockMvc.perform(put(API_PATH + "/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/tasks/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal07: 正常な更新リクエストで200と更新済みタスクを返す")
    void updateTask_normal01() throws Exception {
        TaskResponse updated = new TaskResponse(TASK_ID, ROLE_ID, TITLE_PERM, true);
        when(taskService.updateTask(TASK_ID, TITLE_PERM, true)).thenReturn(updated);

        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", TITLE_PERM, "isPermanent", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.title").value(TITLE_PERM))
                .andExpect(jsonPath("$.isPermanent").value(true));

        verify(taskService).updateTask(TASK_ID, TITLE_PERM, true);
    }

    @Test
    @DisplayName("normal08: isPermanent=falseへの更新で200と更新済みタスクを返す")
    void updateTask_normal02() throws Exception {
        TaskResponse updated = new TaskResponse(TASK_ID, ROLE_ID, TITLE_TEMP, false);
        when(taskService.updateTask(TASK_ID, TITLE_TEMP, false)).thenReturn(updated);

        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", TITLE_TEMP, "isPermanent", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPermanent").value(false));

        verify(taskService).updateTask(TASK_ID, TITLE_TEMP, false);
    }

    @Test
    @DisplayName("error06: titleが空文字のとき400を返し、Serviceを呼ばない")
    void updateTask_error01() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", "", "isPermanent", false)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error07: titleが未指定のとき400を返し、Serviceを呼ばない")
    void updateTask_error02() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("isPermanent", true)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error08: isPermanentが未指定のとき400を返し、Serviceを呼ばない")
    void updateTask_error03() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", TITLE_TEMP)))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error09: リクエストボディなしのとき400を返し、Serviceを呼ばない")
    void updateTask_error04() throws Exception {
        mockMvc.perform(put(API_PATH + "/" + TASK_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @DisplayName("error10: 存在しないIDを指定したとき404を返す")
    void updateTask_error05() throws Exception {
        int nonExistingTaskId = 9999;
        when(taskService.updateTask(eq(nonExistingTaskId), any(), any()))
                .thenThrow(new NoSuchElementException("Task not found: " + nonExistingTaskId));

        mockMvc.perform(put(API_PATH + "/" + nonExistingTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJsonBody("title", TITLE_TEMP, "isPermanent", false)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/tasks/{id}
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normal09: 存在するIDの削除リクエストで204を返す")
    void deleteTask_normal01() throws Exception {
        doNothing().when(taskService).deleteTask(TASK_ID);

        mockMvc.perform(delete(API_PATH + "/" + TASK_ID))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(TASK_ID);
    }

    @Test
    @DisplayName("error11: 存在しないIDの削除リクエストで404を返す")
    void deleteTask_error01() throws Exception {
        int nonExistingTaskId = 9999;
        doThrow(new NoSuchElementException("Task not found: " + nonExistingTaskId))
                .when(taskService).deleteTask(nonExistingTaskId);

        mockMvc.perform(delete(API_PATH + "/" + nonExistingTaskId))
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
