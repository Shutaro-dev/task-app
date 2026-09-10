require "test_helper"

module Api
  class DayNotesControllerTest < ActionDispatch::IntegrationTest
    WEEK_START = "2026-09-07"

    setup do
      @user = create_and_sign_in_user
    end

    test "normal01: notesを更新するとweek_data/day_noteが自動作成される" do
      put "/api/week_data/#{WEEK_START}/day_notes/1", params: { notes: "よく眠れた" }.to_json, headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal 1, body["day"]
      assert_equal "よく眠れた", body["notes"]
      assert_nil body["sleepStart"]
    end

    test "normal02: sleepStart/sleepEndを更新できる" do
      put "/api/week_data/#{WEEK_START}/day_notes/2",
        params: { sleepStart: "23:00", sleepEnd: "07:00" }.to_json,
        headers: json_headers

      assert_response :success
      body = JSON.parse(response.body)
      assert_equal "23:00", body["sleepStart"]
      assert_equal "07:00", body["sleepEnd"]
    end

    test "normal03: 同じ日に2回PUTしても1件のまま更新される" do
      put "/api/week_data/#{WEEK_START}/day_notes/3", params: { notes: "1回目" }.to_json, headers: json_headers
      put "/api/week_data/#{WEEK_START}/day_notes/3", params: { notes: "2回目" }.to_json, headers: json_headers

      assert_response :success
      week_data = WeekData.find_by(user: @user, week_start: WEEK_START)
      assert_equal 1, week_data.day_notes.where(day: 3).count
      assert_equal "2回目", JSON.parse(response.body)["notes"]
    end

    test "error01: dayが範囲外(7)のとき404を返す" do
      put "/api/week_data/#{WEEK_START}/day_notes/7", params: { notes: "x" }.to_json, headers: json_headers

      assert_response :not_found
    end

    private

    def json_headers
      { "CONTENT_TYPE" => "application/json" }
    end
  end
end
