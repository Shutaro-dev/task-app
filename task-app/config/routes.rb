Rails.application.routes.draw do
  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
  # Can be used by load balancers and uptime monitors to verify that the app is live.
  get "up" => "rails/health#show", as: :rails_health_check

  namespace :api do
    resources :users, only: [:create]
    resource :session, only: [:show, :create, :destroy]

    resources :roles, only: [:index, :create, :update, :destroy] do
      collection do
        put :reorder
      end
    end

    resources :tasks, only: [:index, :create, :update, :destroy] do
      collection do
        put :reorder
      end
    end

    get "mission_statement", to: "mission_statement#show"
    put "mission_statement", to: "mission_statement#update"

    get "sharpen_the_saw_areas", to: "sharpen_the_saw_areas#index"
    put "sharpen_the_saw_areas", to: "sharpen_the_saw_areas#update"

    week_start_constraint = { week_start: /\d{4}-\d{2}-\d{2}/ }

    get "week_data/:week_start", to: "week_data#show", constraints: week_start_constraint
    put "week_data/:week_start", to: "week_data#update", constraints: week_start_constraint

    post "week_data/:week_start/scheduled_tasks", to: "scheduled_tasks#create", constraints: week_start_constraint
    put "scheduled_tasks/:id", to: "scheduled_tasks#update"
    delete "scheduled_tasks/:id", to: "scheduled_tasks#destroy"

    put "week_data/:week_start/day_notes/:day", to: "day_notes#update",
      constraints: week_start_constraint.merge(day: /[0-6]/)
  end
end
