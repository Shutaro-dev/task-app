import axios from 'axios'
import { API_ORIGIN } from './apiBase'

const BASE = `${API_ORIGIN}/api/mission_statement`

export async function fetchMissionStatement(): Promise<string> {
  const { data } = await axios.get<{ missionStatement: string }>(BASE)
  return data.missionStatement
}

export async function updateMissionStatement(text: string): Promise<void> {
  await axios.put(BASE, { missionStatement: text })
}
