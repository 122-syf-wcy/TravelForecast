<template>
  <div class="not-found-container w-full h-screen flex flex-col items-center justify-center relative overflow-hidden">
    <!-- 清爽山水风背景装饰 -->
    <div class="bg-decoration">
      <div class="mountain mountain-1"></div>
      <div class="mountain mountain-2"></div>
      <div class="mountain mountain-3"></div>
      <div class="cloud cloud-1"></div>
      <div class="cloud cloud-2"></div>
      <div class="cloud cloud-3"></div>
    </div>
    
    <div class="z-10 text-center">
      <!-- 404大数字 -->
      <h1 class="text-[120px] font-bold leading-none mb-2 tracking-tight">
        <span class="text-[#2A9D8F]">4</span>
        <span class="text-[#E76F51]">0</span>
        <span class="text-[#2A9D8F]">4</span>
      </h1>
      
      <!-- 提示文字 -->
      <div class="my-6">
        <h2 class="text-2xl font-semibold text-[#2C3E50] mb-2">页面未找到</h2>
        <p class="text-[#909399] text-base">您访问的页面似乎已经迷失在山水之间</p>
      </div>
      
      <!-- 操作按钮 -->
      <div class="flex space-x-4 justify-center mt-8">
        <el-button type="default" size="large" @click="goBack" class="back-btn">
          <el-icon class="mr-1"><ArrowLeft /></el-icon>
          返回上页
        </el-button>
        <el-button type="primary" size="large" @click="goHome" class="home-btn">
          <el-icon class="mr-1"><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
    
    <!-- 底部水波装饰 -->
    <div class="wave-decoration">
      <svg viewBox="0 0 1200 120" preserveAspectRatio="none">
        <path d="M0,60 C200,100 400,20 600,60 C800,100 1000,20 1200,60 L1200,120 L0,120 Z" fill="rgba(42, 157, 143, 0.06)"/>
        <path d="M0,80 C200,40 400,100 600,80 C800,40 1000,100 1200,80 L1200,120 L0,120 Z" fill="rgba(42, 157, 143, 0.04)"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found-container {
  background: linear-gradient(180deg, #F5F7FA 0%, #E8F6F3 100%);
  min-height: 100vh;
}

/* 山水背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.mountain {
  position: absolute;
  bottom: 0;
  border-radius: 50% 50% 0 0;
  opacity: 0.06;
}

.mountain-1 {
  width: 500px;
  height: 250px;
  left: -5%;
  background: linear-gradient(to top, #2A9D8F, transparent);
}

.mountain-2 {
  width: 700px;
  height: 300px;
  left: 30%;
  background: linear-gradient(to top, #264653, transparent);
}

.mountain-3 {
  width: 450px;
  height: 200px;
  right: -5%;
  background: linear-gradient(to top, #2A9D8F, transparent);
}

.cloud {
  position: absolute;
  border-radius: 50%;
  background: rgba(42, 157, 143, 0.04);
  animation: float 20s ease-in-out infinite;
}

.cloud-1 {
  width: 200px;
  height: 60px;
  top: 15%;
  left: 10%;
  animation-delay: 0s;
}

.cloud-2 {
  width: 300px;
  height: 80px;
  top: 25%;
  right: 15%;
  animation-delay: -7s;
}

.cloud-3 {
  width: 160px;
  height: 50px;
  top: 10%;
  right: 35%;
  animation-delay: -14s;
}

@keyframes float {
  0%, 100% { transform: translateX(0); opacity: 0.5; }
  50% { transform: translateX(30px); opacity: 0.8; }
}

/* 按钮样式 */
.back-btn {
  border-color: #DCDFE6;
  color: #606266;
  border-radius: 10px;
  padding: 12px 24px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  border-color: #2A9D8F;
  color: #2A9D8F;
  background: rgba(42, 157, 143, 0.04);
}

.home-btn {
  background: linear-gradient(135deg, #2A9D8F, #264653);
  border: none;
  border-radius: 10px;
  padding: 12px 24px;
  color: #fff;
  transition: all 0.3s ease;
}

.home-btn:hover {
  box-shadow: 0 6px 20px rgba(42, 157, 143, 0.3);
  transform: translateY(-1px);
}

/* 底部水波 */
.wave-decoration {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  pointer-events: none;
}

.wave-decoration svg {
  width: 100%;
  height: 100%;
}
</style>
