# Weather

一个基于 Android 的天气助手应用，支持实时天气、7 日预报、城市管理、穿衣建议、AI 对话和地图选城。

## 项目简介

Weather 面向日常出行场景，核心目标是把“看天气、选城市、决定怎么穿、顺手问一句 AI”放进一个轻量但完整的应用里。

当前版本已经实现：

- 启动时自动定位并获取当前城市天气
- 已关注城市管理、搜索、删除、切换
- 百度地图选城，支持当前位置回中与中心点选城
- 实时天气、7 日预报、天气指数与穿衣建议
- 按天气切换的动态主题背景
- AI 天气助手对话面板

## 技术栈

- 语言：Java
- 最低版本：Android 7.0（API 24）
- 目标版本：Android 16（API 36）
- 构建：Gradle Kotlin DSL
- 网络：Retrofit + OkHttp + Gson
- 数据库：Room
- 图片：Coil
- 地图：百度地图 Android SDK
- 天气：和风天气 API
- AI：阿里云百炼兼容接口

## 本地配置

在 `local.properties` 中补充以下配置：

```properties
QWEATHER_API_KEY=你的和风天气Key
QWEATHER_API_HOST=你的和风天气API Host
ALIYUN_BAILIAN_API_KEY=你的百炼API Key
ALIYUN_BAILIAN_APP_ID=你的百炼应用ID
ALIYUN_BAILIAN_BASE_URL=https://dashscope.aliyuncs.com
ALIYUN_BAILIAN_MODEL=qwen-plus
BAIDU_MAP_AK=你的百度地图AK
```

## 运行方式

1. 使用 Android Studio 打开项目目录 `D:\Android\project\Weather`
2. 配置 `local.properties`
3. 连接真机或启动模拟器
4. 执行 `assembleDebug` 或直接运行 `app`

## 主要页面

### 首页

- 当前城市天气
- 实时温度、天气图标、状态说明
- 体感、湿度、风力、降水、气压、能见度、云量
- 穿衣建议卡片
- 未来 7 天预报
- AI 助手入口

### 城市管理页

- 搜索城市并添加
- 查看已关注城市
- 删除城市
- 右上角进入地图选城

### 地图选城页

- 打开后先回到当前位置
- 地图中心即当前选择点
- 拖动或缩放停下后自动刷新地点
- 优先识别到市级城市名称
- 点击“添加到天气”后返回主流程

### AI 助手

- 底部弹层聊天界面
- 键盘弹出时输入框跟随上移
- 基于当前城市天气提供建议
- 支持快捷提问和自由输入

## 当前目录结构

```text
app/src/main/java/com/example/weather
├─ MainActivity.java
├─ CityManageActivity.java
├─ MapPickActivity.java
├─ WeatherApplication.java
├─ adapter/
├─ api/
├─ db/
├─ location/
├─ model/
├─ service/
├─ ui/
└─ utils/
```

## 说明

- 本项目当前以单 APK 方式打包
- 城市地图识别以市级为主，不追求街道级精确地址
- AI 服务异常时不会阻断天气主流程
