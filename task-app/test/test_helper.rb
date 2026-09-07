ENV["RAILS_ENV"] ||= "test"
require_relative "../config/environment"
require "rails/test_help"

module ActiveSupport
  class TestCase
    # JUnit版のテストは単一スレッドで @BeforeEach ごとにテーブルをクリアしていたため、
    # それと対応させるためここでも並列実行はしない
    # (Rails のトランザクショナルテストにより各テストは自動的にロールバックされる)

    # Add more helper methods to be used by all tests here...

    TEST_USER_PASSWORD = "password12345"

    # roles/tasks コントローラーは認証必須なので、統合テストで実際に
    # /api/session へログインしてセッション Cookie を確立する
    def create_and_sign_in_user(email: "tester@example.com")
      user = User.create!(email: email, password: TEST_USER_PASSWORD)
      post "/api/session", params: { email: user.email, password: TEST_USER_PASSWORD }, as: :json
      user
    end
  end
end
