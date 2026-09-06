class CreateDayNotes < ActiveRecord::Migration[8.1]
  def change
    create_table :day_notes, id: :serial do |t|
      t.references :week_data, type: :integer, null: false, foreign_key: { on_delete: :cascade }, index: true
      t.integer :day, null: false
      t.text :notes
      t.time :sleep_start
      t.time :sleep_end
      t.timestamps
    end

    add_check_constraint :day_notes, "day >= 0 AND day <= 6", name: "day_notes_day_check"
    add_index :day_notes, :day, name: "idx_day_notes_day"
    add_index :day_notes, [:week_data_id, :day], unique: true
  end
end
