module Api
  class MissionStatementController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:update]

    def show
      render json: { missionStatement: current_user.mission_statement || "" }
    end

    def update
      current_user.update!(mission_statement: params[:missionStatement].to_s)
      render json: { missionStatement: current_user.mission_statement || "" }
    end
  end
end
