// persistenceMode.ts が 'local' の間、各serviceがAPIの代わりに読み書きする
// localStorageのみで完結したデータストア。DBのテーブル構成・カスケード削除挙動
// (roles/tasksのFK on_delete: cascade、tasksのweek_data_id on_delete: nullify)を
// できるだけそのまま踏襲し、'api'モードに戻したときの見た目・挙動の差異が出ないようにする。
import type { DayNotes, Role, ScheduledTask, SharpenTheSawArea, Task, WeekData } from '../types'

const STORAGE_KEY = 'weekly-compass:local-db:v1'

interface StoredWeekData {
  weeklyNotes: string
  scheduledTasks: ScheduledTask[]
  dayNotes: DayNotes[]
  temporaryTasks: Task[]
}

interface LocalDB {
  nextId: number
  roles: Role[] // tasksは永続タスクのみ保持(RolesController#role_json相当)
  sharpenTheSawAreas: SharpenTheSawArea[]
  missionStatement: string
  weekData: Record<string, StoredWeekData>
}

// サーバー側db/seeds.rbの初期データと同一(id/name/icon/表示順)
const DEFAULT_AREAS: { id: string; name: string; icon: string }[] = [
  { id: 'physical', name: 'Physical', icon: '💪' },
  { id: 'mental', name: 'Intellectual', icon: '🧠' },
  { id: 'social-emotional', name: 'Social/Emotional', icon: '❤️' },
  { id: 'spiritual', name: 'Spiritual', icon: '🙏' },
]

function defaultDb(): LocalDB {
  return {
    nextId: 1,
    roles: [],
    sharpenTheSawAreas: DEFAULT_AREAS.map(area => ({ ...area, tasks: [] })),
    missionStatement: '',
    weekData: {},
  }
}

function blankStoredWeek(): StoredWeekData {
  return {
    weeklyNotes: '',
    scheduledTasks: [],
    dayNotes: Array.from({ length: 7 }, (_, i) => ({ day: i, notes: '' })),
    temporaryTasks: [],
  }
}

let cache: LocalDB | null = null

function load(): LocalDB {
  if (cache) return cache
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      cache = JSON.parse(raw) as LocalDB
      return cache
    }
  } catch (error) {
    console.warn('Failed to read local persistence store, starting fresh:', error)
  }
  cache = defaultDb()
  return cache
}

function persist(db: LocalDB): void {
  cache = db
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
  } catch (error) {
    console.warn('Failed to write local persistence store:', error)
  }
}

function nextId(db: LocalDB): string {
  const id = String(db.nextId)
  db.nextId += 1
  return id
}

function getOrCreateWeek(db: LocalDB, weekStart: string): StoredWeekData {
  const existing = db.weekData[weekStart]
  if (existing) return existing
  const created = blankStoredWeek()
  db.weekData[weekStart] = created
  return created
}

// --- roles ---

export function getRoles(): Role[] {
  return load().roles
}

export function createRole(name: string, color?: string, isExpanded = false): Role {
  const db = load()
  const role: Role = { id: nextId(db), name, tasks: [], isExpanded, color }
  db.roles = [...db.roles, role]
  persist(db)
  return role
}

export function deleteRole(id: string): void {
  const db = load()
  db.roles = db.roles.filter(r => r.id !== id)
  Object.values(db.weekData).forEach(week => {
    week.scheduledTasks = week.scheduledTasks.filter(t => t.roleId !== id)
    week.temporaryTasks = week.temporaryTasks.filter(t => t.roleId !== id)
  })
  persist(db)
}

export function updateRole(id: string, updates: Partial<Pick<Role, 'name' | 'color' | 'isExpanded'>>): void {
  const db = load()
  db.roles = db.roles.map(r => (r.id === id ? { ...r, ...updates } : r))
  persist(db)
}

export function reorderRoles(items: { id: string; sortOrder: number }[]): void {
  const db = load()
  const order = new Map(items.map(i => [i.id, i.sortOrder]))
  // 対象外のロールは同一キー(0)を割り当てることで、安定ソートにより元の相対順序を保つ
  db.roles = [...db.roles].sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0))
  persist(db)
}

// --- tasks ---

interface CreateTaskInput {
  roleId: string
  title: string
  isPermanent: boolean
  weekStart?: string
}

export function createTask(input: CreateTaskInput): Task {
  const db = load()
  const task: Task = { id: nextId(db), title: input.title, roleId: input.roleId, isPermanent: input.isPermanent }
  if (input.isPermanent) {
    db.roles = db.roles.map(r => (r.id === input.roleId ? { ...r, tasks: [...r.tasks, task] } : r))
  } else {
    const week = getOrCreateWeek(db, input.weekStart!)
    week.temporaryTasks = [...week.temporaryTasks, task]
  }
  persist(db)
  return task
}

export function deleteTask(id: string): void {
  const db = load()
  db.roles = db.roles.map(r => ({ ...r, tasks: r.tasks.filter(t => t.id !== id) }))
  Object.values(db.weekData).forEach(week => {
    week.temporaryTasks = week.temporaryTasks.filter(t => t.id !== id)
    week.scheduledTasks = week.scheduledTasks.filter(t => t.taskId !== id)
  })
  persist(db)
}

function findTask(db: LocalDB, id: string): Task | undefined {
  for (const role of db.roles) {
    const found = role.tasks.find(t => t.id === id)
    if (found) return found
  }
  for (const week of Object.values(db.weekData)) {
    const found = week.temporaryTasks.find(t => t.id === id)
    if (found) return found
  }
  return undefined
}

export function updateTask(
  id: string,
  payload: { title: string; isPermanent: boolean; weekStart?: string }
): Task {
  const db = load()
  const existing = findTask(db, id)
  if (!existing) throw new Error(`Task not found: ${id}`)

  const updated: Task = { ...existing, title: payload.title, isPermanent: payload.isPermanent }

  if (existing.isPermanent && !payload.isPermanent) {
    // 永続 -> 一時: その週のweek_dataへ付け替える
    db.roles = db.roles.map(r => ({ ...r, tasks: r.tasks.filter(t => t.id !== id) }))
    const week = getOrCreateWeek(db, payload.weekStart!)
    week.temporaryTasks = [...week.temporaryTasks, updated]
  } else if (!existing.isPermanent && payload.isPermanent) {
    // 一時 -> 永続: week_dataとの紐付けを外す
    Object.values(db.weekData).forEach(week => {
      week.temporaryTasks = week.temporaryTasks.filter(t => t.id !== id)
    })
    db.roles = db.roles.map(r => (r.id === updated.roleId ? { ...r, tasks: [...r.tasks, updated] } : r))
  } else if (existing.isPermanent) {
    db.roles = db.roles.map(r => ({ ...r, tasks: r.tasks.map(t => (t.id === id ? updated : t)) }))
  } else {
    Object.values(db.weekData).forEach(week => {
      week.temporaryTasks = week.temporaryTasks.map(t => (t.id === id ? updated : t))
    })
  }

  persist(db)
  return updated
}

export function reorderTasks(items: { id: string; sortOrder: number }[]): void {
  const db = load()
  const order = new Map(items.map(i => [i.id, i.sortOrder]))
  const sortBy = <T extends { id: string }>(list: T[]): T[] =>
    [...list].sort((a, b) => (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0))
  db.roles = db.roles.map(r => ({ ...r, tasks: sortBy(r.tasks) }))
  Object.values(db.weekData).forEach(week => {
    week.temporaryTasks = sortBy(week.temporaryTasks)
  })
  persist(db)
}

// --- week data / scheduled tasks / day notes ---

export function getWeekData(weekStart: string): WeekData {
  const db = load()
  const week = getOrCreateWeek(db, weekStart)
  persist(db)
  return { weekStart: new Date(weekStart), ...week }
}

export function updateWeeklyNotes(weekStart: string, notes: string): void {
  const db = load()
  getOrCreateWeek(db, weekStart).weeklyNotes = notes
  persist(db)
}

interface CreateScheduledTaskInput {
  taskId: string
  day: number
  startTime: string
  duration: number
  title: string
  roleId: string
}

export function createScheduledTask(weekStart: string, input: CreateScheduledTaskInput): ScheduledTask {
  const db = load()
  const week = getOrCreateWeek(db, weekStart)
  const task: ScheduledTask = { id: nextId(db), completed: false, ...input }
  week.scheduledTasks = [...week.scheduledTasks, task]
  persist(db)
  return task
}

export function updateScheduledTask(
  id: string,
  updates: Partial<Pick<ScheduledTask, 'day' | 'startTime' | 'duration' | 'title' | 'completed'>>
): void {
  const db = load()
  Object.values(db.weekData).forEach(week => {
    week.scheduledTasks = week.scheduledTasks.map(t => (t.id === id ? { ...t, ...updates } : t))
  })
  persist(db)
}

export function deleteScheduledTask(id: string): void {
  const db = load()
  Object.values(db.weekData).forEach(week => {
    week.scheduledTasks = week.scheduledTasks.filter(t => t.id !== id)
  })
  persist(db)
}

export function upsertDayNotes(
  weekStart: string,
  day: number,
  updates: Partial<Pick<DayNotes, 'notes' | 'sleepStart' | 'sleepEnd'>>
): void {
  const db = load()
  const week = getOrCreateWeek(db, weekStart)
  week.dayNotes = week.dayNotes.map(dn => (dn.day === day ? { ...dn, ...updates } : dn))
  persist(db)
}

// --- sharpen the saw ---

export function getSharpenTheSawAreas(): SharpenTheSawArea[] {
  return load().sharpenTheSawAreas
}

export function updateSharpenTheSawAreas(areas: SharpenTheSawArea[]): SharpenTheSawArea[] {
  const db = load()
  db.sharpenTheSawAreas = db.sharpenTheSawAreas.map(existingArea => {
    const incoming = areas.find(a => a.id === existingArea.id)
    if (!incoming) return existingArea

    const existingById = new Map(existingArea.tasks.map(t => [t.id, t]))
    const tasks: Task[] = []
    for (const t of incoming.tasks) {
      const title = t.title.trim()
      if (!title) continue // サーバー側と同様、空タイトルは保存せず(=削除扱い)にする
      const match = existingById.get(t.id)
      tasks.push(match ? { ...match, title } : { id: nextId(db), title, roleId: 'renewal', isPermanent: true })
    }
    return { ...existingArea, tasks }
  })
  persist(db)
  return db.sharpenTheSawAreas
}

// --- mission statement ---

export function getMissionStatement(): string {
  return load().missionStatement
}

export function updateMissionStatement(text: string): void {
  const db = load()
  db.missionStatement = text
  persist(db)
}
