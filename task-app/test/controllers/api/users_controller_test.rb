require "test_helper"

module Api
  class UsersControllerTest < ActionDispatch::IntegrationTest
    EMAIL = "newuser@example.com"
    PASSWORD = "password12345"

    # ─────────────────────────────────────────────────────────────────
    # POST /api/users (サインアップ)
    # ─────────────────────────────────────────────────────────────────

    test "normal01: 有効な情報でサインアップすると201を返しユーザーが作成される" do
      post "/api/users", params: {
        email: EMAIL, password: PASSWORD, passwordConfirmation: PASSWORD, name: "New User"
      }, as: :json

      assert_response :created
      assert_equal 1, User.count
      body = JSON.parse(response.body)
      assert_equal EMAIL, body["email"]
    end

    test "normal02: サインアップ後はログイン状態になる" do
      post "/api/users", params: {
        email: EMAIL, password: PASSWORD, passwordConfirmation: PASSWORD
      }, as: :json

      get "/api/session"

      assert_response :success
      assert_equal EMAIL, JSON.parse(response.body)["email"]
    end

    test "normal03: メールアドレスは小文字に正規化されて保存される" do
      post "/api/users", params: {
        email: "Mixed@Example.com", password: PASSWORD, passwordConfirmation: PASSWORD
      }, as: :json

      assert_response :created
      assert_equal "mixed@example.com", User.first.email
    end

    test "error01: メールアドレスが不正な形式のとき422を返す" do
      post "/api/users", params: {
        email: "not-an-email", password: PASSWORD, passwordConfirmation: PASSWORD
      }, as: :json

      assert_response :unprocessable_entity
      assert_equal 0, User.count
    end

    test "error02: パスワードが短すぎるとき422を返す" do
      post "/api/users", params: {
        email: EMAIL, password: "short", passwordConfirmation: "short"
      }, as: :json

      assert_response :unprocessable_entity
      assert_equal 0, User.count
    end

    test "error03: パスワード確認が一致しないとき422を返す" do
      post "/api/users", params: {
        email: EMAIL, password: PASSWORD, passwordConfirmation: "different-password"
      }, as: :json

      assert_response :unprocessable_entity
      assert_equal 0, User.count
    end

    test "error04: メールアドレスが重複しているとき422を返す" do
      User.create!(email: EMAIL, password: PASSWORD)

      post "/api/users", params: {
        email: EMAIL, password: PASSWORD, passwordConfirmation: PASSWORD
      }, as: :json

      assert_response :unprocessable_entity
      assert_equal 1, User.count
    end

    test "error05: メールアドレスが大文字小文字違いで重複しているとき422を返す" do
      User.create!(email: EMAIL, password: PASSWORD)

      post "/api/users", params: {
        email: EMAIL.upcase, password: PASSWORD, passwordConfirmation: PASSWORD
      }, as: :json

      assert_response :unprocessable_entity
      assert_equal 1, User.count
    end
  end
end
