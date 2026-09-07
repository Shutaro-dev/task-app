module Api
  class UsersController < ApplicationController
    before_action :require_json_content_type, only: [:create]

    # サインアップ。成功したらそのままログイン状態にする
    def create
      user = User.new(
        email: params[:email],
        password: params[:password],
        password_confirmation: params[:passwordConfirmation],
        name: params[:name]
      )

      if user.save
        reset_session
        session[:user_id] = user.id
        render json: { id: user.id, email: user.email, name: user.name }, status: :created
      else
        render json: { error: user.errors.full_messages.join(", ") }, status: :unprocessable_entity
      end
    end
  end
end
