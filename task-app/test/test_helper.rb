ENV["RAILS_ENV"] ||= "test"
require_relative "../config/environment"
require "rails/test_help"

# sharpen_the_saw_areas はマイグレーション(FixSharpenTheSawAreasSeedData)のupメソッドで
# 投入しているデータだが、`bin/rails test` が使う db:test:prepare は schema.rb からの
# 構造ロードのみでデータ移行の中身は実行しないため、ここで明示的に投入する。
# トランザクショナルテストのロールバック対象外(テスト実行プロセス起動時に1度だけ、
# 通常のテストのトランザクションの外側で)に行うことで、全テストから恒常的に参照できる。
[
  { id: "physical", name: "Physical", icon: "💪" },
  { id: "mental", name: "Intellectual", icon: "🧠" },
  { id: "social-emotional", name: "Social/Emotional", icon: "❤️" },
  { id: "spiritual", name: "Spiritual", icon: "🙏" }
].each do |attrs|
  SharpenTheSawArea.find_or_create_by!(id: attrs[:id]) do |area|
    area.name = attrs[:name]
    area.icon = attrs[:icon]
  end
end

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
