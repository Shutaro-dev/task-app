// 旧localStorage運用時代のデータをDBへ一度だけ取り込むための移行処理。
// Dashboard.tsxのマウント時、DB側にまだロールが1件も無く・かつ未移行のlocalStorageが
// 残っている場合にのみ呼ばれる。
import type { Role, SharpenTheSawArea, WeekData } from '../types'
import * as roleService from './roleService'
import * as taskService from './taskService'
import * as weekDataService from './weekDataService'
import * as sharpenTheSawService from './sharpenTheSawService'
import * as missionStatementService from './missionStatementService'

const ROLE_COLORS = ['#4a90d9', '#e67e22', '#27ae60', '#8e44ad', '#e74c3c', '#16a085']

interface LegacyState {
  roles: Role[]
  sharpenTheSawAreas: SharpenTheSawArea[]
  missionStatement: string
  weekData: [string, WeekData][]
  isListMode: boolean
}

function parseLegacyState(raw: string): LegacyState | null {
  try {
    const parsed = JSON.parse(raw)
    return {
      roles: Array.isArray(parsed.roles) ? parsed.roles : [],
      sharpenTheSawAreas: Array.isArray(parsed.sharpenTheSawAreas) ? parsed.sharpenTheSawAreas : [],
      missionStatement: typeof parsed.missionStatement === 'string' ? parsed.missionStatement : '',
      weekData: Array.isArray(parsed.weekData) ? parsed.weekData : [],
      isListMode: typeof parsed.isListMode === 'boolean' ? parsed.isListMode : false,
    }
  } catch (error) {
    console.warn('Failed to parse legacy localStorage data for migration:', error)
    return null
  }
}

function migratedFlagKey(storageKey: string): string {
  return `${storageKey}:migrated`
}

export function hasPendingLegacyData(storageKey: string): boolean {
  if (localStorage.getItem(migratedFlagKey(storageKey))) return false
  return !!localStorage.getItem(storageKey)
}

// isListMode(表示モード)はDB移行の成否と無関係に即座に復元したいので、
// Dashboard.tsxのuseState lazy initializerから同期的に呼ばれる想定の純粋な読み取り専用ヘルパー
export function readLegacyListMode(storageKey: string): boolean | undefined {
  const raw = localStorage.getItem(storageKey)
  if (!raw) return undefined
  return parseLegacyState(raw)?.isListMode
}

// 実際にDBへ書き込みを始める前に呼ぶガード。GET /api/rolesが空でない=既に
// このユーザーのデータがDB側にある(既存ユーザー、または移行済み)ので、二重取り込みを避ける
export async function migrateLegacyLocalStorageIfNeeded(storageKey: string): Promise<boolean> {
  if (!hasPendingLegacyData(storageKey)) return false

  const existingRoles = await roleService.fetchRoles()
  if (existingRoles.length > 0) {
    // DB側に既にデータがあるなら、このブラウザのlocalStorageは古い残骸とみなして触れない
    return false
  }

  const raw = localStorage.getItem(storageKey)
  if (!raw) return false
  const legacy = parseLegacyState(raw)
  if (!legacy) return false

  await runMigration(legacy)

  localStorage.setItem(migratedFlagKey(storageKey), 'true')
  localStorage.removeItem(storageKey)
  return true
}

async function runMigration(legacy: LegacyState): Promise<void> {
  const roleIdMap = new Map<string, string>()
  const taskIdMap = new Map<string, string>()

  for (const [index, role] of legacy.roles.entries()) {
    const created = await roleService.createRole(
      role.name,
      role.color || ROLE_COLORS[index % ROLE_COLORS.length],
      role.isExpanded
    )
    roleIdMap.set(role.id, created.id)

    for (const task of role.tasks.filter(t => t.isPermanent)) {
      const createdTask = await taskService.createTask({ roleId: created.id, title: task.title, isPermanent: true })
      taskIdMap.set(task.id, createdTask.id)
    }
  }

  if (legacy.sharpenTheSawAreas.length > 0) {
    await sharpenTheSawService.updateSharpenTheSawAreas(legacy.sharpenTheSawAreas)
  }

  if (legacy.missionStatement.trim()) {
    await missionStatementService.updateMissionStatement(legacy.missionStatement)
  }

  for (const [weekStart, weekData] of legacy.weekData) {
    try {
      await migrateWeek(weekStart, weekData, roleIdMap, taskIdMap)
    } catch (error) {
      // 1週分のデータ不整合(削除済みロールへの参照等)で移行処理全体を止めない。
      // 他の週や、既に作成済みのロール/タスクの重複作成を避けるため続行する
      console.warn(`Migration: failed to migrate week "${weekStart}", skipping it`, error)
    }
  }
}

async function migrateWeek(
  weekStart: string,
  weekData: WeekData,
  roleIdMap: Map<string, string>,
  taskIdMap: Map<string, string>
): Promise<void> {
  if (weekData.weeklyNotes?.trim()) {
    await weekDataService.updateWeeklyNotes(weekStart, weekData.weeklyNotes)
  }

  for (const task of weekData.temporaryTasks ?? []) {
    const roleId = roleIdMap.get(task.roleId)
    if (!roleId) {
      // 参照元のロールが見つからない(削除済みロールへの参照等、旧データの不整合)場合は諦める
      console.warn(`Migration: skipping temporary task "${task.title}" — source role not found`)
      continue
    }
    const created = await taskService.createTask({ roleId, title: task.title, isPermanent: false, weekStart })
    taskIdMap.set(task.id, created.id)
  }

  for (const dayNote of weekData.dayNotes) {
    const hasContent = dayNote.notes?.trim() || dayNote.sleepStart || dayNote.sleepEnd
    if (!hasContent) continue
    await weekDataService.upsertDayNotes(weekStart, dayNote.day, {
      notes: dayNote.notes,
      sleepStart: dayNote.sleepStart,
      sleepEnd: dayNote.sleepEnd,
    })
  }

  for (const scheduled of weekData.scheduledTasks) {
    const taskId = taskIdMap.get(scheduled.taskId)
    const roleId = roleIdMap.get(scheduled.roleId)
    if (!taskId || !roleId) {
      // 参照元のタスク/ロールが見つからない(旧データの不整合)場合はスケジュールごと諦める
      console.warn(`Migration: skipping scheduled task "${scheduled.title}" — source task or role not found`)
      continue
    }
    const created = await weekDataService.createScheduledTask(weekStart, {
      taskId,
      day: scheduled.day,
      startTime: scheduled.startTime,
      duration: scheduled.duration,
      title: scheduled.title,
      roleId,
    })
    if (scheduled.completed) {
      await weekDataService.updateScheduledTask(created.id, { completed: true })
    }
  }
}
