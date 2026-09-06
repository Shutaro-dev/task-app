class CreateSharpenTheSawAreas < ActiveRecord::Migration[8.1]
  def change
    create_table :sharpen_the_saw_areas, id: :string, limit: 50 do |t|
      t.string :name, null: false
      t.string :icon, limit: 10, null: false
      t.timestamps
    end
  end
end
