class Task < ApplicationRecord
  belongs_to :role, foreign_key: "role_id", inverse_of: :tasks
  belongs_to :user, optional: true
  # 永続タスクは常にnil。一時タスクだけ、その週のweek_dataに紐づく。
  # "week_data"はRailsの英語推論だと単数"WeekDatum"になってしまうため明示する
  belongs_to :week_data, class_name: "WeekData", optional: true
  has_many :scheduled_tasks, dependent: :destroy
end
