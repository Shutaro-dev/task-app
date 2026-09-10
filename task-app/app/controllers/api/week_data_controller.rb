module Api
  class WeekDataController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:update]

    def show
      render json: week_data_json(find_or_create_week_data)
    end

    def update
      week_data = find_or_create_week_data
      week_data.update!(weekly_notes: params[:weeklyNotes]) if params.key?(:weeklyNotes)
      render json: week_data_json(week_data)
    end

    private

    def find_or_create_week_data
      current_user.week_data.find_or_create_by!(week_start: params[:week_start])
    end

    def week_data_json(week_data)
      {
        weekStart: week_data.week_start.iso8601,
        weeklyNotes: week_data.weekly_notes || "",
        scheduledTasks: week_data.scheduled_tasks.order(:id).map { |task| scheduled_task_json(task) },
        dayNotes: week_data.day_notes.order(:day).map { |note| day_note_json(note) },
        temporaryTasks: week_data.temporary_tasks.order(:sort_order, :id).map { |task| task_json(task) },
      }
    end

    def scheduled_task_json(task)
      {
        id: task.id,
        taskId: task.task_id,
        day: task.day,
        startTime: task.start_time.strftime("%H:%M"),
        duration: task.duration,
        title: task.title,
        roleId: task.role_id,
        completed: task.completed,
      }
    end

    def day_note_json(day_note)
      {
        day: day_note.day,
        notes: day_note.notes || "",
        sleepStart: day_note.sleep_start&.strftime("%H:%M"),
        sleepEnd: day_note.sleep_end&.strftime("%H:%M"),
      }
    end

    def task_json(task)
      { id: task.id, title: task.title, roleId: task.role_id, isPermanent: task.is_permanent }
    end
  end
end
