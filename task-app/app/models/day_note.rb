class DayNote < ApplicationRecord
  self.table_name = "day_notes"

  # "week_data"はRailsの英語推論だと単数"WeekDatum"になってしまうため明示する
  belongs_to :week_data, class_name: "WeekData"
end
