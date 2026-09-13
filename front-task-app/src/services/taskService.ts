import axios from 'axios'
import type { Task } from '../types'
import { API_ORIGIN } from './apiBase'
import { isLocalMode } from './persistenceMode'
import * as localStore from './localStore'

const BASE = `${API_ORIGIN}/api/tasks`

interface TaskResponse {
  taskId: number
  roleId: number
  title: string
  isPermanent: boolean
}

function mapTask(json: TaskResponse): Task {
  return { id: String(json.taskId), title: json.title, roleId: String(json.roleId), isPermanent: json.isPermanent }
}

export interface CreateTaskInput {
  roleId: string
  title: string
  isPermanent: boolean
  // isPermanent: false のときは必須(サーバー側でその週のweek_dataに紐づけるため)
  weekStart?: string
}

export async function createTask(input: CreateTaskInput): Promise<Task> {
  if (isLocalMode) return localStore.createTask(input)
  const { data } = await axios.post<TaskResponse>(BASE, {
    roleId: input.roleId,
    title: input.title,
    isPermanent: input.isPermanent,
    weekStart: input.weekStart,
  })
  return mapTask(data)
}

export async function deleteTask(id: string): Promise<void> {
  if (isLocalMode) return localStore.deleteTask(id)
  await axios.delete(`${BASE}/${id}`)
}

// title/isPermanentはサーバー側で常に両方必須のため、変更しない側も呼び出し元から渡す
export async function updateTask(
  id: string,
  payload: { title: string; isPermanent: boolean; weekStart?: string }
): Promise<Task> {
  if (isLocalMode) return localStore.updateTask(id, payload)
  const { data } = await axios.put<TaskResponse>(`${BASE}/${id}`, payload)
  return mapTask(data)
}

export async function reorderTasks(items: { id: string; sortOrder: number }[]): Promise<void> {
  if (isLocalMode) return localStore.reorderTasks(items)
  await axios.put(`${BASE}/reorder`, items)
}
