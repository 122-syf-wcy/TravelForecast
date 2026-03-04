#!/bin/bash
echo "=== OPTIONS preflight test ==="
curl -sv -X OPTIONS http://127.0.0.1:8888/api/auth/login \
  -H "Origin: http://39.97.232.141" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  2>&1 | grep -E 'HTTP/|Access-Control|< '

echo ""
echo "=== POST login test ==="
curl -s -X POST http://127.0.0.1:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://39.97.232.141" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}' \
  2>&1 | head -3

echo ""
echo "=== Backend direct POST ==="
curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"qwe123","password":"123456","captchaId":"test","captchaCode":"test"}' \
  2>&1 | head -3

echo ""
echo "=== Content landing ==="
curl -s http://127.0.0.1:8888/api/content/landing 2>&1 | head -3
