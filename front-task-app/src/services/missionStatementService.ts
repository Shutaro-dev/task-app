import axios from 'axios'
import { API_ORIGIN } from './apiBase'
import { isLocalMode } from './persistenceMode'
import * as localStore from './localStore'

const BASE = `${API_ORIGIN}/api/mission_statement`

export async function fetchMissionStatement(): Promise<string> {
  if (isLocalMode) return localStore.getMissionStatement()
  const { data } = await axios.get<{ missionStatement: string }>(BASE)
  return data.missionStatement
}

export async function updateMissionStatement(text: string): Promise<void> {
  if (isLocalMode) return localStore.updateMissionStatement(text)
  await axios.put(BASE, { missionStatement: text })
}
