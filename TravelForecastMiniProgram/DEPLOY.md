# 小程序上线前必做配置

> 本文档记录将 `TravelForecastMiniProgram` 发布到微信小程序生产环境前**必须**完成的配置项。体验版 / 开发版可以跳过。

## 一、微信小程序后台 → 开发管理 → 开发设置 → 服务器域名

小程序在**真机 / 体验版 / 正式版**下会校验服务器域名，只有登记过的域名才能被 `uni.request` / `uni.uploadFile` / `uni.downloadFile` / `<web-view>` 调用到。请在登录 [mp.weixin.qq.com](https://mp.weixin.qq.com) 后进入 `开发管理 → 开发设置 → 服务器域名` 修改。

### request 合法域名（必填）

```
https://travel.dongsiwei.com
```

- 用于所有业务接口调用，包括用户登录、景区、商城、AI 对话、意见反馈等

### uploadFile 合法域名（必填）

```
https://travel.dongsiwei.com
```

- 用于数字人页面的录音上传 (`/api/digital-human/voice-chat`)、后续用户头像上传等

### downloadFile 合法域名（必填）

```
https://travel.dongsiwei.com
```

- 用于数字人 TTS 语音文件下载、后续下载图片等

### 业务域名（仅当 banner link 是外链时填）

```
https://travel.dongsiwei.com
```

- `pages/webview/index` 使用 `<web-view>` 渲染外链；若不登记则外链白屏

---

## 二、`manifest.json` 权限声明

已在 `src/manifest.json → mp-weixin.permission` 内配置好 `scope.userLocation`。上线前再次确认以下字段没被删除：

```jsonc
"permission": {
  "scope.userLocation": {
    "desc": "您的位置信息将用于显示景区距离、提供导航与实时客流信息"
  }
}
```

如后续新增相机 / 相册等权限，也须补充对应 `scope.*` 描述。

---

## 三、订阅消息模板（可选，若要用模板消息）

当前 `pages/profile/settings.vue` 里的 `SUBSCRIBE_TMPL_IDS` 为空数组，"消息通知"开关只存本地 flag，不会真的调起授权。

如果后续需要订单支付成功、研学打卡提醒等模板消息，请：

1. 在 `mp.weixin.qq.com → 功能 → 订阅消息` 下选用或新建模板
2. 复制对应的 `tmplId`
3. 填入 `src/pages/profile/settings.vue` 顶部的 `SUBSCRIBE_TMPL_IDS` 数组

---

## 四、上传代码前的检查清单

- [ ] 服务器域名 4 项全部已登记
- [ ] `src/utils/url.js` / `src/config/index.js` 里的 `API_BASE_URL` 指向正式域名
- [ ] `manifest.json` 里 `mp-weixin.appid` 已替换为真实 AppID
- [ ] `pages/index/index.vue` 已去掉 `wttr.in` 外部调用（已处理 ✓）
- [ ] `App.vue` 已去掉调试 `console.log`（已处理 ✓）
- [ ] `src/pages/profile/settings.vue` 的版本号展示符合预期（模拟器显示 "1.0.0"，真机正式版显示 `miniProgram.version`）
- [ ] 真机跑通：登录 / 地图定位 / 数字人对话（含录音）/ 下单支付 / 研学答题

---

## 五、已知的非阻断性问题（不影响上线，后续迭代处理）

参考扫描结论，以下为**中/低优先级**体验问题，与审核无关：

- 订单支付成功后没有自动切到"待使用" tab
- 订单取消 / 删除未加确认弹窗
- 各详情页首次加载期间没有 skeleton / 加载中态
- 优惠券 `useCoupon` 没传券 id 进商城
- 地址选择回跳在部分场景静默失败（eventChannel 未注册的调用方）
- 研学答题 / 基地接口返回空时没有本地兜底数据
