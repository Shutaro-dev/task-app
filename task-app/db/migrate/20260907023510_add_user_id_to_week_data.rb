class AddUserIdToWeekData < ActiveRecord::Migration[8.1]
  def up
    add_reference :week_data, :user, null: true, foreign_key: true, index: true

    # week_start は「全ユーザー共通で一意」だったが、ユーザーごとに同じ週を
    # 持てるよう (user_id, week_start) の複合ユニークに置き換える。
    # unique_constraint/unique index どちらで存在していても対応できるようにする。
    execute "ALTER TABLE week_data DROP CONSTRAINT IF EXISTS week_data_week_start_key"
    remove_index :week_data, :week_start, if_exists: true

    add_index :week_data, [:user_id, :week_start], unique: true, name: "idx_week_data_user_id_week_start_unique"
  end

  def down
    remove_index :week_data, name: "idx_week_data_user_id_week_start_unique", if_exists: true
    add_index :week_data, :week_start, unique: true
    remove_reference :week_data, :user, foreign_key: true
  end
end
