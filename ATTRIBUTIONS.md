# 第三方源與授權資訊

本文件列出了本專案中使用的第三方源代碼和相關授權資訊。

## 專案基礎

本專案基於 TheFreeOne 的 Java-Bean-To-Ts-Interface 專案進行修改和增強。

- **原始專案**: [Java-Bean-To-Ts-Interface](https://github.com/TheFreeOne/Java-Bean-To-Ts-Interface)
- **原始作者**: TheFreeOne
- **授權**: MIT
- **來源日期**: 2022
- **修改者**: AudiChuang
- **修改日期**: 2024

## 功能增強與修改

在原始專案的基礎上，我們進行了以下增強與修改：

1. 添加了介面排序功能，優先顯示請求類及其依賴
2. 過濾標準庫類型，避免顯示非業務相關的類型
3. 使用 any 類型替代無法識別的類型
4. 增加未知類型的警告提示
5. 增強依賴關係檢測
6. 改進用戶介面與提示訊息

## 授權聲明

本專案沿用了原始專案的 MIT 許可證。根據 MIT 許可證的條款，使用者必須在所有副本或重要部分中保留原始版權和許可聲明。以下是完整的許可證文本：

```
MIT License

Copyright (c) 2022 TheFreeOne
Copyright (c) 2024 AudiChuang (modifications)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 如何使用與引用本專案

如果您使用或修改本專案，請確保：

1. 保留原始的版權聲明（包括 TheFreeOne 和 AudiChuang 的版權資訊）
2. 保留完整的 MIT 許可證文本
3. 在您的文檔中適當引用原始專案和修改者的貢獻

## 如何貢獻

如果您對本專案有任何建議或發現問題，歡迎提交 Issue 或 Pull Request。請確保您的貢獻也符合 MIT 許可證的條款。
