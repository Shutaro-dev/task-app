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
        attrs = { role_name: role_name, color: params[:color] }
        attrs[:is_expanded] = params[:isExpanded] unless params[:isExpanded].nil?
        role = current_user.roles.create!(attrs)
      end

      # 新規作成時、フロントがクライアント側の仮IDを本物のroleIdへ差し替えられるようボディを返す
      render json: role_json(role), status: :created
    end

    def update
      role = current_user.roles.find_by(role_id: params[:id])
      raise ActiveRecord::RecordNotFound, "Role not found: #{params[:id]}" unless role

      return render_bad_request if [:roleName, :isExpanded, :color].none? { |key| params.key?(key) }

      attrs = {}
      # roleNameは色・isExpandedだけの部分更新(色ピッカー選択・折りたたみトグル単体のAPI呼び出し)
      # でも呼ばれるため、キーが送られてきたときだけ必須チェック・更新する
      if params.key?(:roleName)
        return render_role_name_blank if params[:roleName].blank?

        attrs[:role_name] = params[:roleName]
      end
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

    def render_bad_request
      render json: { error: "roleName, isExpanded or color is required" }, status: :bad_request
    end

    def role_json(role)
      {
        roleId: role.role_id,
        roleName: role.role_name,
        isExpanded: role.is_expanded,
        color: role.color,
        # 一時タスクも同じtasksテーブルに入るが、role.tasksには永続タスクのみ含める。
        # (Rubyでのselectではなく)SQL側でwhereして絞ることで、一時タスクが週を重ねて
        # 増えても毎回それらを取得しなくて済むようにする
        tasks: role.tasks.where(is_permanent: true).map { |task| task_json(task) }
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
