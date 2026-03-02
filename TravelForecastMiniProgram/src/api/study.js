import { request } from './request'

/**
 * 获取研学题库
 */
export const fetchQuizList = (scenicName) =>
  request({ url: '/study/quiz', data: scenicName ? { scenicName } : {} })

/**
 * 提交答题
 */
export const submitQuizAnswer = (userId, quizId, userAnswer) =>
  request({ url: '/study/quiz/answer', method: 'POST', data: { userId, quizId, userAnswer } })

/**
 * 获取研学护照（徽章+积分）
 */
export const fetchPassport = (userId) =>
  request({ url: '/study/passport', data: { userId } })

/**
 * 增加积分
 */
export const addPoints = (userId, points) =>
  request({ url: '/study/points/add', method: 'POST', data: { userId, points } })
