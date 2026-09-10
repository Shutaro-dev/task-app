class AddWeekDataIdToTasks < ActiveRecord::Migration[8.1]
  def change
    # 一時タスク(is_permanent: false)を特定の週に紐づけるための列。
    # 永続タスクは常に NULL。週(week_data)が削除されたら一時タスクは
    # 孤立させず外部キーだけ外す(タスク自体は消さない)ため on_delete: :nullify。
    add_reference :tasks, :week_data, type: :integer, null: true,
      foreign_key: { on_delete: :nullify }, index: true
  end
end
