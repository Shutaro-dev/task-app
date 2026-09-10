module Api
  class DayNotesController < ApplicationController
    before_action :authenticate_user!
    before_action :require_json_content_type, only: [:update]

    # notes(updateDayNotes)とsleepStart/sleepEnd(updateSleepTime)の両方をこの1本でカバーする
    def update
      week_data = current_user.week_data.find_or_create_by!(week_start: params[:week_start])
      day_note = week_data.day_notes.find_or_create_by!(day: params[:day])

      attrs = {}
      attrs[:notes] = params[:notes] if params.key?(:notes)
      attrs[:sleep_start] = params[:sleepStart] if params.key?(:sleepStart)
      attrs[:sleep_end] = params[:sleepEnd] if params.key?(:sleepEnd)
      day_note.update!(attrs)

      render json: day_note_json(day_note)
    end

    private

    def day_note_json(day_note)
      {
        day: day_note.day,
        notes: day_note.notes || "",
        sleepStart: day_note.sleep_start&.strftime("%H:%M"),
        sleepEnd: day_note.sleep_end&.strftime("%H:%M"),
      }
    end
  end
end
