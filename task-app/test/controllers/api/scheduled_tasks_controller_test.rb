require "test_helper"

module Api
  class ScheduledTasksControllerTest < ActionDispatch::IntegrationTest
    WEEK_START = "2026-09-07"
    NON_EXISTING_ID = 9_999_997

    setup do
      @user = create_and_sign_in_user
      @role = Role.create!(user: @user, role_name: "Role")
      @task = Task.create!(user: @user, role_id: @role.role_id, title: "Task", is_permanent: true)
    end

    test "normal01: 作成するとweek_dataも自動作成されて紐づく" do
      post "/api/week_data/#{WEEK_START}/scheduled_tasks",
        params: base_params.to_json,
        headers: json_headers

      assert_response :created
      body = JSON.parse(response.body)
      assert_equal @task.id, body["taskId"]
      assert_equal @role.role_id, body["roleId"]
      assert_equal "09:00", body["startTime"]
      assert_equal false, body["completed"]
      assert_equal 1, WeekData.where(user: @user, week_start: WEEK_START).count
    end

    test "normal02: startTime/durationを部分更新できる" do
      scheduled = create_scheduled_task

      put "/api/scheduled_tasks/#{scheduled['id']}", params: { startTime: "10:30", duration: 90 }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal "10:30", body["startTime"]
      assert_equal 90, body["duration"]
    end

    test "normal03: completedを更新できる" do
      scheduled = create_scheduled_task

      put "/api/scheduled_tasks/#{scheduled['id']}", params: { completed: true }.to_json, headers: json_headers

      assert_response :success
      assert_equal true, JSON.parse(response.body)["completed"]
    end

    test "normal04: 削除できる" do
      scheduled = create_scheduled_task

      delete "/api/scheduled_tasks/#{scheduled['id']}"

      assert_response :no_content
      assert_equal 0, ScheduledTask.count
    end

    test "error01: 存在しないtaskIdのとき404を返す" do
      post "/api/week_data/#{WEEK_START}/scheduled_tasks",
        params: base_params.merge(taskId: NON_EXISTING_ID).to_json,
        headers: json_headers

      assert_response :not_found
    end

    test "error03: titleが未指定のとき400を返し何も作成されない" do
      post "/api/week_data/#{WEEK_START}/scheduled_tasks",
        params: base_params.except(:title).to_json,
        headers: json_headers

      assert_response :bad_request
      assert_equal 0, ScheduledTask.count
      assert_equal 0, WeekData.count
    end

    test "error04: day=0(月曜)は必須チェックでブロックされず作成できる" do
      post "/api/week_data/#{WEEK_START}/scheduled_tasks", params: base_params.merge(day: 0).to_json, headers: json_headers

      assert_response :created
      assert_equal 0, JSON.parse(response.body)["day"]
    end

    test "error02: 他ユーザーのscheduled_taskは更新できない" do
      other_user = User.create!(email: "other@example.com", password: "password12345")
      other_role = Role.create!(user: other_user, role_name: "OtherRole")
      other_task = Task.create!(user: other_user, role_id: other_role.role_id, title: "OtherTask", is_permanent: true)
      other_week = WeekData.create!(user: other_user, week_start: WEEK_START)
      other_scheduled = ScheduledTask.create!(
        week_data: other_week, task: other_task, role_id: other_role.role_id,
        day: 0, start_time: "09:00", duration: 60, title: "OtherTask"
      )

      put "/api/scheduled_tasks/#{other_scheduled.id}", params: { completed: true }.to_json, headers: json_headers

      assert_response :not_found
    end

    private

    def base_params
      { taskId: @task.id, roleId: @role.role_id, day: 0, startTime: "09:00", duration: 60, title: "Task" }
    end

    def create_scheduled_task
      post "/api/week_data/#{WEEK_START}/scheduled_tasks", params: base_params.to_json, headers: json_headers
      JSON.parse(response.body)
    end

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
