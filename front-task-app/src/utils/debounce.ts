// スケジュール済みタスクのドラッグ/リサイズ(mousemoveのたびに呼ばれる)や
// メモ欄のキー入力のように高頻度で発火する更新を、キー(タスクIDや日付など)ごとに
// まとめて1回のAPIコールへ間引くための小さなユーティリティ。
// ローカルstateは呼び出し側で即時更新し、これはサーバーへの保存だけを遅延させる。
export class KeyedDebouncer<T extends object> {
  private timers = new Map<string, ReturnType<typeof setTimeout>>();
  private pending = new Map<string, T>();

  constructor(
    private readonly save: (key: string, value: T) => void,
    private readonly delayMs = 500
  ) {}

  // 同じキーに対する連続呼び出しはマージする(例: リサイズ中にstartTimeとdurationが
  // 別々のタイミングで飛んでくる場合でも、直近の値を両方まとめて1回のPUTにする)
  schedule(key: string, value: T): void {
    this.pending.set(key, { ...(this.pending.get(key) as T | undefined), ...value });
    const existing = this.timers.get(key);
    if (existing) clearTimeout(existing);
    this.timers.set(
      key,
      setTimeout(() => this.flushKey(key), this.delayMs)
    );
  }

  private flushKey(key: string): void {
    this.timers.delete(key);
    const value = this.pending.get(key);
    if (value === undefined) return;
    this.pending.delete(key);
    this.save(key, value);
  }

  // 週切り替え・ログアウトなど、保留中の書き込みを即座に確定させたいときに呼ぶ
  flushAll(): void {
    for (const key of Array.from(this.timers.keys())) {
      clearTimeout(this.timers.get(key));
      this.flushKey(key);
    }
  }

  // サーバー未反映のまま削除された等、保留中の書き込みを送らずに捨てたいときに呼ぶ
  cancel(key: string): void {
    const existing = this.timers.get(key);
    if (existing) clearTimeout(existing);
    this.timers.delete(key);
    this.pending.delete(key);
  }

  // 楽観的作成のID差し替え(仮ID→サーバー採番ID)に追従して保留中の書き込みを引き継ぐ
  rekey(oldKey: string, newKey: string): void {
    const existing = this.timers.get(oldKey);
    const value = this.pending.get(oldKey);
    if (existing) clearTimeout(existing);
    this.timers.delete(oldKey);
    this.pending.delete(oldKey);
    if (value !== undefined) this.schedule(newKey, value);
  }
}
