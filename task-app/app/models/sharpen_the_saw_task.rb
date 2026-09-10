class SharpenTheSawTask < ApplicationRecord
  belongs_to :sharpen_the_saw_area, foreign_key: "area_id", inverse_of: :sharpen_the_saw_tasks
  belongs_to :user
end
