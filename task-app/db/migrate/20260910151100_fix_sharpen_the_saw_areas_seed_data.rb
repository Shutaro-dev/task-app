class FixSharpenTheSawAreasSeedData < ActiveRecord::Migration[8.1]
  # 旧Spring版から移植した際のシード値(Body/Intelligence/Social・Emotional/Mental)が
  # フロントエンドの DEFAULT_SAW_AREAS (front-task-app/src/components/Dashboard.tsx) の
  # id/name/icon と一致していなかったため、そちらに合わせて正しいマスタデータへ入れ替える。
  # sharpen_the_saw_tasks は現状どのAPIからも書き込まれておらず空のため、
  # 領域を丸ごと delete_all → 再投入して問題ない。
  class SharpenTheSawArea < ActiveRecord::Base
    self.table_name = "sharpen_the_saw_areas"
  end

  CANONICAL_AREAS = [
    { id: "physical", name: "Physical", icon: "💪" },
    { id: "mental", name: "Intellectual", icon: "🧠" },
    { id: "social-emotional", name: "Social/Emotional", icon: "❤️" },
    { id: "spiritual", name: "Spiritual", icon: "🙏" },
  ].freeze

  def up
    SharpenTheSawArea.delete_all
    CANONICAL_AREAS.each { |attrs| SharpenTheSawArea.create!(attrs) }
  end

  def down
    SharpenTheSawArea.delete_all
    [
      { id: "Body", name: "Body", icon: "💪" },
      { id: "Intelligence", name: "Intelligence", icon: "🧠" },
      { id: "Social・Emotional", name: "Social・Emotional", icon: "❤️" },
      { id: "Mental", name: "Mental", icon: "🙏" },
    ].each { |attrs| SharpenTheSawArea.create!(attrs) }
  end
end
