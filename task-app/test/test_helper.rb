ENV["RAILS_ENV"] ||= "test"
require_relative "../config/environment"
require "rails/test_help"

module ActiveSupport
  class TestCase
    # JUnit版のテストは単一スレッドで @BeforeEach ごとにテーブルをクリアしていたため、
    # それと対応させるためここでも並列実行はしない
    # (Rails のトランザクショナルテストにより各テストは自動的にロールバックされる)

    # Add more helper methods to be used by all tests here...
  end
end
