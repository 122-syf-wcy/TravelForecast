/**
 * 数字人控制器 (AvatarController)
 * 智教黔行 - 3D数字人研学导师
 * 
 * 功能：
 * - 3D模型加载与优化
 * - 骨骼动画状态机管理
 * - 口型同步驱动
 * - 与地图坐标系同步
 */

import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';
import { LipSyncEngine } from './LipSyncEngine.js';

// 动画状态枚举
export const ANIMATION_STATES = {
    IDLE: 'idle',
    SPEAKING: 'speaking',
    WALKING: 'walking',
    POINTING: 'pointing',
    WAVING: 'waving',
    THINKING: 'thinking'
};

// 默认配置
const DEFAULT_CONFIG = {
    modelScale: 1,
    enableDraco: true,
    dracoDecoderPath: 'https://www.gstatic.com/draco/versioned/decoders/1.5.6/',
    lipSyncMode: 'auto',
    smoothingFactor: 0.15,
    crossFadeDuration: 0.3
};

export class AvatarController {
    constructor(scene, options = {}) {
        this.scene = scene;
        this.config = { ...DEFAULT_CONFIG, ...options };

        // 模型和动画
        this.model = null;
        this.mixer = null;
        this.skeleton = null;
        this.animations = {};
        this.currentAction = null;
        this.currentState = ANIMATION_STATES.IDLE;

        // MorphTarget网格（用于口型同步）
        this.morphTargetMeshes = [];

        // 口型同步引擎
        this.lipSyncEngine = new LipSyncEngine({
            mode: this.config.lipSyncMode,
            smoothingFactor: this.config.smoothingFactor,
            onWeightsUpdate: (weights) => {
                this._applyMorphTargets(weights);
            }
        });

        // 状态
        this.isLoaded = false;
        this.isVisible = true;
        this._modelScaleLocked = false;
        this._isSpeaking = false;
        this._speakingTime = 0;

        // 回调
        this.onLoadComplete = null;
        this.onLoadError = null;
        this.onStateChange = null;

        // 初始化加载器
        this._initLoaders();

        console.log('🧍 AvatarController 初始化完成');
    }

    _initLoaders() {
        this.gltfLoader = new GLTFLoader();

        if (this.config.enableDraco) {
            const dracoLoader = new DRACOLoader();
            dracoLoader.setDecoderPath(this.config.dracoDecoderPath);
            this.gltfLoader.setDRACOLoader(dracoLoader);
        }
    }

    loadModel(modelUrl) {
        return new Promise((resolve, reject) => {
            console.log(`📦 开始加载模型: ${modelUrl}`);

            this.gltfLoader.load(
                modelUrl,
                (gltf) => {
                    console.log('✅ 模型加载成功');
                    this._processModel(gltf);
                    resolve(this.model);
                },
                (progress) => {
                    if (progress.total > 0) {
                        const percent = Math.round((progress.loaded / progress.total) * 100);
                        console.log(`📥 加载进度: ${percent}%`);
                    }
                },
                (error) => {
                    console.error('❌ 模型加载失败:', error);
                    if (this.onLoadError) {
                        this.onLoadError(error);
                    }
                    reject(error);
                }
            );
        });
    }

    _processModel(gltf) {
        this.model = gltf.scene;

        // 应用缩放
        const scale = this.config.modelScale;
        this.model.scale.set(scale, scale, scale);
        this._modelScaleLocked = true;

        // RPM模型是Y-up标准，通常不需要旋转
        // 如果需要面向特定方向，可以调整Y轴旋转
        this.model.rotation.x = Math.PI / 2; // 让模型站立
        // Y轴不旋转，模型自然面向镜头

        // 遍历模型，收集MorphTarget网格和骨骼
        this.model.traverse((child) => {
            if (child.isMesh) {
                child.castShadow = true;
                child.receiveShadow = true;

                if (child.morphTargetInfluences && child.morphTargetInfluences.length > 0) {
                    this.morphTargetMeshes.push(child);
                    const names = child.morphTargetDictionary ? Object.keys(child.morphTargetDictionary) : [];
                    console.log(`📐 发现MorphTarget网格: ${child.name}, BlendShapes: ${names.length}`);
                }
            }

            if (child.isSkinnedMesh && child.skeleton) {
                this.skeleton = child.skeleton;
            }
        });

        this.scene.add(this.model);

        if (gltf.animations && gltf.animations.length > 0) {
            this._processAnimations(gltf.animations);
        }

        this.isLoaded = true;

        if (this.onLoadComplete) {
            this.onLoadComplete(this.model);
        }
    }

    _processAnimations(animationClips) {
        this.mixer = new THREE.AnimationMixer(this.model);

        animationClips.forEach((clip, index) => {
            // 移除缩放轨道，防止动画改变模型大小
            const tracks = clip.tracks.filter(track => !track.name.includes('.scale'));
            clip.tracks = tracks;

            const action = this.mixer.clipAction(clip);
            const name = clip.name.toLowerCase();
            let state = ANIMATION_STATES.IDLE;

            if (name.includes('idle') || name.includes('breath')) {
                state = ANIMATION_STATES.IDLE;
            } else if (name.includes('talk') || name.includes('speak')) {
                state = ANIMATION_STATES.SPEAKING;
            } else if (name.includes('walk') || name.includes('move')) {
                state = ANIMATION_STATES.WALKING;
            } else if (name.includes('point')) {
                state = ANIMATION_STATES.POINTING;
            } else if (name.includes('wave') || name.includes('greet')) {
                state = ANIMATION_STATES.WAVING;
            } else if (name.includes('think')) {
                state = ANIMATION_STATES.THINKING;
            }

            if (!this.animations[state]) {
                this.animations[state] = action;
            } else {
                this.animations[`animation_${index}`] = action;
            }

            console.log(`🎬 动画 ${index}: "${clip.name}" -> ${state}`);
        });

        this.playAnimation(ANIMATION_STATES.IDLE);
    }

    playAnimation(state, options = {}) {
        const action = this.animations[state];
        if (!action) {
            console.warn(`⚠️ 未找到动画状态: ${state}`);
            return;
        }

        const { loop = THREE.LoopRepeat, crossFade = true, duration = this.config.crossFadeDuration } = options;
        action.setLoop(loop);

        if (this.currentAction && crossFade) {
            this.currentAction.crossFadeTo(action, duration, true);
            action.reset().play();
        } else {
            if (this.currentAction) {
                this.currentAction.stop();
            }
            action.reset().play();
        }

        this.currentAction = action;
        const oldState = this.currentState;
        this.currentState = state;

        if (this.onStateChange && oldState !== state) {
            this.onStateChange(state, oldState);
        }

        console.log(`▶️ 播放动画: ${state}`);
    }

    getAvailableAnimations() {
        return Object.keys(this.animations);
    }

    nextAnimation() {
        const states = this.getAvailableAnimations();
        const currentIndex = states.indexOf(this.currentState);
        const nextIndex = (currentIndex + 1) % states.length;
        this.playAnimation(states[nextIndex]);
    }

    _applyMorphTargets(weights) {
        this.morphTargetMeshes.forEach(mesh => {
            const dictionary = mesh.morphTargetDictionary;
            if (!dictionary) return;

            Object.entries(weights).forEach(([name, value]) => {
                let index = dictionary[name];

                if (index === undefined) {
                    const variations = this._getNameVariations(name);
                    for (const variant of variations) {
                        if (dictionary[variant] !== undefined) {
                            index = dictionary[variant];
                            break;
                        }
                    }
                }

                if (index !== undefined && value > 0) {
                    mesh.morphTargetInfluences[index] = value;
                }
            });
        });
    }

    _getNameVariations(name) {
        const variations = [name];
        const mappings = {
            'jawOpen': ['jawOpen', 'Jaw_Open', 'jaw_open', 'mouthOpen', 'mouth_open', 'Open'],
            'mouthFunnel': ['mouthFunnel', 'Mouth_Funnel', 'mouth_funnel', 'Funnel'],
            'mouthPucker': ['mouthPucker', 'Mouth_Pucker', 'mouth_pucker', 'Pucker'],
            'mouthSmileLeft': ['mouthSmileLeft', 'Mouth_Smile_Left', 'mouth_smile_left', 'smileLeft'],
            'mouthSmileRight': ['mouthSmileRight', 'Mouth_Smile_Right', 'mouth_smile_right', 'smileRight'],
            'mouthClose': ['mouthClose', 'Mouth_Close', 'mouth_close'],
            'mouthRollLower': ['mouthRollLower', 'Mouth_Roll_Lower', 'mouth_roll_lower'],
            'mouthRollUpper': ['mouthRollUpper', 'Mouth_Roll_Upper', 'mouth_roll_upper']
        };

        if (mappings[name]) {
            variations.push(...mappings[name]);
        }

        const snakeCase = name.replace(/([A-Z])/g, '_$1').toLowerCase();
        const pascalCase = name.charAt(0).toUpperCase() + name.slice(1);
        variations.push(snakeCase, pascalCase);

        return [...new Set(variations)];
    }

    async startLipSync(audioSource, visemeData = null) {
        console.log('👄 启动口型同步...');

        // 标记正在说话
        this._isSpeaking = true;
        this._speakingTime = 0;

        // 初始化音频分析（无论是否有MorphTarget都初始化）
        await this.lipSyncEngine.initAudioAnalyser();

        if (visemeData) {
            this.lipSyncEngine.addVisemeData(visemeData);
        }

        this.lipSyncEngine.connectAudioSource(audioSource);

        // 切换到说话动画
        this.playAnimation(ANIMATION_STATES.SPEAKING);

        const mode = this.morphTargetMeshes.length > 0 ? '口型BlendShapes' : '备用动画';
        console.log(`👄 口型同步已启动 (${mode})`);
    }

    stopLipSync() {
        this.lipSyncEngine.clearVisemeQueue();
        this._isSpeaking = false;
        this.playAnimation(ANIMATION_STATES.IDLE);
        console.log('👄 口型同步已停止');
    }

    update(deltaTime, audioCurrentTime = null) {
        // 更新动画混合器
        if (this.mixer) {
            this.mixer.update(deltaTime);
        }

        // 更新口型同步
        if (this.lipSyncEngine) {
            const weights = this.lipSyncEngine.update(deltaTime, audioCurrentTime);

            if (this.morphTargetMeshes.length > 0) {
                // 有MorphTargets时使用口型同步
                if (weights && Object.keys(weights).some(k => weights[k] > 0)) {
                    this._applyMorphTargets(weights);
                }
            } else if (this._isSpeaking && weights) {
                // 没有MorphTargets时使用备用动画
                this._applyFallbackSpeakingAnimation(weights, deltaTime);
            }
        }

        // 强制锁定缩放
        if (this.model && this._modelScaleLocked) {
            const s = this.config.modelScale;
            this.model.scale.set(s, s, s);
        }
    }

    _applyFallbackSpeakingAnimation(weights, deltaTime) {
        if (!this.model) return;

        const volume = weights.jawOpen || 0;
        this._speakingTime = (this._speakingTime || 0) + deltaTime;

        const headBobAmount = volume * 0.02;
        const headBobSpeed = 8;

        const headRotationY = Math.sin(this._speakingTime * headBobSpeed) * headBobAmount;
        const headRotationX = Math.sin(this._speakingTime * headBobSpeed * 1.5) * headBobAmount * 0.5;

        const baseRotationX = Math.PI / 2;
        this.model.rotation.x = baseRotationX + headRotationX;
        this.model.rotation.z = headRotationY;
    }

    setPosition(position) {
        if (this.model) {
            this.model.position.copy(position);
        }
    }

    setRotation(rotation) {
        if (this.model) {
            this.model.rotation.copy(rotation);
        }
    }

    setScale(scale) {
        if (this._modelScaleLocked) return;
        if (this.model) {
            this.model.scale.set(scale, scale, scale);
        }
    }

    setVisible(visible) {
        if (this.model) {
            this.model.visible = visible;
            this.isVisible = visible;
        }
    }

    getLipSyncEngine() {
        return this.lipSyncEngine;
    }

    destroy() {
        if (this.lipSyncEngine) {
            this.lipSyncEngine.destroy();
        }

        if (this.mixer) {
            this.mixer.stopAllAction();
            this.mixer = null;
        }

        if (this.model) {
            this.scene.remove(this.model);
            this.model.traverse((child) => {
                if (child.geometry) child.geometry.dispose();
                if (child.material) {
                    if (Array.isArray(child.material)) {
                        child.material.forEach(m => m.dispose());
                    } else {
                        child.material.dispose();
                    }
                }
            });
            this.model = null;
        }

        this.morphTargetMeshes = [];
        this.animations = {};
        this.isLoaded = false;

        console.log('🔌 AvatarController 已销毁');
    }
}
