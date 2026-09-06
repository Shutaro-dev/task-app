class Role < ApplicationRecord
  self.primary_key = "role_id"

  has_many :tasks, -> { order(:sort_order, :id) }, foreign_key: "role_id", inverse_of: :role, dependent: :destroy
end
