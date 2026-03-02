<script>
export default {
  onLaunch() {
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style lang="scss">
@import "@/uni.scss";

page {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue',
    'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #2C3E50;
  background-color: #F2F5F8;
  -webkit-font-smoothing: antialiased;
}

.safe-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* AI 悬浮球 */
.ai-fab {
  position: fixed;
  right: 16px;
  bottom: 110px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ai-fab-bubble {
  background: #fff;
  padding: 8px 14px;
  border-radius: 12px 12px 4px 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  margin-bottom: 10px;
  font-size: 12px;
  color: #333;
  font-weight: 500;
  max-width: 150px;
  animation: bubbleIn 0.35s ease;
}

.ai-fab-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2A9D8F 0%, #1A6B5A 100%);
  border: 3px solid #fff;
  box-shadow: 0 6px 20px rgba(42, 157, 143, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fabFloat 3s ease-in-out infinite;
}

.ai-fab-label {
  font-size: 14px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 1px;
}

@keyframes fabFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes bubbleIn {
  from { opacity: 0; transform: translateY(6px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
</style>
