class CreateUsers < ActiveRecord::Migration[8.1]
  def change
    create_table :users do |t|
      t.string :email, limit: 255, null: false
      t.string :password_digest, null: false
      t.string :name, limit: 255

      t.timestamps
    end

    add_index :users, "lower(email)", unique: true, name: "idx_users_email_unique"
  end
end
