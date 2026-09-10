class WeekData < ApplicationRecord
  belongs_to :user, optional: true
  has_many :scheduled_tasks, dependent: :destroy
  has_many :day_notes, dependent: :destroy
  has_many :temporary_tasks, -> { where(is_permanent: false) }, class_name: "Task"
end
