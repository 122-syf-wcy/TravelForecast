import request from '@/utils/request'

// 获取平台系统状态（管理员）
export function getSystemStatus() {
    return request<{
        serverStatus: {
            cpu: number
            memory: number
            disk: number
            uptime: number
            responseTime: number
        }
        serviceStatus: {
            api: 'normal' | 'warning' | 'error'
            database: 'normal' | 'warning' | 'error'
            cache: 'normal' | 'warning' | 'error'
            prediction: 'normal' | 'warning' | 'error'
        }
        recentErrors: Array<{
            time: string
            service: string
            message: string
            count: number
        }>
        activeUsers: {
            total: number
            userRoleDistribution: {
                user: number
                business: number
                admin: number
            }
        }
    }>({
        url: '/admin/dashboard',
        method: 'get'
    })
}

// 获取区域旅游数据统计（管理员）
export function getRegionStats(params?: {
    period?: 'day' | 'week' | 'month' | 'year'
    startDate?: string
    endDate?: string
}) {
    return request<{
        period: string
        totalVisitors: number
        visitorsTrend: Array<{
            date: string
            count: number
        }>
        scenicRanking: Array<{
            id: number
            name: string
            visitors: number
            growth: number
            revenue: number
        }>
        geographicDistribution: Record<string, number>
        economicImpact: {
            totalRevenue: number
            averageSpending: number
            employmentEffect: number
            indirectBenefit: number
        }
    }>({
        url: '/stats/overview',
        method: 'get',
        params
    })
}

// 获取平台数据统计（管理员）
export function getPlatformStats(params?: {
    period?: 'day' | 'week' | 'month' | 'year'
}) {
    return request<{
        period: string
        userGrowth: {
            total: number
            newUsers: number
            activeUsers: number
            retentionRate: number
        }
        usage: {
            totalPlans: number
            totalPredictions: number
            avgSessionTime: number
            mostUsedFeatures: Array<{
                feature: string
                usage: number
            }>
        }
        predictionAccuracy: {
            overallAccuracy: number
            accuracyTrend: Array<{
                date: string
                accuracy: number
            }>
            modelPerformance: Record<string, number>
        }
    }>({
        url: '/admin/analytics',
        method: 'get',
        params
    })
}

// 获取模型训练状态（管理员）
export function getModelTrainingStatus() {
    return request<{
        activeTraining: boolean
        lastTrainingTime: string
        currentModel: string
        trainingProgress: number
        metrics: {
            accuracy: number
            precision: number
            recall: number
            f1Score: number
            mse: number
        }
        datasetStats: {
            totalRecords: number
            trainTestSplit: string
            features: string[]
            dataQuality: number
        }
    }>({
        url: '/models',
        method: 'get'
    })
}

// 获取政策效果模拟数据（管理员）
export function getPolicySimulation(policyId: number) {
    return request<{
        policyId: number
        policyName: string
        simulationResult: {
            visitorIncrease: number
            revenueChange: number
            congestionImpact: string
            environmentalImpact: string
            socialImpact: string
        }
        scenarioComparison: Array<{
            scenario: string
            visitorChange: number
            revenueChange: number
            description: string
        }>
        recommendations: string[]
    }>({
        url: `/admin/policy-simulation/${policyId}`,
        method: 'get'
    })
} 