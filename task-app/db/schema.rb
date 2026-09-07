# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# This file is the source Rails uses to define your schema when running `bin/rails
# db:schema:load`. When creating a new database, `bin/rails db:schema:load` tends to
# be faster and is potentially less error prone than running all of your
# migrations from scratch. Old migrations may fail to apply correctly if those
# migrations use external dependencies or application code.
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema[8.1].define(version: 2026_09_07_023510) do
  # These are extensions that must be enabled in order to support this database
  enable_extension "pg_catalog.plpgsql"

  create_table "day_notes", id: :serial, force: :cascade do |t|
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.integer "day", null: false
    t.text "notes"
    t.time "sleep_end"
    t.time "sleep_start"
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.integer "week_data_id", null: false
    t.index ["day"], name: "idx_day_notes_day"
    t.index ["week_data_id"], name: "idx_day_notes_week_data_id"
    t.check_constraint "day >= 0 AND day <= 6", name: "day_notes_day_check"
    t.unique_constraint ["week_data_id", "day"], name: "day_notes_week_data_id_day_key"
  end

  create_table "roles", primary_key: "role_id", id: :serial, force: :cascade do |t|
    t.string "color", limit: 7, default: "#4a90d9"
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.boolean "is_expanded", default: true
    t.string "role_name", limit: 255, null: false
    t.integer "sort_order", default: 0
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.bigint "user_id"
    t.index ["user_id"], name: "index_roles_on_user_id"
  end

  create_table "scheduled_tasks", id: :serial, force: :cascade do |t|
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.integer "day", null: false
    t.integer "duration", null: false
    t.integer "role_id", null: false
    t.time "start_time", null: false
    t.integer "task_id", null: false
    t.string "title", limit: 255, null: false
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.integer "week_data_id", null: false
    t.index ["day"], name: "idx_scheduled_tasks_day"
    t.index ["week_data_id"], name: "idx_scheduled_tasks_week_data_id"
    t.check_constraint "day >= 0 AND day <= 6", name: "scheduled_tasks_day_check"
  end

  create_table "sharpen_the_saw_areas", id: { type: :string, limit: 50 }, force: :cascade do |t|
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.string "icon", limit: 10, null: false
    t.string "name", limit: 255, null: false
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
  end

  create_table "sharpen_the_saw_tasks", id: :serial, force: :cascade do |t|
    t.string "area_id", limit: 50, null: false
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.string "title", limit: 255, null: false
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
  end

  create_table "tasks", id: :serial, force: :cascade do |t|
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.boolean "is_permanent", default: false
    t.integer "role_id", null: false
    t.integer "sort_order", default: 0
    t.string "title", limit: 255, null: false
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.bigint "user_id"
    t.index ["role_id"], name: "idx_tasks_role_id"
    t.index ["user_id"], name: "index_tasks_on_user_id"
  end

  create_table "users", force: :cascade do |t|
    t.datetime "created_at", null: false
    t.string "email", limit: 255, null: false
    t.string "name", limit: 255
    t.string "password_digest", null: false
    t.datetime "updated_at", null: false
    t.index "lower((email)::text)", name: "idx_users_email_unique", unique: true
  end

  create_table "week_data", id: :serial, force: :cascade do |t|
    t.datetime "created_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.datetime "updated_at", precision: nil, default: -> { "CURRENT_TIMESTAMP" }
    t.bigint "user_id"
    t.date "week_start", null: false
    t.text "weekly_notes"
    t.index ["user_id", "week_start"], name: "idx_week_data_user_id_week_start_unique", unique: true
    t.index ["user_id"], name: "index_week_data_on_user_id"
  end

  add_foreign_key "day_notes", "week_data", column: "week_data_id", name: "day_notes_week_data_id_fkey", on_delete: :cascade
  add_foreign_key "roles", "users"
  add_foreign_key "scheduled_tasks", "roles", primary_key: "role_id", name: "scheduled_tasks_role_id_fkey", on_delete: :cascade
  add_foreign_key "scheduled_tasks", "tasks", name: "scheduled_tasks_task_id_fkey", on_delete: :cascade
  add_foreign_key "scheduled_tasks", "week_data", column: "week_data_id", name: "scheduled_tasks_week_data_id_fkey", on_delete: :cascade
  add_foreign_key "sharpen_the_saw_tasks", "sharpen_the_saw_areas", column: "area_id", name: "sharpen_the_saw_tasks_area_id_fkey", on_delete: :cascade
  add_foreign_key "tasks", "roles", primary_key: "role_id", name: "tasks_role_id_fkey", on_delete: :cascade
  add_foreign_key "tasks", "users"
  add_foreign_key "week_data", "users"
end
