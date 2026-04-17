本文件夹：02-素材与源码

作用：存放智教黔行项目的全部源代码和代表性素材。

当前文件清单：
1. 智教黔行-素材与源码.zip ─ 源代码压缩包（61 MB，996 文件；已排除 node_modules / target / .git 等）
2. 源码目录（原始开发目录，供本地查阅）：
   ├── TravelForecastBackend/                  # Java Spring Boot 主后端服务
   ├── TravelForecastGateway/                  # Spring Cloud Gateway 网关
   ├── TravelForecastingAIBackend/             # Java AI 服务（DashScope / RAG）
   ├── TravelForecast-DigitalHuman/            # Python FastAPI 数字人服务
   ├── TravelForecast-PythonPredictionService/ # Python 客流预测服务
   ├── TravelForecastFrontend/                 # Vue 3 Web 前端
   ├── TravelForecastMiniProgram/              # 微信小程序前端
   ├── TravelForecastMiniProgramBackend/       # 小程序后端
   ├── deploy/                                 # 部署配置（Docker / K8s / 脚本）
   └── docs/                                   # 项目文档与设计稿

技术栈：
  - 后端：   Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL + Redis
  - AI：     Python 3.10 + FastAPI + DeepSeek + DashScope + Edge TTS
  - 预测：   Python + PyTorch + ARIMA + LSTM
  - 前端：   Vue 3 + Vite + Element Plus + ECharts / ECharts GL
  - 小程序： UniApp + Vue 3
  - 网关：   Spring Cloud Gateway

说明：
  - 提交评审时仅需 zip 压缩包，原始目录已同步打包
  - 构建与运行说明详见压缩包内 deploy/ 目录与 docs/ 目录
  - 所有代码均为团队原创，核心算法模块：
      ARIMA-LSTM 双流融合预测（Python）
      RAG 检索增强（Java + Python）
      三级缓存（内存 LRU + 磁盘 JSON + 启动预热）
