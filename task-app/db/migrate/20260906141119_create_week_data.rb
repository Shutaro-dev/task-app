class CreateWeekData < ActiveRecord::Migration[8.1]
  def change
    create_table :week_data, id: :serial do |t|
      t.date :week_start, null: false
      t.text :weekly_notes
      t.timestamps
    end

    add_index :week_data, :week_start, unique: true
  end
end
