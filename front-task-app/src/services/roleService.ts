import axios from 'axios'
import type { Role, Task } from '../types'
import { API_ORIGIN } from './apiBase'

const BASE = `${API_ORIGIN}/api/roles`

interface RoleTaskResponse {
  taskId: number
  roleId: number
  title: string
  isPermanent: boolean
}

interface RoleResponse {
  roleId: number
  roleName: string
  isExpanded: boolean
  color: string | null
  tasks: RoleTaskResponse[]
}

function mapTask(json: RoleTaskResponse): Task {
  return { id: String(json.taskId), title: json.title, roleId: String(json.roleId), isPermanent: json.isPermanent }
}

function mapRole(json: RoleResponse): Role {
  return {
    id: String(json.roleId),
    name: json.roleName,
    isExpanded: json.isExpanded,
    color: json.color || undefined,
    tasks: json.tasks.map(mapTask),
  }
}

export async function fetchRoles(): Promise<Role[]> {
  const { data } = await axios.get<RoleResponse[]>(BASE)
  return data.map(mapRole)
}

export async function createRole(name: string, color?: string, isExpanded?: boolean): Promise<Role> {
  const { data } = await axios.post<RoleResponse>(BASE, { roleName: name, color, isExpanded })
  return mapRole(data)
}

export async function deleteRole(id: string): Promise<void> {
  await axios.delete(`${BASE}/${id}`)
}

export async function updateRoleName(id: string, name: string): Promise<void> {
  await axios.put(`${BASE}/${id}`, { roleName: name })
}

export async function updateRoleColor(id: string, color: string): Promise<void> {
  await axios.put(`${BASE}/${id}`, { color })
}

export async function updateRoleExpanded(id: string, isExpanded: boolean): Promise<void> {
  await axios.put(`${BASE}/${id}`, { isExpanded })
}

export async function reorderRoles(items: { id: string; sortOrder: number }[]): Promise<void> {
  await axios.put(`${BASE}/reorder`, items)
}
