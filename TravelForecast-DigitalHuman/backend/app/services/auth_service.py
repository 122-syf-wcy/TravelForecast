"""
智教黔行 - JWT 认证服务
提供 WebSocket 连接的令牌验证
"""

import jwt
import hashlib
from datetime import datetime, timedelta
from typing import Optional, Dict
from dataclasses import dataclass

from app.core.config import get_settings

settings = get_settings()


@dataclass
class User:
    """用户信息"""
    username: str
    role: str = "user"


class AuthService:
    """
    JWT 认证服务
    
    功能：
    - 生成访问令牌
    - 验证令牌
    - 简单的用户管理
    """
    
    # 默认管理员账户（生产环境应使用数据库）
    USERS = {
        "admin": {
            "password_hash": hashlib.sha256("admin123".encode()).hexdigest(),
            "role": "admin"
        },
        "guest": {
            "password_hash": hashlib.sha256("guest".encode()).hexdigest(),
            "role": "guest"
        }
    }
    
    def __init__(self):
        self.secret_key = settings.JWT_SECRET or "qianxiaoyou-secret-key-2024"
        self.algorithm = "HS256"
        self.access_token_expire_hours = 24
        
        print("[Auth] 认证服务初始化完成")
    
    def verify_password(self, username: str, password: str) -> bool:
        """验证用户密码"""
        if username not in self.USERS:
            return False
        
        password_hash = hashlib.sha256(password.encode()).hexdigest()
        return password_hash == self.USERS[username]["password_hash"]
    
    def authenticate_user(self, username: str, password: str) -> Optional[User]:
        """认证用户"""
        if not self.verify_password(username, password):
            return None
        
        return User(
            username=username,
            role=self.USERS[username]["role"]
        )
    
    def create_access_token(self, user: User) -> str:
        """创建访问令牌"""
        expire = datetime.utcnow() + timedelta(hours=self.access_token_expire_hours)
        
        payload = {
            "sub": user.username,
            "role": user.role,
            "exp": expire,
            "iat": datetime.utcnow()
        }
        
        token = jwt.encode(payload, self.secret_key, algorithm=self.algorithm)
        return token
    
    def verify_token(self, token: str) -> Optional[User]:
        """验证令牌"""
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=[self.algorithm])
            username = payload.get("sub")
            role = payload.get("role", "user")
            
            if username is None:
                return None
            
            return User(username=username, role=role)
            
        except jwt.ExpiredSignatureError:
            print("[Auth] 令牌已过期")
            return None
        except jwt.InvalidTokenError as e:
            print(f"[Auth] 无效令牌: {e}")
            return None
    
    def generate_guest_token(self) -> str:
        """生成访客令牌（用于未登录用户）"""
        guest_user = User(username="guest", role="guest")
        return self.create_access_token(guest_user)


# 单例
auth_service = AuthService()
