class CreateTasks < ActiveRecord::Migration[8.1]
  def change
    create_table :tasks, id: :serial do |t|
      t.string :title, null: false
      t.references :role, type: :integer, null: false, foreign_key: { to_table: :roles, primary_key: :role_id, on_delete: :cascade }, index: true
      t.boolean :is_permanent, default: false
      t.integer :sort_order, default: 0
      t.timestamps
    end
  end
end
