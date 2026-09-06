class CreateRoles < ActiveRecord::Migration[8.1]
  def change
    create_table :roles, primary_key: :role_id, id: :serial do |t|
      t.string :role_name, null: false
      t.boolean :is_expanded, default: true
      t.string :color, limit: 7, default: "#4a90d9"
      t.integer :sort_order, default: 0
      t.timestamps
    end
  end
end
