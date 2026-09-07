module Api
  class RolesController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:create, :update, :reorder]

    def index
      roles = current_user.roles.order(:sort_order, :role_id)
      render json: roles.map { |role| role_json(role) }
    end

    def create
      role_name = params[:roleName]
      return render_role_name_blank if role_name.blank?

      role_id = params[:roleId]
      if role_id.present?
        role = current_user.roles.find_by(role_id: role_id)
        raise ActiveRecord::RecordNotFound, "Role not found: #{role_id}" unless role

        role.update!(role_name: role_name, color: params[:color])
      else
        current_user.roles.create!(role_name: role_name, color: params[:color])
      end

      head :created
    end

    def update
      role = current_user.roles.find_by(role_id: params[:id])
      raise ActiveRecord::RecordNotFound, "Role not found: #{params[:id]}" unless role

      role_name = params[:roleName]
      return render_role_name_blank if role_name.blank?

      attrs = { role_name: role_name }
      attrs[:is_expanded] = params[:isExpanded] unless params[:isExpanded].nil?
      attrs[:color] = params[:color] unless params[:color].nil?
      role.update!(attrs)

      render json: role_json(role)
    end

    def reorder
      (params[:_json] || []).each do |item|
        current_user.roles.where(role_id: item[:id]).update_all(sort_order: item[:sortOrder])
      end
      head :ok
    end

    def destroy
      role = current_user.roles.find_by(role_id: params[:id])
      raise ActiveRecord::RecordNotFound, "Role not found: #{params[:id]}" unless role

      role.destroy!
      head :no_content
    end

    private

    def render_role_name_blank
      render json: { error: "roleName must not be blank" }, status: :bad_request
    end

    def role_json(role)
      {
        roleId: role.role_id,
        roleName: role.role_name,
        isExpanded: role.is_expanded,
        color: role.color,
        tasks: role.tasks.map { |task| task_json(task) }
      }
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
