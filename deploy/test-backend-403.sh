#!/bin/bash
echo "=== Backend direct POST with Origin header ==="
curl -sv -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://39.97.232.141" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}' \
  2>&1 | grep -E 'HTTP/|< |Invalid|403|Forbidden'

echo ""
echo "=== Backend direct POST without Origin ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}'

echo ""
echo "=== Backend direct POST response body with Origin ==="
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://39.97.232.141" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}'

echo ""
echo "=== Check backend log for 403 ==="
tail -20 /opt/travel/logs/backend.log 2>/dev/null | grep -i '403\|forbid\|denied\|cors\|error' | tail -5
