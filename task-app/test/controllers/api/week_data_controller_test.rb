require "test_helper"

module Api
  class WeekDataControllerTest < ActionDispatch::IntegrationTest
    WEEK_START = "2026-09-07"

    setup do
      @user = create_and_sign_in_user
    end

    test "normal01: 初回アクセスでweek_dataが自動作成され空のデータを返す" do
      get "/api/week_data/#{WEEK_START}"

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal WEEK_START, body["weekStart"]
      assert_equal "", body["weeklyNotes"]
      assert_equal [], body["scheduledTasks"]
      assert_equal [], body["dayNotes"]
      assert_equal [], body["temporaryTasks"]
      assert_equal 1, WeekData.where(user: @user, week_start: WEEK_START).count
    end

    test "normal02: 2回アクセスしても重複作成されない" do
      get "/api/week_data/#{WEEK_START}"
      get "/api/week_data/#{WEEK_START}"

      assert_response :success
      assert_equal 1, WeekData.where(user: @user, week_start: WEEK_START).count
    end

    test "normal03: weeklyNotesを更新できる" do
      put "/api/week_data/#{WEEK_START}", params: { weeklyNotes: "今週の振り返り" }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal "今週の振り返り", body["weeklyNotes"]
    end

    test "normal04: 一時タスクのみtemporaryTasksに含まれる" do
      role = Role.create!(user: @user, role_name: "Role")
      week_data = WeekData.create!(user: @user, week_start: WEEK_START)
      Task.create!(user: @user, role_id: role.role_id, title: "永続", is_permanent: true)
      Task.create!(user: @user, role_id: role.role_id, title: "一時", is_permanent: false, week_data: week_data)

      get "/api/week_data/#{WEEK_START}"

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal ["一時"], body["temporaryTasks"].map { |t| t["title"] }
    end

    test "error01: 他ユーザーのweek_dataは見えない(自分の別行が新規作成される)" do
      other_user = User.create!(email: "other@example.com", password: "password12345")
      WeekData.create!(user: other_user, week_start: WEEK_START, weekly_notes: "他人のメモ")

      get "/api/week_data/#{WEEK_START}"

      assert_response :success
      assert_equal "", JSON.parse(response.body)["weeklyNotes"]
    end

    private

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
