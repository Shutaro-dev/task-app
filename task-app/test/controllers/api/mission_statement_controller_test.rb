require "test_helper"

module Api
  class MissionStatementControllerTest < ActionDispatch::IntegrationTest
    STATEMENT = "家族との時間を大切にしながら学び続ける。"

    setup do
      @user = create_and_sign_in_user
    end

    test "normal01: 未設定のとき空文字を返す" do
      get "/api/mission_statement"

      assert_response :success
      assert_equal "", JSON.parse(response.body)["missionStatement"]
    end

    test "normal02: 更新すると反映される" do
      put "/api/mission_statement", params: { missionStatement: STATEMENT }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal STATEMENT, body["missionStatement"]
      assert_equal STATEMENT, @user.reload.mission_statement
    end

    test "normal03: 空文字に更新できる" do
      @user.update!(mission_statement: STATEMENT)

      put "/api/mission_statement", params: { missionStatement: "" }.to_json, headers: json_headers

      assert_response :success
      assert_equal "", @user.reload.mission_statement
    end

    test "error01: 未ログインのとき401を返す" do
      delete "/api/session"

      get "/api/mission_statement"

      assert_response :unauthorized
    end

    private

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
