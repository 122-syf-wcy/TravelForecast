# secrets/ 说明

本文件夹存放所有敏感凭证，**不应提交到版本控制**。

## 文件清单

| 文件 | 作用 | 是否提交 |
|---|---|---|
| `.env.example` | 环境变量模板（含占位符） | ✅ 提交 |
| `.env`         | 真实密钥（本地 / 服务器配置） | ❌ 禁止提交 |
| `README.md`    | 本说明文件 | ✅ 提交 |

## 使用步骤

1. **首次配置**
   ```bash
   cp secrets/.env.example secrets/.env
   # 用编辑器打开 secrets/.env，填入真实密钥
   ```

2. **启动服务**（deploy/start-all.sh 会自动加载）
   ```bash
   bash deploy/start-all.sh start
   ```

3. **本地 IDE 运行**：请在 IDE 的运行配置里加载 `secrets/.env` 作为环境变量。
   - IntelliJ IDEA：EnvFile 插件
   - VS Code：`.vscode/launch.json` 里配置 `envFile: "${workspaceFolder}/secrets/.env"`

## 获取各密钥的渠道

| 密钥 | 申请入口 |
|---|---|
| `OSS_ACCESS_KEY_*` | https://ram.console.aliyun.com/ |
| `DASHSCOPE_API_KEY` | https://dashscope.console.aliyun.com/apiKey |
| `DEEPSEEK_API_KEY` | https://platform.deepseek.com/api_keys |
| `AMAP_API_KEY` | https://lbs.amap.com/dev/key/app |
| `WECHAT_APPID / APPSECRET` | https://mp.weixin.qq.com → 开发 → 开发管理 |

## ⚠️ 安全警告

- 若曾将真实密钥写入 Git 历史，**即使删除当前文件也必须到各平台后台轮换（revoke & regenerate）**。
- 建议使用最小权限策略，例如阿里云 RAM 子账户只授予 OSS 读写权限。
- 生产环境 JWT Secret 至少 32 字节且每环境独立。
