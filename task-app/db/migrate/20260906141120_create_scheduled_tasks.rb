class CreateScheduledTasks < ActiveRecord::Migration[8.1]
  def change
    create_table :scheduled_tasks, id: :serial do |t|
      t.references :week_data, type: :integer, null: false, foreign_key: { on_delete: :cascade }, index: true
      t.references :task, type: :integer, null: false, foreign_key: { on_delete: :cascade }, index: true
      t.integer :day, null: false
      t.time :start_time, null: false
      t.integer :duration, null: false
      t.string :title, null: false
      # 元の database_schema.sql は roles(id) を参照していたが roles の主キーは role_id のため
      # 実際には適用不可能なFKだった(未使用テーブルのため無害)。ここで roles(role_id) に修正して移植。
      t.references :role, type: :integer, null: false, foreign_key: { to_table: :roles, primary_key: :role_id, on_delete: :cascade }, index: true
      t.timestamps
    end

    add_check_constraint :scheduled_tasks, "day >= 0 AND day <= 6", name: "scheduled_tasks_day_check"
    add_index :scheduled_tasks, :day, name: "idx_scheduled_tasks_day"
  end
end
