#!/bin/bash

# ---------------- 自动加载 secrets/.env ----------------
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
for cand in "$SCRIPT_DIR/../secrets/.env" "/opt/travel/secrets/.env"; do
    if [ -f "$cand" ]; then
        set -a
        # shellcheck disable=SC1090
        source "$cand"
        set +a
        break
    fi
done
# -------------------------------------------------------

# ---------------- 自动加载 secrets/.env ----------------
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
if [ -f "$REPO_ROOT/secrets/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "$REPO_ROOT/secrets/.env"
    set +a
elif [ -f "/opt/travel/secrets/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    source "/opt/travel/secrets/.env"
    set +a
else
    echo "❌ 未找到 secrets/.env"
    echo "   请复制 secrets/.env.example 为 secrets/.env 并填入真实密钥"
    exit 1
fi
# -------------------------------------------------------

mysql -uroot -p"${DB_PASSWORD}" <<'SQL'
CREATE USER IF NOT EXISTS 'travel_user'@'localhost' IDENTIFIED BY 'Travel@2024';
GRANT ALL PRIVILEGES ON travel_prediction.* TO 'travel_user'@'localhost';
FLUSH PRIVILEGES;
SELECT User,Host FROM mysql.user WHERE User='travel_user';
SQL
echo "DONE"
