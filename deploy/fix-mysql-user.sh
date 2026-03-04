#!/bin/bash
mysql -uroot -p'<REDACTED_DB_PASSWORD>' <<'SQL'
CREATE USER IF NOT EXISTS 'travel_user'@'localhost' IDENTIFIED BY 'Travel@2024';
GRANT ALL PRIVILEGES ON travel_prediction.* TO 'travel_user'@'localhost';
FLUSH PRIVILEGES;
SELECT User,Host FROM mysql.user WHERE User='travel_user';
SQL
echo "DONE"
