# 設置說明

要完成 IntelliJ IDEA 插件項目的設置，您需要獲取 gradle-wrapper.jar 文件。此文件是必要的，因為它允許您在不需要安裝 Gradle 的情況下構建項目。

## 獲取 gradle-wrapper.jar

請執行以下步驟之一：

### 選項 1：使用 Gradle 初始化 Wrapper（推薦）

如果您的系統已經安裝了 Gradle，可以運行以下命令：

```bash
gradle wrapper --gradle-version 8.5
```

這將自動創建所需的 gradle-wrapper.jar 文件。

### 選項 2：從其他 Gradle 項目複製

1. 從另一個 Gradle 項目找到 `gradle/wrapper/gradle-wrapper.jar` 文件
2. 複製此文件到您項目的 `gradle/wrapper/` 目錄中

### 選項 3：下載 Gradle 發行版

1. 訪問 [Gradle 發行頁面](https://gradle.org/releases/)
2. 下載 Gradle 8.5
3. 解壓縮下載的文件
4. 從解壓縮的目錄中找到 `gradle/wrapper/gradle-wrapper.jar` 並複製到您項目的相同位置

## 完成設置後

一旦您獲得了 gradle-wrapper.jar，您就可以使用 Gradle Wrapper 運行構建：

```bash
./gradlew build
```

## 在 IntelliJ IDEA 中開發

1. 在 IntelliJ IDEA 中打開項目
2. 它應該自動識別為 Gradle 項目
3. 等待 Gradle 同步完成
4. 然後您可以運行 Gradle 任務構建和測試插件
