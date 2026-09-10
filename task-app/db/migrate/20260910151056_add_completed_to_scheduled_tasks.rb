class AddCompletedToScheduledTasks < ActiveRecord::Migration[8.1]
  def change
    add_column :scheduled_tasks, :completed, :boolean, default: false, null: false
  end
end
