module Api
  class TasksController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:create, :update, :reorder]

    def index
      tasks = current_user.tasks.order(:sort_order, :id)
      render json: tasks.map { |task| task_json(task) }
    end

    def create
      role_id = params[:roleId]
      title = params[:title]
      is_permanent = params[:isPermanent]
      return render_bad_request if role_id.nil? || title.blank? || is_permanent.nil?

      role = current_user.roles.find_by(role_id: role_id)
      raise ActiveRecord::RecordNotFound, "Role not found: #{role_id}" unless role

      # taskId が指定されていても Spring 版と同じく無視し、常に新規作成する
      current_user.tasks.create!(role_id: role.role_id, title: title, is_permanent: is_permanent)

      head :created
    end

    def update
      task = current_user.tasks.find_by(id: params[:id])
      raise ActiveRecord::RecordNotFound, "Task not found: #{params[:id]}" unless task

      title = params[:title]
      is_permanent = params[:isPermanent]
      return render_bad_request if title.blank? || is_permanent.nil?

      task.update!(title: title, is_permanent: is_permanent)
      render json: task_json(task)
    end

    def reorder
      (params[:_json] || []).each do |item|
        current_user.tasks.where(id: item[:id]).update_all(sort_order: item[:sortOrder])
      end
      head :ok
    end

    def destroy
      task = current_user.tasks.find_by(id: params[:id])
      raise ActiveRecord::RecordNotFound, "Task not found: #{params[:id]}" unless task

      task.destroy!
      head :no_content
    end

    private

    def render_bad_request
      render json: { error: "roleId, title and isPermanent are required" }, status: :bad_request
    end

    def task_json(task)
      {
        taskId: task.id,
        roleId: task.role_id,
        title: task.title,
        isPermanent: task.is_permanent
      }
    end
  end
end
