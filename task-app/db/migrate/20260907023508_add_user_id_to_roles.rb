class AddUserIdToRoles < ActiveRecord::Migration[8.1]
  def change
    # 既存データ(seed で作った Role)は user_id なしのまま残る。
    # ログイン導入後に current_user でスコープするため列自体は必須にするが、
    # 移行済みの開発 DB を壊さないよう NOT NULL 制約は付けない
    # (reset-db.sh で作り直すか、後続の対応で紐付ける想定)。
    add_reference :roles, :user, null: true, foreign_key: true, index: true
  end
end
