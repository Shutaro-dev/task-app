# Be sure to restart your server when you modify this file.

# Avoid CORS issues when API is called from the frontend app.
# Handle Cross-Origin Resource Sharing (CORS) in order to accept cross-origin Ajax requests.
#
# Spring Boot 版の WebConfig#addCorsMappings と同じ設定
# (すべてのエンドポイントに対して http://localhost:5173 からのアクセスを許可)

Rails.application.config.middleware.insert_before 0, Rack::Cors do
  allow do
    origins "http://localhost:5173"

    resource "*",
      headers: :any,
      methods: [:get, :post, :put, :delete, :options],
      credentials: true
  end
end
