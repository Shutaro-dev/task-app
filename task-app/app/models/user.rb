class User < ApplicationRecord
  has_secure_password

  has_many :roles, dependent: :destroy
  has_many :tasks, dependent: :destroy

  before_validation { email&.downcase!; email&.strip! }

  validates :email, presence: true, uniqueness: { case_sensitive: false },
                     format: { with: URI::MailTo::EMAIL_REGEXP }
  validates :password, length: { minimum: 8 }, allow_nil: true
end
