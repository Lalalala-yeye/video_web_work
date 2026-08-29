#!/bin/sh
# 把仓库里的演示样片拷到 uploads（宿主机卷若为空，clone 后也能播）
set -e
UPLOAD_ROOT="${UPLOAD_PATH:-/app/uploads}"
mkdir -p "$UPLOAD_ROOT/videos" "$UPLOAD_ROOT/covers" "$UPLOAD_ROOT/avatars"
if [ -d /opt/demo-media/videos ]; then
  cp -n /opt/demo-media/videos/. "$UPLOAD_ROOT/videos/" 2>/dev/null || true
fi
if [ -d /opt/demo-media/covers ]; then
  cp -n /opt/demo-media/covers/. "$UPLOAD_ROOT/covers/" 2>/dev/null || true
fi
exec java -jar /app/app.jar
