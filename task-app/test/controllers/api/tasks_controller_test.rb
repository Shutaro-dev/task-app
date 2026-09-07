require "test_helper"

module Api
  class TasksControllerTest < ActionDispatch::IntegrationTest
    TITLE_TEMP      = "Temporary Task"
    TITLE_PERMANENT = "Permanent Task"
    TITLE_JA        = "タスクのテスト"
    MAX_TITLE_LENGTH = 255
    NON_EXISTING_TASK_ID = 9_999_998
    NON_EXISTING_ROLE_ID = 9_999_999

    setup do
      Task.delete_all
      Role.delete_all
      @user = create_and_sign_in_user
      @role = Role.create!(user: @user, role_name: "TestRole")
    end

    # ─────────────────────────────────────────────────────────────────
    # POST /api/tasks
    # ─────────────────────────────────────────────────────────────────

    test "normal01: isPermanent=falseのタスクを作成できる" do
      post "/api/tasks", params: { roleId: @role.role_id, title: TITLE_TEMP, isPermanent: false }.to_json, headers: json_headers

      assert_response :created
      task = Task.first
      assert_equal TITLE_TEMP, task.title
      assert_equal @role.role_id, task.role_id
      assert_equal false, task.is_permanent
    end

    test "normal02: isPermanent=trueのタスクを作成できる" do
      post "/api/tasks", params: { roleId: @role.role_id, title: TITLE_PERMANENT, isPermanent: true }.to_json, headers: json_headers

      assert_response :created
      assert_equal true, Task.first.is_permanent
    end

    test "normal03: taskIdを指定しても無視され常に新規作成される" do
      post "/api/tasks", params: { taskId: 12_345, roleId: @role.role_id, title: TITLE_PERMANENT, isPermanent: true }.to_json, headers: json_headers

      assert_response :created
      assert_equal 1, Task.count
      assert_not_equal 12_345, Task.first.id
    end

    test "normal04: 日本語タイトルで作成できる" do
      post "/api/tasks", params: { roleId: @role.role_id, title: TITLE_JA, isPermanent: false }.to_json, headers: json_headers

      assert_response :created
      assert_equal TITLE_JA, Task.first.title
    end

    test "normal05: 最大長タイトルで作成できる" do
      max_length_title = "T" * MAX_TITLE_LENGTH
      post "/api/tasks", params: { roleId: @role.role_id, title: max_length_title, isPermanent: false }.to_json, headers: json_headers

      assert_response :created
      assert_equal max_length_title, Task.first.title
    end

    test "error01: titleが空文字のとき400を返す" do
      post "/api/tasks", params: { roleId: @role.role_id, title: "", isPermanent: false }.to_json, headers: json_headers

      assert_response :bad_request
      assert_equal 0, Task.count
    end

    test "error02: titleが未指定のとき400を返す" do
      post "/api/tasks", params: { roleId: @role.role_id, isPermanent: false }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error03: roleIdが未指定のとき400を返す" do
      post "/api/tasks", params: { title: TITLE_TEMP, isPermanent: false }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error04: リクエストボディなしのとき400を返す" do
      post "/api/tasks", headers: json_headers

      assert_response :bad_request
    end

    test "error05: Content-Type未指定のとき415を返す" do
      post "/api/tasks", params: { roleId: @role.role_id, title: TITLE_TEMP, isPermanent: false }.to_json

      assert_response :unsupported_media_type
      assert_equal 0, Task.count
    end

    test "error06: 存在しないroleIdのとき404を返しタスクが作成されない" do
      # ログイン導入時に current_user.roles.find_by で所有権を検証するようになったため、
      # 他のroleId系404レスポンスと同じ扱いになった(以前はDBのFK制約違反任せで422/500だった)
      post "/api/tasks", params: { roleId: NON_EXISTING_ROLE_ID, title: TITLE_TEMP, isPermanent: false }.to_json, headers: json_headers

      assert_response :not_found
      assert_equal 0, Task.count
    end

    test "error10: 他ユーザーのroleIdを指定すると404を返しタスクが作成されない" do
      other_user = User.create!(email: "other@example.com", password: TEST_USER_PASSWORD)
      other_role = Role.create!(user: other_user, role_name: "OtherUserRole")

      post "/api/tasks", params: { roleId: other_role.role_id, title: TITLE_TEMP, isPermanent: false }.to_json, headers: json_headers

      assert_response :not_found
      assert_equal 0, Task.count
    end

    # ─────────────────────────────────────────────────────────────────
    # GET /api/tasks
    # ─────────────────────────────────────────────────────────────────

    test "normal06: タスクが存在する場合200と一覧を返す" do
      Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      get "/api/tasks"

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal TITLE_TEMP, body[0]["title"]
      assert_equal @role.role_id, body[0]["roleId"]
      assert_equal false, body[0]["isPermanent"]
    end

    test "normal07: タスクが0件の場合200と空配列を返す" do
      get "/api/tasks"

      assert_response :success
      assert_equal [], JSON.parse(response.body)
    end

    # ─────────────────────────────────────────────────────────────────
    # PUT /api/tasks/reorder
    # ─────────────────────────────────────────────────────────────────

    test "normal08: reorderでsort_orderがDBに反映される" do
      a = Task.create!(user: @user, role_id: @role.role_id, title: "Task A", is_permanent: false)
      b = Task.create!(user: @user, role_id: @role.role_id, title: "Task B", is_permanent: false)

      put "/api/tasks/reorder", params: [
        { id: b.id, sortOrder: 0 },
        { id: a.id, sortOrder: 1 }
      ].to_json, headers: json_headers

      assert_response :success
      assert_equal 0, b.reload.sort_order
      assert_equal 1, a.reload.sort_order
    end

    test "normal09: 空配列のreorderでも200を返す" do
      put "/api/tasks/reorder", params: [].to_json, headers: json_headers

      assert_response :success
    end

    # ─────────────────────────────────────────────────────────────────
    # PUT /api/tasks/:id
    # ─────────────────────────────────────────────────────────────────

    test "normal10: titleを変更すると更新される" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      put "/api/tasks/#{task.id}", params: { title: "Updated Title", isPermanent: false }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal "Updated Title", body["title"]
      assert_equal "Updated Title", task.reload.title
    end

    test "normal11: isPermanentをtrueに変更するとDBに反映される" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      put "/api/tasks/#{task.id}", params: { title: TITLE_TEMP, isPermanent: true }.to_json, headers: json_headers

      assert_response :success
      assert_equal true, task.reload.is_permanent
    end

    test "normal12: isPermanentをfalseに変更するとDBに反映される" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_PERMANENT, is_permanent: true)

      put "/api/tasks/#{task.id}", params: { title: TITLE_PERMANENT, isPermanent: false }.to_json, headers: json_headers

      assert_response :success
      assert_equal false, task.reload.is_permanent
    end

    test "error07: titleが空文字のとき400を返す" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      put "/api/tasks/#{task.id}", params: { title: "", isPermanent: false }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error08: titleが未指定のとき400を返す" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      put "/api/tasks/#{task.id}", params: { isPermanent: true }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error09: isPermanentが未指定のとき400を返す" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      put "/api/tasks/#{task.id}", params: { title: TITLE_TEMP }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error10: 存在しないIDを指定したとき404を返す" do
      put "/api/tasks/#{NON_EXISTING_TASK_ID}", params: { title: TITLE_TEMP, isPermanent: false }.to_json, headers: json_headers

      assert_response :not_found
    end

    # ─────────────────────────────────────────────────────────────────
    # DELETE /api/tasks/:id
    # ─────────────────────────────────────────────────────────────────

    test "normal13: 存在するタスクを削除すると204を返す" do
      task = Task.create!(user: @user, role_id: @role.role_id, title: TITLE_TEMP, is_permanent: false)

      delete "/api/tasks/#{task.id}"

      assert_response :no_content
      assert_equal 0, Task.count
    end

    test "error11: 存在しないIDの削除で404を返す" do
      delete "/api/tasks/#{NON_EXISTING_TASK_ID}"

      assert_response :not_found
    end

    private

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
