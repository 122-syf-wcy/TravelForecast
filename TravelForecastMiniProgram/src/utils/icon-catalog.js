const icon = (name) => `/static/icons/${name}.svg`

export const COMMON_ICONS = {
  ai: icon('ai-orbit'),
  arrowRight: icon('arrow-right'),
  audioWave: icon('audio-wave'),
  back: icon('back'),
  cart: icon('cart-bag'),
  checkCircle: icon('check-circle'),
  certificate: icon('certificate'),
  feedback: icon('feedback-bubble'),
  gear: icon('gear'),
  heartFilled: icon('heart-filled'),
  heartOutline: icon('heart-outline'),
  home: icon('home-simple'),
  keyboard: icon('keyboard'),
  location: icon('location-pin'),
  lock: icon('lock'),
  lightbulb: icon('lightbulb'),
  microphone: icon('microphone'),
  package: icon('package-box'),
  question: icon('question-circle'),
  passport: icon('passport-book'),
  refund: icon('refund'),
  search: icon('search'),
  service: icon('service-headset'),
  share: icon('share'),
  smile: icon('smile-circle'),
  star: icon('star-circle'),
  alert: icon('alert-circle'),
  shield: icon('shield-check'),
  trash: icon('trash'),
  truck: icon('truck-delivery'),
  wallet: icon('wallet'),
  edit: icon('edit-pen'),
  xCircle: icon('x-circle')
}

export const QUICK_CARD_ICONS = {
  redStudy: icon('study-red'),
  season: icon('snowflake')
}

export const HOME_MENU_LIST = [
  { name: '数字导游', icon: icon('guide-tour'), bg: 'linear-gradient(135deg, #f8a5c2, #f78fb3)' },
  { name: '红色研学', icon: icon('study-red'), bg: 'linear-gradient(135deg, #ff6b6b, #ee5a24)' },
  { name: '景区预约', icon: icon('ticket'), bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)' },
  { name: 'AI 伴游', icon: icon('ai-orbit'), bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  { name: '文创商城', icon: icon('shop-bag'), bg: 'linear-gradient(135deg, #55efc4, #00b894)' }
]

export const HOME_CATEGORY_VISUALS = {
  '交通出行': { icon: icon('transport-bus'), bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' },
  '酒店': { icon: icon('hotel-bed'), bg: 'linear-gradient(135deg, #81ecec, #00cec9)' },
  '美食': { icon: icon('food-utensils'), bg: 'linear-gradient(135deg, #ffeaa7, #fdcb6e)' },
  '非遗体验': { icon: icon('heritage-knot'), bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
  '民宿': { icon: icon('homestay-home'), bg: 'linear-gradient(135deg, #fd79a8, #e84393)' },
  '门票': { icon: icon('ticket'), bg: 'linear-gradient(135deg, #55efc4, #00b894)' },
  '攻略': { icon: icon('route-map'), bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  '特色活动': { icon: icon('activity-calendar'), bg: 'linear-gradient(135deg, #fab1a0, #e17055)' },
  '优惠福利': { icon: icon('benefit-gift'), bg: 'linear-gradient(135deg, #ffeaa7, #f39c12)' },
  '更多服务': { icon: icon('more-grid'), bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' }
}

export const HOME_CATEGORY_LIST = Object.entries(HOME_CATEGORY_VISUALS).map(([name, visual]) => ({
  name,
  ...visual
}))

export const PROFILE_BADGE_VISUALS = {
  '初来乍到': { icon: COMMON_ICONS.location, bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  '三线学者': { icon: icon('study-red'), bg: 'linear-gradient(135deg, #ff6b6b, #ee5a24)' },
  '美食猎人': { icon: icon('food-utensils'), bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)' },
  '凉都达人': { icon: icon('snowflake'), bg: 'linear-gradient(135deg, #55efc4, #00b894)' },
  '文创大师': { icon: icon('heritage-knot'), bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
  '全景解锁': { icon: icon('route-map'), bg: 'linear-gradient(135deg, #FFD93D, #F39C12)' },
  '答题学霸': { icon: icon('study-red'), bg: 'linear-gradient(135deg, #fd79a8, #e84393)' },
  '全景探索': { icon: icon('route-map'), bg: 'linear-gradient(135deg, #00cec9, #0984e3)' },
  '凉都研学大师': { icon: COMMON_ICONS.passport, bg: 'linear-gradient(135deg, #FFD93D, #F39C12)' }
}

export const PROFILE_ORDER_VISUALS = {
  '待付款': { icon: COMMON_ICONS.wallet, bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  '待使用': { icon: icon('ticket'), bg: 'linear-gradient(135deg, #55efc4, #00b894)' },
  '待评价': { icon: COMMON_ICONS.star, bg: 'linear-gradient(135deg, #ffd32a, #f6b93b)' },
  '退款': { icon: COMMON_ICONS.refund, bg: 'linear-gradient(135deg, #dfe6e9, #b2bec3)' }
}

export const PROFILE_MENU_VISUALS = {
  '我的行程': { icon: icon('route-map'), bg: 'linear-gradient(135deg, #74b9ff, #0984e3)' },
  'AI 对话历史': { icon: COMMON_ICONS.ai, bg: 'linear-gradient(135deg, #2A9D8F, #1A6B5A)' },
  '地址管理': { icon: COMMON_ICONS.location, bg: 'linear-gradient(135deg, #fab1a0, #e17055)' },
  '意见反馈': { icon: COMMON_ICONS.feedback, bg: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
  '联系客服': { icon: COMMON_ICONS.service, bg: 'linear-gradient(135deg, #55efc4, #00b894)' }
}

export const FEEDBACK_TYPE_VISUALS = {
  '功能异常': { icon: COMMON_ICONS.alert, bg: '#FFF1EF', fg: '#E55039' },
  '体验问题': { icon: COMMON_ICONS.smile, bg: '#EAF8F5', fg: '#2A9D8F' },
  '内容纠错': { icon: COMMON_ICONS.edit, bg: '#F4ECFF', fg: '#8E44AD' },
  '功能建议': { icon: COMMON_ICONS.lightbulb, bg: '#FFF7E6', fg: '#F39C12' }
}

export const SHOP_CATEGORY_VISUALS = {
  '非遗文创': { icon: icon('heritage-knot'), bg: 'linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%)' },
  '地道美食': { icon: icon('food-utensils'), bg: 'linear-gradient(135deg, #fd79a8 0%, #e17055 100%)' },
  '茶饮伴手': { icon: icon('benefit-gift'), bg: 'linear-gradient(135deg, #55efc4 0%, #00b894 100%)' },
  '手工艺品': { icon: icon('shop-bag'), bg: 'linear-gradient(135deg, #fdcb6e 0%, #e17055 100%)' }
}

export const SHOP_SERVICE_LIST = [
  { name: '正品保障', icon: COMMON_ICONS.shield },
  { name: '7天无理由', icon: COMMON_ICONS.checkCircle },
  { name: '全国包邮', icon: COMMON_ICONS.truck },
  { name: '非遗认证', icon: COMMON_ICONS.certificate }
]

export const SHOP_FEATURE_ICONS = {
  highland: COMMON_ICONS.location,
  fresh: COMMON_ICONS.shield,
  delivery: COMMON_ICONS.truck,
  heritage: icon('heritage-knot'),
  certificate: COMMON_ICONS.certificate,
  package: COMMON_ICONS.package,
  organic: icon('leaf'),
  award: COMMON_ICONS.star,
  quality: COMMON_ICONS.shield
}

export const decorateServiceCategory = (category = {}) => {
  const visual = HOME_CATEGORY_VISUALS[category.name] || HOME_CATEGORY_VISUALS['更多服务']
  return {
    ...category,
    bg: category.bg || visual.bg,
    icon: category.icon || visual.icon
  }
}

export const resolveServiceItemVisual = (item = {}) => {
  const visual = HOME_CATEGORY_VISUALS[item.category] || HOME_CATEGORY_VISUALS['更多服务']
  return {
    coverBg: visual.bg,
    coverIcon: visual.icon
  }
}

export const decorateProfileBadge = (badge = {}) => {
  const visual = PROFILE_BADGE_VISUALS[badge.name] || PROFILE_BADGE_VISUALS['全景解锁']
  return {
    ...badge,
    bg: badge.bg || visual.bg,
    icon: badge.icon || visual.icon
  }
}

export const decorateProfileOrder = (order = {}) => {
  const visual = PROFILE_ORDER_VISUALS[order.name] || PROFILE_ORDER_VISUALS['待付款']
  return {
    ...order,
    bg: order.bg || visual.bg,
    icon: order.icon || visual.icon
  }
}

export const decorateProfileMenu = (menu = {}) => {
  const visual = PROFILE_MENU_VISUALS[menu.name] || PROFILE_MENU_VISUALS['我的行程']
  return {
    ...menu,
    bg: menu.bg || visual.bg,
    icon: menu.icon || visual.icon
  }
}

export const decorateFeedbackType = (item = {}) => {
  const visual = FEEDBACK_TYPE_VISUALS[item.name] || FEEDBACK_TYPE_VISUALS['功能建议']
  return {
    ...item,
    icon: item.icon || visual.icon,
    bg: item.bg || visual.bg,
    fg: item.fg || visual.fg
  }
}

export const resolveShopCategoryVisual = (category) => (
  SHOP_CATEGORY_VISUALS[category] || {
    icon: icon('shop-bag'),
    bg: 'linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%)'
  }
)
