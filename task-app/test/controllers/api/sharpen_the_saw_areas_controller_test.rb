require "test_helper"

module Api
  class SharpenTheSawAreasControllerTest < ActionDispatch::IntegrationTest
    setup do
      SharpenTheSawTask.delete_all
      @user = create_and_sign_in_user
    end

    test "normal01: 4領域を固定順で返す" do
      get "/api/sharpen_the_saw_areas"

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal %w[physical mental social-emotional spiritual], body.map { |a| a["id"] }
      assert_equal [], body[0]["tasks"]
    end

    test "normal02: 一括更新でタスクが新規作成される" do
      put "/api/sharpen_the_saw_areas",
        params: [{ id: "physical", tasks: [{ title: "ジョギング" }] }].to_json,
        headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      physical = body.find { |a| a["id"] == "physical" }
      assert_equal ["ジョギング"], physical["tasks"].map { |t| t["title"] }
      assert_equal 1, current_user_task_count("physical")
    end

    test "normal03: 一括更新で既存タスクを更新・削除できる" do
      kept = SharpenTheSawTask.create!(user: @user, area_id: "physical", title: "旧タイトル")
      SharpenTheSawTask.create!(user: @user, area_id: "physical", title: "削除される")

      put "/api/sharpen_the_saw_areas",
        params: [{ id: "physical", tasks: [{ id: kept.id, title: "新タイトル" }] }].to_json,
        headers: json_headers

      assert_response :success
      assert_equal "新タイトル", kept.reload.title
      assert_equal 1, current_user_task_count("physical")
    end

    test "normal04: タイトル空文字のタスクは保存されない" do
      put "/api/sharpen_the_saw_areas",
        params: [{ id: "physical", tasks: [{ title: "" }] }].to_json,
        headers: json_headers

      assert_response :success
      assert_equal 0, current_user_task_count("physical")
    end

    test "normal05: 他ユーザーのタスクには影響しない" do
      other_user = User.create!(email: "other@example.com", password: "password12345")
      other_task = SharpenTheSawTask.create!(user: other_user, area_id: "physical", title: "他人のタスク")

      put "/api/sharpen_the_saw_areas", params: [{ id: "physical", tasks: [] }].to_json, headers: json_headers

      assert_response :success
      assert SharpenTheSawTask.exists?(other_task.id)
    end

    private

    def current_user_task_count(area_id)
      @user.sharpen_the_saw_tasks.where(area_id: area_id).count
    end

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
