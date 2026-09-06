require "test_helper"

module Api
  class RolesControllerTest < ActionDispatch::IntegrationTest
    ROLE_NAME_TEST     = "TestRole"
    ROLE_NAME_UPDATED  = "UpdatedRole"
    ROLE_NAME_ORIGINAL = "OriginalName"
    ROLE_NAME_JA       = "開発者"
    COLOR_BLUE         = "#4a90d9"
    COLOR_RED          = "#e74c3c"
    MAX_ROLE_NAME_LENGTH = 255
    NON_EXISTING_ROLE_ID = 9_999_999

    setup do
      Role.delete_all
    end

    # ─────────────────────────────────────────────────────────────────
    # POST /api/roles
    # ─────────────────────────────────────────────────────────────────

    test "normal01: roleIdなしで作成すると201を返し1件登録される" do
      post "/api/roles", params: { roleName: ROLE_NAME_TEST }.to_json, headers: json_headers

      assert_response :created
      assert_equal 1, Role.count
      assert_equal ROLE_NAME_TEST, Role.first.role_name
    end

    test "normal02: roleIdありで作成すると件数を増やさず名前だけ更新する" do
      role = Role.create!(role_name: ROLE_NAME_ORIGINAL)

      post "/api/roles", params: { roleId: role.role_id, roleName: ROLE_NAME_UPDATED }.to_json, headers: json_headers

      assert_response :created
      assert_equal 1, Role.count
      assert_equal ROLE_NAME_UPDATED, role.reload.role_name
    end

    test "normal03: colorを指定して作成するとDBに保存される" do
      post "/api/roles", params: { roleName: ROLE_NAME_TEST, color: COLOR_BLUE }.to_json, headers: json_headers

      assert_response :created
      assert_equal COLOR_BLUE, Role.first.color
    end

    test "normal04: 日本語ロール名でも作成できる" do
      post "/api/roles", params: { roleName: ROLE_NAME_JA }.to_json, headers: json_headers

      assert_response :created
      assert_equal ROLE_NAME_JA, Role.first.role_name
    end

    test "normal05: 最大長のroleNameでも作成できる" do
      max_length_name = "A" * MAX_ROLE_NAME_LENGTH
      post "/api/roles", params: { roleName: max_length_name }.to_json, headers: json_headers

      assert_response :created
      assert_equal max_length_name, Role.first.role_name
    end

    test "normal06: 新規作成時 is_expanded はDBデフォルトのtrueになる" do
      post "/api/roles", params: { roleName: ROLE_NAME_TEST }.to_json, headers: json_headers

      assert_response :created
      assert_equal true, Role.first.is_expanded
    end

    test "error01: roleNameが空文字のとき400を返しDBに登録されない" do
      post "/api/roles", params: { roleName: "" }.to_json, headers: json_headers

      assert_response :bad_request
      assert_equal 0, Role.count
    end

    test "error02: roleNameが未指定のとき400を返す" do
      post "/api/roles", params: { color: COLOR_BLUE }.to_json, headers: json_headers

      assert_response :bad_request
      assert_equal 0, Role.count
    end

    test "error03: リクエストボディなしのとき400を返す" do
      post "/api/roles", headers: json_headers

      assert_response :bad_request
    end

    test "error04: 存在しないroleIdを指定したとき404を返す" do
      post "/api/roles", params: { roleId: NON_EXISTING_ROLE_ID, roleName: ROLE_NAME_TEST }.to_json, headers: json_headers

      assert_response :not_found
    end

    test "error05: Content-Type未指定のとき415を返しDBに登録されない" do
      post "/api/roles", params: { roleName: ROLE_NAME_TEST }.to_json

      assert_response :unsupported_media_type
      assert_equal 0, Role.count
    end

    # ─────────────────────────────────────────────────────────────────
    # GET /api/roles
    # ─────────────────────────────────────────────────────────────────

    test "normal07: ロールが存在する場合200とtasks付きの一覧を返す" do
      role = Role.create!(role_name: ROLE_NAME_TEST, color: COLOR_BLUE)
      Task.create!(role_id: role.role_id, title: "Task 1", is_permanent: false)

      get "/api/roles"

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal role.role_id, body[0]["roleId"]
      assert_equal ROLE_NAME_TEST, body[0]["roleName"]
      assert_equal true, body[0]["isExpanded"]
      assert_equal COLOR_BLUE, body[0]["color"]
      assert_equal "Task 1", body[0]["tasks"][0]["title"]
    end

    test "normal08: ロールが0件の場合200と空配列を返す" do
      get "/api/roles"

      assert_response :success
      assert_equal [], JSON.parse(response.body)
    end

    test "normal09: sort_order, role_idの順でソートされる" do
      r1 = Role.create!(role_name: "Role1", sort_order: 2)
      r2 = Role.create!(role_name: "Role2", sort_order: 1)

      get "/api/roles"

      body = JSON.parse(response.body)
      assert_equal [r2.role_id, r1.role_id], body.map { |r| r["roleId"] }
    end

    # ─────────────────────────────────────────────────────────────────
    # PUT /api/roles/reorder
    # ─────────────────────────────────────────────────────────────────

    test "normal10: reorderでsort_orderがDBに反映される" do
      r1 = Role.create!(role_name: "Role1")
      r2 = Role.create!(role_name: "Role2")

      put "/api/roles/reorder", params: [
        { id: r2.role_id, sortOrder: 0 },
        { id: r1.role_id, sortOrder: 1 }
      ].to_json, headers: json_headers

      assert_response :success
      assert_equal 0, r2.reload.sort_order
      assert_equal 1, r1.reload.sort_order
    end

    test "normal11: 空配列のreorderでも200を返す" do
      put "/api/roles/reorder", params: [].to_json, headers: json_headers

      assert_response :success
    end

    # ─────────────────────────────────────────────────────────────────
    # PUT /api/roles/:id
    # ─────────────────────────────────────────────────────────────────

    test "normal12: roleNameを変更すると名前が更新される" do
      role = Role.create!(role_name: ROLE_NAME_ORIGINAL)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_UPDATED }.to_json, headers: json_headers

      assert_response :success
      assert_equal ROLE_NAME_UPDATED, role.reload.role_name
    end

    test "normal13: isExpandedをfalseに変更すると反映される" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_TEST, isExpanded: false }.to_json, headers: json_headers

      assert_response :success
      assert_equal false, role.reload.is_expanded
    end

    test "normal14: isExpandedを省略すると既存の値が保持される" do
      role = Role.create!(role_name: ROLE_NAME_ORIGINAL, is_expanded: true)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_UPDATED }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal true, body["isExpanded"]
      assert_equal true, role.reload.is_expanded
    end

    test "normal15: colorを変更するとDBに反映されレスポンスに含まれる" do
      role = Role.create!(role_name: ROLE_NAME_TEST, color: COLOR_BLUE)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_TEST, color: COLOR_RED }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal COLOR_RED, body["color"]
      assert_equal COLOR_RED, role.reload.color
    end

    test "normal16: 更新後のレスポンスに紐づくタスクが含まれる" do
      role = Role.create!(role_name: ROLE_NAME_ORIGINAL)
      Task.create!(role_id: role.role_id, title: "Task A", is_permanent: false)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_UPDATED, isExpanded: true }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal 1, body["tasks"].size
      assert_equal "Task A", body["tasks"][0]["title"]
    end

    test "error06: roleNameが空文字のとき400を返す" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      put "/api/roles/#{role.role_id}", params: { roleName: "", isExpanded: true }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error07: roleNameが未指定のとき400を返す" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      put "/api/roles/#{role.role_id}", params: { isExpanded: true }.to_json, headers: json_headers

      assert_response :bad_request
    end

    test "error08: 存在しないIDを指定したとき404を返す" do
      put "/api/roles/#{NON_EXISTING_ROLE_ID}", params: { roleName: ROLE_NAME_UPDATED, isExpanded: true }.to_json, headers: json_headers

      assert_response :not_found
    end

    test "error09: リクエストボディなしのとき400を返す" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      put "/api/roles/#{role.role_id}", headers: json_headers

      assert_response :bad_request
    end

    test "error10: Content-Type未指定のとき415を返す" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      put "/api/roles/#{role.role_id}", params: { roleName: ROLE_NAME_UPDATED, isExpanded: true }.to_json

      assert_response :unsupported_media_type
    end

    # ─────────────────────────────────────────────────────────────────
    # DELETE /api/roles/:id
    # ─────────────────────────────────────────────────────────────────

    test "normal17: 存在するロールを削除すると204を返しDBから消える" do
      role = Role.create!(role_name: ROLE_NAME_TEST)

      delete "/api/roles/#{role.role_id}"

      assert_response :no_content
      assert_equal 0, Role.count
    end

    test "normal18: ロール削除時に紐づくタスクもCASCADE削除される" do
      role = Role.create!(role_name: ROLE_NAME_TEST)
      Task.create!(role_id: role.role_id, title: "Task 1", is_permanent: false)
      Task.create!(role_id: role.role_id, title: "Task 2", is_permanent: false)

      delete "/api/roles/#{role.role_id}"

      assert_response :no_content
      assert_equal 0, Task.count
    end

    test "error11: 存在しないIDの削除で404を返す" do
      delete "/api/roles/#{NON_EXISTING_ROLE_ID}"

      assert_response :not_found
    end

    private

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
