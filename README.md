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

## 設置選項

- 可選擇是否將 java.util.Date 轉換為 string
- 可選擇是否使用 @JsonProperty 註解的值作為字段名
- 可選擇是否在全局範圍內查找類
- 可選擇是否忽略父類字段
