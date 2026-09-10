module Api
  class SharpenTheSawAreasController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:update]

    # 表示順はフロントの DEFAULT_SAW_AREAS (Dashboard.tsx) と揃える。
    # id文字列のアルファベット順とは一致しないため固定配列で明示する。
    AREA_ORDER = %w[physical mental social-emotional spiritual].freeze

    def index
      render json: areas_json
    end

    # body: [{ id: areaId, tasks: [{ id?, title }] }] (SharpenTheSawSettingsが
    # 保存ボタン押下時に全領域まとめて送る一括更新)
    def update
      (params[:_json] || []).each { |item| sync_area(item) }
      render json: areas_json
    end

    private

    def sync_area(item)
      area = SharpenTheSawArea.find_by(id: item[:id])
      return unless area

      existing = current_user.sharpen_the_saw_tasks.where(area_id: area.id).index_by { |t| t.id.to_s }
      kept_ids = (item[:tasks] || []).filter_map { |attrs| upsert_task(area, existing, attrs) }

      (existing.values.map(&:id) - kept_ids).each { |id| current_user.sharpen_the_saw_tasks.find(id).destroy! }
    end

    def upsert_task(area, existing, attrs)
      title = attrs[:title].to_s.strip
      return nil if title.blank?

      existing_task = existing[attrs[:id].to_s]
      if existing_task
        existing_task.update!(title: title)
        existing_task.id
      else
        current_user.sharpen_the_saw_tasks.create!(area_id: area.id, title: title).id
      end
    end

    def areas_json
      areas_by_id = SharpenTheSawArea.all.index_by(&:id)
      tasks_by_area = current_user.sharpen_the_saw_tasks.order(:id).group_by(&:area_id)

      AREA_ORDER.filter_map { |id| areas_by_id[id] }.map do |area|
        {
          id: area.id,
          name: area.name,
          icon: area.icon,
          tasks: (tasks_by_area[area.id] || []).map { |task| task_json(task) },
        }
      end
    end

    def task_json(task)
      { id: task.id, title: task.title, roleId: "renewal", isPermanent: true }
    end
  end
end
