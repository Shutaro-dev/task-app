// オンボーディングツアーの各ステップ定義。
// target は各コンポーネントに付与した data-tour 属性のセレクタ値(null = 中央に表示する導入/終了ステップ)。
export interface TourStep {
  id: string;
  target: string | null;
  placement: 'top' | 'bottom' | 'left' | 'right' | 'center';
  // 'bottom'/'top' でターゲットの矩形のどちらの辺を基準に配置するか(未指定時は placement と同じ辺)。
  // calendar-grid のように縦に長い要素の近くに吹き出しを出したい場合に使う。
  anchor?: 'start' | 'end';
  title: string;
  body: string;
}

export const ONBOARDING_STEPS: TourStep[] = [
  {
    id: 'welcome',
    target: null,
    placement: 'center',
    title: 'ようこそ、WeeklyCompassへ',
    body: '「7つの習慣」の考え方をもとに、役割ごとにタスクを管理し、週次カレンダーに配置していくタイムマネジメントアプリです。主な機能を1分ほどでご案内します。',
  },
  {
    id: 'roles',
    target: 'roles-section',
    placement: 'right',
    title: '役割(ロール)とタスク',
    body: '「Professional」「Family」のように、人生の役割ごとにタスクをまとめます。P は週をまたいで続く永続タスク、T はその週限りの一時タスクです。',
  },
  {
    id: 'sharpen',
    target: 'sharpen-summary',
    placement: 'right',
    title: 'Sharpen the Saw',
    body: 'Physical・Social/Emotional・Spiritual・Intellectual の4領域で、自分を磨く活動を管理します。歯車アイコンから内容を設定できます。',
  },
  {
    id: 'calendar',
    target: 'calendar-grid',
    placement: 'bottom',
    anchor: 'start',
    title: 'ドラッグ&ドロップでスケジュール',
    body: '左のタスクをカレンダーの時間帯にドラッグすると、その場でスケジュールに配置されます。端をドラッグすれば所要時間も調整できます。',
  },
  {
    id: 'list-mode',
    target: 'list-mode-toggle',
    placement: 'bottom',
    title: 'リスト表示',
    body: '時間軸を気にせずチェックリストのようにタスクをこなしたいときは、ここで表示を切り替えられます。',
  },
  {
    id: 'mission',
    target: 'mission-summary',
    placement: 'left',
    title: 'ミッションステートメント',
    body: '自分の指針となる言葉を設定しておくと、右サイドバーにいつでも表示されます。週次メモとあわせて振り返りに使ってください。',
  },
  {
    id: 'done',
    target: null,
    placement: 'center',
    title: '準備は完了です',
    body: 'この案内はいつでもアカウント欄のアイコンから見返せます。さっそく最初の役割を追加してみましょう。',
  },
];
