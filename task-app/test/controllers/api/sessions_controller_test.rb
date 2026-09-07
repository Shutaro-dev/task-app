require "test_helper"

module Api
  class SessionsControllerTest < ActionDispatch::IntegrationTest
    EMAIL = "user@example.com"
    PASSWORD = "correct-password"

    setup do
      @user = User.create!(email: EMAIL, password: PASSWORD, name: "Test User")
    end

    # ─────────────────────────────────────────────────────────────────
    # POST /api/session (ログイン)
    # ─────────────────────────────────────────────────────────────────

    test "normal01: 正しいメールアドレスとパスワードでログインできる" do
      post "/api/session", params: { email: EMAIL, password: PASSWORD }, as: :json

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal @user.id, body["id"]
      assert_equal EMAIL, body["email"]
    end

    test "normal02: メールアドレスの大文字小文字を無視してログインできる" do
      post "/api/session", params: { email: EMAIL.upcase, password: PASSWORD }, as: :json

      assert_response :success
    end

    test "error01: パスワードが間違っている場合401を返す" do
      post "/api/session", params: { email: EMAIL, password: "wrong-password" }, as: :json

      assert_response :unauthorized
    end

    test "error02: 存在しないメールアドレスの場合401を返す" do
      post "/api/session", params: { email: "unknown@example.com", password: PASSWORD }, as: :json

      assert_response :unauthorized
    end

    # ─────────────────────────────────────────────────────────────────
    # GET /api/session (ログイン状態確認)
    # ─────────────────────────────────────────────────────────────────

    test "normal03: ログイン中はユーザー情報を返す" do
      post "/api/session", params: { email: EMAIL, password: PASSWORD }, as: :json

      get "/api/session"

      assert_response :success
      assert_equal EMAIL, JSON.parse(response.body)["email"]
    end

    test "error03: 未ログインの場合401を返す" do
      get "/api/session"

      assert_response :unauthorized
    end

    # ─────────────────────────────────────────────────────────────────
    # DELETE /api/session (ログアウト)
    # ─────────────────────────────────────────────────────────────────

    test "normal04: ログアウトすると204を返しセッションが破棄される" do
      post "/api/session", params: { email: EMAIL, password: PASSWORD }, as: :json

      delete "/api/session"

      assert_response :no_content

      get "/api/session"
      assert_response :unauthorized
    end
  end
end
