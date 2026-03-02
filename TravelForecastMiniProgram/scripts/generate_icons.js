const fs = require('fs');
const path = require('path');

const targetDir = path.join(__dirname, '../src/static/tabbar');

// 简单的 1x1 像素 PNG Base64 (红色和绿色，区分选中状态)
const normalIcon = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAFVJREFUaEPt0sEJACAAwzB990/6cxgX0Qj2MzCl51a39f6vHwQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIEHgqsO0fEZ4e36IAAAAASUVORK5CYII=', 'base64'); // 灰色
const activeIcon = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAAAXNSR0IArs4c6QAAAFZJREFUaEPt0zENAAAMA6Dyz7oZNvAJkJC65dyt7q/+/SBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgAABAgQIECBAgMCtwE4nES6j120AAAAASUVORK5CYII=', 'base64'); // 绿色/青色

if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
}

const icons = ['home', 'guide', 'shop', 'profile'];

icons.forEach(name => {
    // 普通图标
    fs.writeFileSync(path.join(targetDir, `${name}.png`), normalIcon);
    // 选中图标
    fs.writeFileSync(path.join(targetDir, `${name}-active.png`), activeIcon);
    console.log(`Generated ${name}.png and ${name}-active.png`);
});
