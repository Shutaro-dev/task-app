class ScheduledTask < ApplicationRecord
  # "week_data"はRailsの英語推論だと単数"WeekDatum"になってしまうため明示する
  belongs_to :week_data, class_name: "WeekData"
  belongs_to :task
  belongs_to :role, foreign_key: "role_id", primary_key: "role_id", inverse_of: false
end
