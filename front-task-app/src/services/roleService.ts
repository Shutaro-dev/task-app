import axios from 'axios'

const BASE = 'http://localhost:8080/api/roles'

export async function updateRoleColor(id: string, color: string): Promise<void> {
  await axios.put(`${BASE}/${id}`, { color })
}

export async function reorderRoles(items: { id: number; sortOrder: number }[]): Promise<void> {
  await axios.put(`${BASE}/reorder`, items)
}
