class CreateSharpenTheSawTasks < ActiveRecord::Migration[8.1]
  def change
    create_table :sharpen_the_saw_tasks, id: :serial do |t|
      t.string :area_id, limit: 50, null: false
      t.string :title, null: false
      t.timestamps
    end

    add_foreign_key :sharpen_the_saw_tasks, :sharpen_the_saw_areas, column: :area_id, primary_key: :id, on_delete: :cascade
  end
end
