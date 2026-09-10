class AddMissionStatementToUsers < ActiveRecord::Migration[8.1]
  def change
    add_column :users, :mission_statement, :text
  end
end
