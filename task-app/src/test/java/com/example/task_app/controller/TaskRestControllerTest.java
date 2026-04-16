package com.example.task_app.controller;

import com.example.task_app.dto.TaskDto;
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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(status().isOk());

        // Assert
        verify(taskService, times(1)).createTask(taskDtoCaptor.capture());
        TaskDto captured = taskDtoCaptor.getValue();
        assertNull(captured.getTaskId());
        assertEquals(ROLE_ID, captured.getRoleId());
        assertEquals(TITLE_TEMP, captured.getTitle());
        assertFalse(captured.isPermanent());
    }

    @Test
    @DisplayName("normal02: taskId付きかつisPermanent=trueの登録で200を返し、Serviceへ正しいDTOを渡す")
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
                .andExpect(status().isOk());

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

    private String toJsonBody(Object... keyValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }
}
