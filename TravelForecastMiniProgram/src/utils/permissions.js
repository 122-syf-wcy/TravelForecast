export const hasMiniProgramPermission = (scope) => new Promise((resolve) => {
  // #ifdef MP-WEIXIN
  uni.getSetting({
    success: (res) => resolve(!!res.authSetting?.[scope]),
    fail: () => resolve(false)
  })
  // #endif
  // #ifndef MP-WEIXIN
  resolve(true)
  // #endif
})

export const ensureMiniProgramPermission = (scope, description) => new Promise((resolve, reject) => {
  // #ifdef MP-WEIXIN
  uni.getSetting({
    success: (res) => {
      if (res.authSetting?.[scope]) {
        resolve(true)
        return
      }
      uni.authorize({
        scope,
        success: () => resolve(true),
        fail: () => {
          uni.showModal({
            title: '需要授权',
            content: description,
            confirmText: '去开启',
            success: (modalRes) => {
              if (!modalRes.confirm) {
                reject(new Error('用户取消授权'))
                return
              }
              uni.openSetting({
                success: (settingRes) => {
                  if (settingRes.authSetting?.[scope]) {
                    resolve(true)
                    return
                  }
                  reject(new Error('未授予权限'))
                },
                fail: () => reject(new Error('打开设置失败'))
              })
            },
            fail: () => reject(new Error('弹窗打开失败'))
          })
        }
      })
    },
    fail: () => reject(new Error('读取授权状态失败'))
  })
  // #endif
  // #ifndef MP-WEIXIN
  resolve(true)
  // #endif
})
