# This file should ensure the existence of records required to run the application in every environment
# (production, development, test). The code here should be idempotent so that it can be executed at any point
# in every environment. The data can then be loaded with the bin/rails db:seed command
# (or created alongside the database with db:setup).
#
# 元の database_schema.sql の初期データ(INSERT文)と同内容

SharpenTheSawArea = Class.new(ApplicationRecord) { self.table_name = "sharpen_the_saw_areas" } unless defined?(SharpenTheSawArea)

[
  { id: "Body", name: "Body", icon: "💪" },
  { id: "Intelligence", name: "Intelligence", icon: "🧠" },
  { id: "Social・Emotional", name: "Social・Emotional", icon: "❤️" },
  { id: "Mental", name: "Mental", icon: "🙏" }
].each do |attrs|
  SharpenTheSawArea.find_or_create_by!(id: attrs[:id]) do |area|
    area.name = attrs[:name]
    area.icon = attrs[:icon]
  end
end

dev_user = User.find_or_create_by!(email: "dev@example.com") do |user|
  user.password = "password123"
  user.name = "Dev User"
end

[
  { role_name: "Professional", is_expanded: true, color: "#4a90d9", sort_order: 1 },
  { role_name: "Family", is_expanded: true, color: "#e67e22", sort_order: 2 }
].each do |attrs|
  dev_user.roles.find_or_create_by!(role_name: attrs[:role_name]) do |role|
    role.is_expanded = attrs[:is_expanded]
    role.color = attrs[:color]
    role.sort_order = attrs[:sort_order]
  end
end
