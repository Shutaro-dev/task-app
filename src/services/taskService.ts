// src/api/tasks.ts
import axios from "axios"
import type { Task } from "../types/task"

export async function fetchTasks(): Promise<Task[]> {
    const response = await axios.get<Task[]>("http://localhost:8080/api/tasks")
    return response.data
}

export async function deleteScheduledTask(id: number): Promise<void> {
    await axios.delete(`http://localhost:8080/api/scheduled-tasks/${id}`)
}
