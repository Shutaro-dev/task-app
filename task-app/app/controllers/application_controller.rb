class ApplicationController < ActionController::API
  rescue_from ActiveRecord::RecordNotFound, with: :render_not_found
  rescue_from ActionDispatch::Http::Parameters::ParseError, with: :render_bad_request

  private

  # @RequestBody を使う Spring のエンドポイントは Content-Type 未指定/不正だと 415 を返す。
  # 同じ挙動にするため、JSON ボディを要求するアクションでは明示的にチェックする。
  def require_json_content_type
    return if request.content_type&.start_with?("application/json")

    render json: { error: "Content-Type must be application/json" }, status: :unsupported_media_type
  end

  def render_not_found(exception)
    render json: { error: exception.message }, status: :not_found
  end

  def render_bad_request(_exception)
    render json: { error: "Malformed JSON request body" }, status: :bad_request
  end
end
