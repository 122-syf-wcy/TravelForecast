#!/bin/bash
echo "=== OPTIONS via Nginx ==="
curl -sv -X OPTIONS http://127.0.0.1/api/auth/login \
  -H "Origin: http://39.97.232.141" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  2>&1 | grep -E 'HTTP/|Access-Control|< '

echo ""
echo "=== POST login via Nginx ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST http://127.0.0.1/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://39.97.232.141" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}'

echo ""
echo "=== Content landing via Nginx ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1/api/content/landing
