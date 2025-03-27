# Java-DTO-To-Ts-Interface

一個 IntelliJ IDEA 插件 - 將 Java DTO 轉換為 TypeScript 接口

在 Java DTO 文件上右鍵點擊，選擇「Java DTO to TypeScript interface」，選擇保存路徑，然後一個以'.d.ts'結尾的聲明文件將被保存到該文件夾。

例如：
右鍵點擊 TestRequest.java，然後點擊「Java DTO to TypeScript interface」

```java
public class TestRequest {
    /**
     * name list
     */
    @NotNull
    private String nameArray[];
    private List<String> names;

    private Boolean isRunning;

    private boolean isSuccess;

}
```

結果 => TestRequest.d.ts

```typescript
export default interface TestRequest {
  /**
   * name list
   */
  nameArray: string[];

  names?: string[];

  isRunning?: boolean;

  isSuccess?: boolean;
}
```

## 主要功能

- 將 Java DTO 類轉換為 TypeScript 接口
- 支持 Java 類註釋和字段註釋
- 自動將 Java 類型映射為 TypeScript 類型
- 支持將結果複製到剪貼板
- 支持在編輯器中查看結果
- 支持保存為 .d.ts 文件
- 支持 @JsonProperty 註解
- 支持基本的泛型處理
- 支持父類字段繼承
- 支持對生成的 TypeScript 接口進行智能排序
- 過濾標準庫類型，使用 any 替代
- 新增未知類型警告提示

## 設置選項

- 可選擇是否將 java.util.Date 轉換為 string
- 可選擇是否使用 @JsonProperty 註解的值作為字段名
- 可選擇是否在全局範圍內查找類
- 可選擇是否忽略父類字段

## 鳴謝與版權

本專案基於 [TheFreeOne](https://github.com/TheFreeOne/Java-Bean-To-Ts-Interface) 的開源專案修改而來，特此感謝原作者的貢獻。

本專案根據 MIT 許可證發布，保留原始版權聲明。使用本專案時，請遵守 MIT 許可證的條款，並在您的專案中適當引用和鳴謝原始作者。

```
MIT License

Copyright (c) 2022 TheFreeOne
Copyright (c) 2024 AudiChuang (modifications)
```

完整的許可證文件和第三方源資訊可在 [LICENSE](LICENSE) 和 [ATTRIBUTIONS.md](ATTRIBUTIONS.md) 檔案中查閱。

如果您對本專案有任何建議或發現問題，歡迎提交 Issue 或 Pull Request。
