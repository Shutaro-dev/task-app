import axios from 'axios'
import type { DayNotes, ScheduledTask, Task, WeekData } from '../types'
import { API_ORIGIN } from './apiBase'

const BASE = `${API_ORIGIN}/api/week_data`

interface ScheduledTaskResponse {
  id: number
  taskId: number
  day: number
  startTime: string
  duration: number
  title: string
  roleId: number
  completed: boolean
}

interface TemporaryTaskResponse {
  id: number
  title: string
  roleId: number
  isPermanent: boolean
}

interface DayNoteResponse {
  day: number
  notes: string
  sleepStart: string | null
  sleepEnd: string | null
}

interface WeekDataResponse {
  weekStart: string
  weeklyNotes: string
  scheduledTasks: ScheduledTaskResponse[]
  dayNotes: DayNoteResponse[]
  temporaryTasks: TemporaryTaskResponse[]
}

function mapScheduledTask(json: ScheduledTaskResponse): ScheduledTask {
  return {
    id: String(json.id),
    taskId: String(json.taskId),
    day: json.day,
    startTime: json.startTime,
    duration: json.duration,
    title: json.title,
    roleId: String(json.roleId),
    completed: json.completed,
  }
}

function mapTemporaryTask(json: TemporaryTaskResponse): Task {
  return { id: String(json.id), title: json.title, roleId: String(json.roleId), isPermanent: json.isPermanent }
}

function mapDayNotes(json: DayNoteResponse): DayNotes {
  return {
    day: json.day,
    notes: json.notes,
    sleepStart: json.sleepStart ?? undefined,
    sleepEnd: json.sleepEnd ?? undefined,
  }
}

function mapWeekData(json: WeekDataResponse): WeekData {
  return {
    weekStart: new Date(json.weekStart),
    weeklyNotes: json.weeklyNotes,
    scheduledTasks: json.scheduledTasks.map(mapScheduledTask),
    dayNotes: json.dayNotes.map(mapDayNotes),
    temporaryTasks: json.temporaryTasks.map(mapTemporaryTask),
  }
}

export async function fetchWeekData(weekStart: string): Promise<WeekData> {
  const { data } = await axios.get<WeekDataResponse>(`${BASE}/${weekStart}`)
  return mapWeekData(data)
}

export async function updateWeeklyNotes(weekStart: string, notes: string): Promise<void> {
  await axios.put(`${BASE}/${weekStart}`, { weeklyNotes: notes })
}

export interface CreateScheduledTaskInput {
  taskId: string
  day: number
  startTime: string
  duration: number
  title: string
  roleId: string
}

export async function createScheduledTask(
  weekStart: string,
  input: CreateScheduledTaskInput
): Promise<ScheduledTask> {
  const { data } = await axios.post<ScheduledTaskResponse>(`${BASE}/${weekStart}/scheduled_tasks`, input)
  return mapScheduledTask(data)
}

export async function updateScheduledTask(
  id: string,
  updates: Partial<Pick<ScheduledTask, 'day' | 'startTime' | 'duration' | 'title' | 'completed'>>
): Promise<void> {
  await axios.put(`${API_ORIGIN}/api/scheduled_tasks/${id}`, updates)
}

export async function deleteScheduledTask(id: string): Promise<void> {
  await axios.delete(`${API_ORIGIN}/api/scheduled_tasks/${id}`)
}

export async function upsertDayNotes(
  weekStart: string,
  day: number,
  updates: Partial<Pick<DayNotes, 'notes' | 'sleepStart' | 'sleepEnd'>>
): Promise<void> {
  await axios.put(`${BASE}/${weekStart}/day_notes/${day}`, updates)
}
