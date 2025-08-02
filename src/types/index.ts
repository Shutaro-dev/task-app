export interface Task {
    id: string;
    title: string;
    roleId: string;
    isPermanent: boolean;
}

export interface Role {
    id: string;
    name: string;
    tasks: Task[];
    isExpanded: boolean;
    showAddTask?: boolean;
}

export interface ScheduledTask {
    id: string;
    taskId: string;
    day: number; // 0-6 (Monday-Sunday)
    startTime: string; // HH:MM format
    duration: number; // in minutes
    title: string;
    roleId: string;
}

export interface SharpenTheSawArea {
    id: string;
    name: string;
    icon: string;
    tasks: Task[];
}

export interface DayNotes {
    day: number;
    notes: string;
    sleepStart?: string; // HH:MM
    sleepEnd?: string; // HH:MM
}

export interface WeekData {
    weekStart: Date;
    scheduledTasks: ScheduledTask[];
    dayNotes: DayNotes[];
    weeklyNotes: string;
    temporaryTasks?: Task[];
}
