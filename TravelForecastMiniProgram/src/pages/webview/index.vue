<template>
  <view class="wv-page">
    <web-view :src="url" @message="onMsg" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const url = ref('')

onLoad((options) => {
  if (options && options.url) {
    try {
      url.value = decodeURIComponent(options.url)
    } catch (_) {
      url.value = options.url
    }
  }
})

const onMsg = () => {}
</script>

<style lang="scss">
.wv-page { width: 100%; height: 100vh; }
</style>
