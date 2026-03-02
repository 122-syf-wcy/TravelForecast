# 景区视频讲解上传指南

## 视频文件存放位置

所有景区讲解视频应存放在以下目录中：

```
web/public/videos/
```

如果该目录不存在，请手动创建它。

## 视频命名规则

为了确保系统能正确识别并加载每个景区的视频，请按照以下命名规则保存视频文件：

| 景区名称 | 文件名 |
|---------|--------|
| 梅花山风景区 | meihuashan-guide.mp4 |
| 乌蒙大草原 | wumeng-guide.mp4 |
| 玉舍国家森林公园 | yushe-guide.mp4 |
| 明湖国家湿地公园 | minghu-guide.mp4 |
| 水城古镇 | shuicheng-guide.mp4 |

## 视频上传步骤

1. **从哔哩哔哩下载视频**
   - 找到合适的景区介绍视频
   - 使用视频下载工具下载视频（如 B站视频下载器、IDM等）
   - 确保下载的视频格式为MP4格式

2. **创建视频文件夹**
   - 在项目根目录下，找到 `web/public/` 文件夹
   - 在 `public` 文件夹中创建 `videos` 文件夹（如果不存在）

3. **放置视频文件**
   - 将下载好的视频文件重命名为对应景区的文件名
   - 将重命名后的视频文件复制到 `web/public/videos/` 文件夹中

4. **修改视频链接**（可选）
   - 如果您使用了不同的视频文件名，需要修改 `RealTimeService.vue` 文件中的 `getScenicVideo` 函数
   - 将在线视频链接替换为本地视频路径，如 `/videos/meihuashan-guide.mp4`

## 视频要求

- **格式**：MP4 格式（H.264 编码）
- **分辨率**：建议 1080p (1920x1080) 或 720p (1280x720)
- **时长**：建议 3-5 分钟，不超过 10 分钟
- **大小**：建议不超过 100MB

## 本地视频路径配置示例

如果您已经准备好了所有视频文件，可以将 `RealTimeService.vue` 文件中的 `getScenicVideo` 函数修改为：

```javascript
// 获取景区视频链接
const getScenicVideo = (scenicId: string): string => {
  // 使用本地视频路径
  const videoMap: Record<string, string> = {
    'meihuashan': '/videos/meihuashan-guide.mp4', // 梅花山视频
    'zhijindong': '/videos/wumeng-guide.mp4',     // 乌蒙大草原视频
    'yushe': '/videos/yushe-guide.mp4',           // 玉舍国家森林公园视频
    'minghu': '/videos/minghu-guide.mp4',         // 明湖国家湿地公园视频
    'panzhou': '/videos/shuicheng-guide.mp4'      // 水城古镇视频
  }
  
  // 返回对应景区的视频链接，如果没有则返回默认视频
  return videoMap[scenicId as keyof typeof videoMap] || '/videos/default-guide.mp4'
}
```

## 故障排除

如果视频无法播放，请检查：

1. 视频文件是否已正确放置在 `web/public/videos/` 目录中
2. 视频文件名是否与代码中的路径一致
3. 视频格式是否为浏览器支持的格式（建议使用MP4格式）
4. 视频文件是否损坏或不完整

## 注意事项

- 请确保您有权使用和分发从哔哩哔哩下载的视频内容
- 视频内容应当符合相关法律法规
- 建议使用高质量、专业的景区介绍视频，以提升用户体验 