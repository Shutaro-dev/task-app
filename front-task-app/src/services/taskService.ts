import axios from 'axios'
import { API_ORIGIN } from './apiBase'

const BASE = `${API_ORIGIN}/api/tasks`

export async function updateTaskTitle(id: string, title: string): Promise<void> {
  await axios.put(`${BASE}/${id}`, { title })
}

export async function toggleTaskPermanent(id: string, isPermanent: boolean): Promise<void> {
  await axios.put(`${BASE}/${id}`, { isPermanent })
}

export async function reorderTasks(items: { id: number; sortOrder: number }[]): Promise<void> {
  await axios.put(`${BASE}/reorder`, items)
}
