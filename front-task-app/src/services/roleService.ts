import axios from 'axios'
import { API_ORIGIN } from './apiBase'

const BASE = `${API_ORIGIN}/api/roles`

export async function updateRoleColor(id: string, color: string): Promise<void> {
  await axios.put(`${BASE}/${id}`, { color })
}

export async function reorderRoles(items: { id: number; sortOrder: number }[]): Promise<void> {
  await axios.put(`${BASE}/reorder`, items)
}
