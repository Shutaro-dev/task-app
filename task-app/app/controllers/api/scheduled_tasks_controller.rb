module Api
  class ScheduledTasksController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:create, :update]

    def create
      return render_bad_request if [:taskId, :roleId, :day, :startTime, :duration, :title].any? { |key| params[key].blank? }

      week_data = current_user.week_data.find_or_create_by!(week_start: params[:week_start])

      task = current_user.tasks.find_by(id: params[:taskId])
      raise ActiveRecord::RecordNotFound, "Task not found: #{params[:taskId]}" unless task

      role = current_user.roles.find_by(role_id: params[:roleId])
      raise ActiveRecord::RecordNotFound, "Role not found: #{params[:roleId]}" unless role

      scheduled_task = week_data.scheduled_tasks.create!(
        task_id: task.id,
        role_id: role.role_id,
        day: params[:day],
        start_time: params[:startTime],
        duration: params[:duration],
        title: params[:title]
      )

      render json: scheduled_task_json(scheduled_task), status: :created
    end

    def update
      scheduled_task = find_scheduled_task

      attrs = {}
      attrs[:day] = params[:day] if params.key?(:day)
      attrs[:start_time] = params[:startTime] if params.key?(:startTime)
      attrs[:duration] = params[:duration] if params.key?(:duration)
      attrs[:title] = params[:title] if params.key?(:title)
      attrs[:completed] = params[:completed] if params.key?(:completed)
      scheduled_task.update!(attrs)

      render json: scheduled_task_json(scheduled_task)
    end

    def destroy
      find_scheduled_task.destroy!
      head :no_content
    end

    private

    def render_bad_request
      render json: { error: "taskId, roleId, day, startTime, duration and title are required" }, status: :bad_request
    end

    # week_data経由でuser_idスコープをかける(scheduled_tasksテーブル自体にuser_id列は無いため)
    def find_scheduled_task
      scheduled_task = ScheduledTask.joins(:week_data).find_by(id: params[:id], week_data: { user_id: current_user.id })
      raise ActiveRecord::RecordNotFound, "ScheduledTask not found: #{params[:id]}" unless scheduled_task

      scheduled_task
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
  end
end
