class AddUserIdToSharpenTheSawTasks < ActiveRecord::Migration[8.1]
  def change
    # このテーブルは現状どのAPIからも書き込まれておらず空のため、
    # NOT NULLで追加してもデータ移行は不要。
    add_reference :sharpen_the_saw_tasks, :user, type: :bigint, null: false, foreign_key: true, index: true
  end
end
