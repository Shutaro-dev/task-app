module Api
  class SessionsController < ApplicationController
    before_action :require_json_content_type, only: [:create]
    before_action :authenticate_user!, only: [:show]

    # ログイン中かどうかの確認 (画面ロード時に呼ぶ)
    def show
      render json: user_json(current_user)
    end

    # ログイン
    def create
      user = User.find_by("lower(email) = ?", params[:email].to_s.downcase)
      if user&.authenticate(params[:password].to_s)
        reset_session
        session[:user_id] = user.id
        render json: user_json(user)
      else
        render json: { error: "メールアドレスまたはパスワードが正しくありません" }, status: :unauthorized
      end
    end

    # ログアウト
    def destroy
      reset_session
      head :no_content
    end

    private

    def user_json(user)
      { id: user.id, email: user.email, name: user.name }
    end
  end
end
