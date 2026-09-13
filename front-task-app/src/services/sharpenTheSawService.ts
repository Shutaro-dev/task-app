import axios from 'axios'
import type { SharpenTheSawArea } from '../types'
import { API_ORIGIN } from './apiBase'
import { isLocalMode } from './persistenceMode'
import * as localStore from './localStore'

const BASE = `${API_ORIGIN}/api/sharpen_the_saw_areas`

// サーバーのタスク形式は既にフロントのTask型 { id, title, roleId, isPermanent } と同一なので変換不要
export async function fetchSharpenTheSawAreas(): Promise<SharpenTheSawArea[]> {
  if (isLocalMode) return localStore.getSharpenTheSawAreas()
  const { data } = await axios.get<SharpenTheSawArea[]>(BASE)
  return data
}

export async function updateSharpenTheSawAreas(areas: SharpenTheSawArea[]): Promise<SharpenTheSawArea[]> {
  if (isLocalMode) return localStore.updateSharpenTheSawAreas(areas)
  const payload = areas.map(area => ({
    id: area.id,
    tasks: area.tasks.map(task => ({ id: task.id, title: task.title })),
  }))
  const { data } = await axios.put<SharpenTheSawArea[]>(BASE, payload)
  return data
}
