// src/services/roleService.ts
// import axios from "axios" // 一時停止: ロールはローカルストレージで管理

export interface RoleData {
    roleId?: number
    roleName: string
}

// export async function createRole(roleData: RoleData): Promise<void> {
//     await axios.post("http://localhost:8080/api/roles", roleData)
// }

// export async function fetchRoles(): Promise<RoleData[]> {
//     const response = await axios.get<RoleData[]>("http://localhost:8080/api/roles")
//     return response.data
// }
