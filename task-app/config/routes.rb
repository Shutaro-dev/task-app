Rails.application.routes.draw do
  # Reveal health status on /up that returns 200 if the app boots with no exceptions, otherwise 500.
  # Can be used by load balancers and uptime monitors to verify that the app is live.
  get "up" => "rails/health#show", as: :rails_health_check

  namespace :api do
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
  end
end
