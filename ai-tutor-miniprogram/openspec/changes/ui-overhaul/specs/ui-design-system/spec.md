## 新增需求 (ADDED Requirements)

### 需求：全局配色方案 (Global Color Palette)
应用必须在整个 UI 中使用以下颜色变量，以确保与 Web 平台的一致性：
-   `--primary`: #00bebd (青色)
-   `--text`: #1f2329 (深灰色)
-   `--bg`: #f6f7fb (浅灰背景)
-   `--card`: #ffffff (白色卡片背景)
-   `--border`: rgba(31, 35, 41, 0.12)
-   `--muted`: #646a73 (静音文本)

#### 场景：应用主色 (Apply Primary Color)
-   **当** 渲染主按钮时
-   **则** 它的背景颜色必须是 `#00bebd`

#### 场景：应用背景色 (Apply Background Color)
-   **当** 渲染页面时
-   **则** 主背景颜色必须是 `#f6f7fb`

### 需求：底部导航栏样式 (Bottom Navigation Bar Style)
底部标签栏必须配置为匹配应用的主题：
-   选中项颜色：`#00bebd`
-   未选中项颜色：`#646a73`
-   背景颜色：`#ffffff`
-   边框样式：`black` (默认细线)

#### 场景：导航栏渲染 (Navigation Bar Rendering)
-   **当** 应用加载时
-   **则** 底部标签栏必须显示配置的颜色
