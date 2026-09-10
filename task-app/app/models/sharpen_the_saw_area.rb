class SharpenTheSawArea < ApplicationRecord
  has_many :sharpen_the_saw_tasks, foreign_key: "area_id", inverse_of: :sharpen_the_saw_area
end
