/**
 * 头像工具函数
 * 基于用户名生成本地SVG头像
 */

// 男性头像基础颜色
const maleAvatarColors = [
    '#3498db', // 蓝色
    '#2980b9', // 深蓝色
    '#1abc9c', // 绿松石色
    '#16a085', // 深绿松石色
    '#2c3e50', // 深蓝灰色
    '#8e44ad', // 紫色
    '#27ae60', // 绿色
];

// 女性头像基础颜色
const femaleAvatarColors = [
    '#e74c3c', // 红色
    '#c0392b', // 深红色
    '#d35400', // 橙色
    '#e67e22', // 浅橙色
    '#f39c12', // 黄色
    '#9b59b6', // 浅紫色
    '#ff7979', // 粉红色
];

// 商家头像颜色
const businessColors = {
    bg: '#0D1117',
    primary: '#00FEFC',
    secondary: '#2D3748'
};

/**
 * 从字符串生成一个数字哈希值
 * @param str 输入字符串
 * @returns 哈希值
 */
function hashString(str: string): number {
    let hash = 0;
    if (str.length === 0) return hash;

    for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
    }

    return Math.abs(hash);
}

/**
 * 判断用户名是否为女性
 * 简单规则：包含"女士"、"小姐"、"女"等词汇的用户名视为女性
 * @param username 用户名
 * @returns 是否为女性
 */
export function isFemale(username: string): boolean {
    const femaleIndicators = ['女士', '女', '小姐', 'ms', 'mrs', 'miss'];
    return femaleIndicators.some(indicator => username.toLowerCase().includes(indicator));
}

/**
 * 生成用户头像SVG
 * @param username 用户名
 * @returns SVG数据URL
 */
export function getUserAvatar(username: string): string {
    const hash = hashString(username);
    const female = isFemale(username);
    const colors = female ? femaleAvatarColors : maleAvatarColors;
    const colorIndex = hash % colors.length;
    const mainColor = colors[colorIndex];

    // 提取用户名首字母或第一个汉字
    const initial = username.charAt(0).toUpperCase();

    // 生成简单的SVG头像
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="50" fill="${mainColor}" />
            <text x="50" y="50" font-family="Arial, sans-serif" font-size="40" 
                fill="white" text-anchor="middle" dominant-baseline="central" font-weight="bold">
                ${initial}
            </text>
        </svg>
    `;

    // 转换为数据URL
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

/**
 * 获取商家头像URL
 * @returns SVG数据URL
 */
export function getBusinessAvatar(): string {
    // 生成商家头像SVG
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
            <rect width="100" height="100" fill="${businessColors.bg}" />
            <path d="M20,70 L50,30 L80,70 Z" fill="${businessColors.primary}" />
            <text x="50" y="60" font-family="Arial, sans-serif" font-size="14" 
                fill="white" text-anchor="middle" dominant-baseline="central" font-weight="bold">
                商家
            </text>
        </svg>
    `;

    // 转换为数据URL
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

export default {
    getUserAvatar,
    getBusinessAvatar,
    isFemale
}; 