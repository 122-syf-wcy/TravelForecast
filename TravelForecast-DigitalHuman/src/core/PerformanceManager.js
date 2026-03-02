/**
 * 智教黔行 - 设备性能检测与降级
 * 自动检测设备性能，低端设备切换轻量模式
 */

(function () {
    'use strict';

    let isLiteMode = false;

    /**
     * 检测设备性能
     */
    function checkDevicePerformance() {
        const checks = {
            // 内存小于4GB
            lowMemory: navigator.deviceMemory && navigator.deviceMemory < 4,
            // 2G网络
            slowConnection: navigator.connection && navigator.connection.effectiveType === '2g',
            // 移动设备
            mobileDevice: /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent),
            // CPU核心数小于4
            lowCores: navigator.hardwareConcurrency && navigator.hardwareConcurrency < 4
        };

        const score = Object.values(checks).filter(Boolean).length;
        console.log(`[性能监测] 检测结果: ${score}/4`, checks);

        // 2个或以上指标触发降级
        if (score >= 2) {
            enableLiteMode('检测到设备性能较低');
        }

        return { score, checks };
    }

    /**
     * 启用轻量模式
     */
    function enableLiteMode(reason) {
        if (isLiteMode) return;
        isLiteMode = true;

        console.log(`[性能监测] 启用轻量模式: ${reason}`);

        // 禁用地图3D效果
        if (window.map) {
            window.map.setPitch(0);
            window.map.setZoom(12);
        }

        // 降低视频质量
        const avatarVideo = document.getElementById('avatarVideo');
        if (avatarVideo) {
            avatarVideo.style.filter = 'blur(0.5px)';
        }

        // 更新状态栏
        const statusBar = document.querySelector('.status-bar span');
        if (statusBar) {
            statusBar.textContent = '黔小游（轻量模式）';
        }

        // 添加轻量模式标记
        document.body.classList.add('lite-mode');

        // 减少动画
        document.documentElement.style.setProperty('--animation-duration', '0.1s');
    }

    /**
     * 禁用轻量模式
     */
    function disableLiteMode() {
        if (!isLiteMode) return;
        isLiteMode = false;

        console.log('[性能监测] 退出轻量模式');

        // 恢复地图3D效果
        if (window.map) {
            window.map.setPitch(50);
            window.map.setZoom(13);
        }

        // 恢复视频
        const avatarVideo = document.getElementById('avatarVideo');
        if (avatarVideo) {
            avatarVideo.style.filter = 'none';
        }

        // 更新状态栏
        const statusBar = document.querySelector('.status-bar span');
        if (statusBar) {
            statusBar.textContent = '黔小游已就位';
        }

        // 移除轻量模式标记
        document.body.classList.remove('lite-mode');

        document.documentElement.style.removeProperty('--animation-duration');
    }

    /**
     * 切换模式
     */
    function toggleLiteMode() {
        if (isLiteMode) {
            disableLiteMode();
        } else {
            enableLiteMode('用户手动切换');
        }
    }

    /**
     * 获取当前模式
     */
    function isLiteModeEnabled() {
        return isLiteMode;
    }

    // 暴露API
    window.PerformanceManager = {
        check: checkDevicePerformance,
        enableLite: enableLiteMode,
        disableLite: disableLiteMode,
        toggle: toggleLiteMode,
        isLite: isLiteModeEnabled
    };

    // 页面加载后自动检测
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            setTimeout(checkDevicePerformance, 1000);
        });
    } else {
        setTimeout(checkDevicePerformance, 1000);
    }

})();
