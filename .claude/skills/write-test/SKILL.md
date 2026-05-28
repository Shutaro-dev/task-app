---
name: write-test
description: テストを書く・追加する・拡充するときに使う。「〜のテストを書いて」「テストケースを追加して」「テストが足りない」など、テスト作成が求められる文脈で自動的にロードする。Spring Boot の Service（@MybatisTest）または Controller（@WebMvcTest）を対象とし、品質チェックリストに基づいて正常系・異常系の両方を書く。
allowed-tools: Read Grep Bash(./gradlew test)
---

# テスト作成

対象クラス・メソッドのテストを作成または拡充する。

## 使い方

```
/write-test <対象クラス名 or ファイルパス>
```

引数がない場合は現在開いているファイル、または直前の会話の対象クラスを使う。

---

## 実行手順

### 1. 対象を把握する

- 対象クラスの実装・既存テストを読む
- テスト対象の公開メソッド一覧と、各メソッドの責務を把握する
- 既存テストと重複するケースは追加しない

### 2. テストの種別を判断する

| 対象 | 使うアノテーション | DB |
|---|---|---|
| Service | `@MybatisTest` + `@Import({XxxService.class})` | H2（実DB相当） |
| Controller | `@WebMvcTest(XxxController.class)` + `@MockitoBean` | なし（Serviceをモック） |

- Service は実 Mapper を使い DB の振る舞いを検証する（Mapper 自体はモックしない）
- Controller は HTTP 層のみ検証する（Service は必ずモック）
- `@SpringBootTest` は統合テストが明示的に必要な場合のみ使う

### 3. テストケースを列挙する

各メソッドについて以下を網羅する：

**正常系**
- 基本的な成功ケース
- 境界値（空リスト/0件/最大長/最小値）
- オプションフィールドが null のケース
- 日本語・マルチバイト文字

**異常系**
- 必須フィールドが null / 空文字
- 存在しない ID を指定
- 重複データ / DB 制約違反
- 権限なし（将来的に認証が入る場合）

### 4. テストを書く

#### 命名規則

```java
// DisplayName: "normalNN: 〜するとXXになる" / "errorNN: 〜のときXXが発生する"
@DisplayName("normal01: roleNameなしで作成すると1件登録される")
void createRole_normal01() { ... }

@DisplayName("error01: roleNameがnullのときDB制約違反で例外が発生する")
void createRole_error01() { ... }
```

- メソッド名は `対象メソッド_normalNN` / `対象メソッド_errorNN`
- normal / error の通番はクラス全体で連番にする

#### 定数管理

```java
// テストデータはすべてクラス先頭で定数化する
private static final String ROLE_NAME_TEST    = "TestRole";
private static final String COLOR_BLUE        = "#4a90d9";
private static final int    NON_EXISTING_ID   = 9999;
private static final int    MAX_NAME_LENGTH   = 255;
```

#### AAA 構造

```java
void someTest() {
    // Arrange
    roleService.createRole(new RoleDto(null, ROLE_NAME_TEST, null));
    Integer roleId = roleMapper.selectAll().getFirst().getRoleId();

    // Act
    roleService.deleteRole(roleId);

    // Assert
    assertThat(roleMapper.selectAll()).isEmpty();
}
```

#### アサーション

- `assertThat(actual).isEqualTo(expected)` — AssertJ を使う
- `assertThatThrownBy(() -> ...).isInstanceOf(XxxException.class)` — 例外検証
- Controller テストは `jsonPath` でレスポンス構造まで検証する
- 「例外なく完了すればOK」のケースはコメントで意図を明示する

#### セクション区切り

```java
// ─────────────────────────────────────────────────────────────────
// methodName
// ─────────────────────────────────────────────────────────────────
```

### 5. 品質チェックリスト（作成後に自己レビューする）

- [ ] テストの目的が明確か（DisplayName が「何を」「どんな条件で」「どうなるか」を示しているか）
- [ ] 1テスト1責務になっているか
- [ ] Arrange / Act / Assert が整理されているか
- [ ] 正常系と異常系の両方があるか（null / 空値 / 存在しないID / 0件 / 境界値 を考慮したか）
- [ ] 振る舞いをテストしているか（private メソッドを直接テストしていない / 内部処理の順を verify していない）
- [ ] Mock の使い方が適切か（Service テストで Mapper をモックしていない / Controller テストで Service をモックしている）
- [ ] テストが独立しているか（`@BeforeEach` で DB をクリアしている / テスト間でデータを共有していない）
- [ ] `@SpringBootTest` を不要に使っていないか
- [ ] 失敗時に原因が分かるか（jsonPath / assertThat の期待値が具体的か）
- [ ] 実装をリファクタリングしても壊れないか（実装の順序ではなく結果を検証しているか）
- [ ] テストデータが定数化されているか（マジックナンバー・マジック文字列を直書きしていないか）
- [ ] テストが仕様を表現しているか（DisplayName を読めばそのメソッドの仕様が分かるか）

### 6. テストを実行して確認する

```bash
cd task-app && ./gradlew test
```

失敗した場合は原因を特定して修正する。レポートは `build/reports/tests/test/index.html` で確認できる。
