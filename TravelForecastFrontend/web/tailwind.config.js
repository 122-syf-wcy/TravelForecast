/** @type {import('tailwindcss').Config} */
module.exports = {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}"
    ],
    theme: {
        extend: {
            colors: {
                // 浅色山水风格配色
                'space-blue': '#F5F7FA',      // 原深蓝改为浅灰背景
                'neon-cyan': '#2A9D8F',       // 原霓虹青改为山水绿
                'tech-purple': '#457B9D',     // 原科技紫改为湖蓝
                'energy-orange': '#E9C46A',   // 原能量橙改为沙黄
                // 新增山水配色
                'mountain-green': '#2A9D8F',
                'lake-blue': '#457B9D',
                'sand-yellow': '#E9C46A',
                'sunset-orange': '#F4A261',
                'earth-brown': '#E76F51'
            },
            fontFamily: {
                'orbitron': ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
                'roboto': ['Roboto', 'sans-serif']
            },
            animation: {
                'glow': 'glow 2s ease-in-out infinite alternate',
                'matrix-flow': 'matrix-flow 20s linear infinite',
            },
            keyframes: {
                glow: {
                    '0%': { 'box-shadow': '0 0 5px rgba(42, 157, 143, 0.3)' },
                    '100%': { 'box-shadow': '0 0 15px rgba(42, 157, 143, 0.4)' }
                },
                'matrix-flow': {
                    '0%': { transform: 'translateY(-100%)' },
                    '100%': { transform: 'translateY(100%)' }
                }
            },
            backdropBlur: {
                'sm': '4px',
                'DEFAULT': '8px',
                'md': '12px',
                'lg': '16px',
                'xl': '24px',
            }
        }
    },
    plugins: [
        require('@tailwindcss/forms')
    ]
} 