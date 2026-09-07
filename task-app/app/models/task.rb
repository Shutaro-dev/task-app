class Task < ApplicationRecord
  belongs_to :role, foreign_key: "role_id", inverse_of: :tasks
  belongs_to :user, optional: true
end
