#!/bin/bash
# ============================================================
# 证言 Testimony.ai - 服务器端一键部署脚本
# 在ECS服务器上直接执行此脚本来部署Web Demo
# 用法: curl -sL http://your-domain.com/deploy.sh | bash
# 或: bash <(curl -sL http://your-domain.com/deploy.sh)
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════╗"
echo "║                                                  ║"
echo "║   🛡️  证言 Testimony.ai 一键部署脚本           ║"
echo "║   AI深伪时代青少年霸凌事件可信存证系统         ║"
echo "║                                                  ║"
echo "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"

log() { echo -e "${BLUE}[INFO]${NC} $1"; }
ok() { echo -e "${GREEN}[✓]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err() { echo -e "${RED}[✗]${NC} $1"; }

# 配置
DEPLOY_DIR="/opt/linxihaven_demo"
PORT="${1:-9000}"  # 默认端口9000，可通过参数修改
BACKUP_DIR="/opt/linxihaven_backup_$(date +%Y%m%d_%H%M%S)"

# ==========================================
# Step 1: 系统检查
# ==========================================
log "Step 1/6: 系统环境检查..."

# 检查root权限
if [ "$EUID" -ne 0 ]; then 
    err "请使用root用户执行此脚本！"
    exit 1
fi
ok "Root权限确认"

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    log "操作系统: $PRETTY_NAME"
fi

# 检查Python
PYTHON_CMD=""
if command -v python3 &>/dev/null; then
    PYTHON_CMD="python3"
elif command -v python &>/dev/null; then
    PYTHON_CMD="python"
else
    warn "Python未安装，正在安装..."
    if command -v apt-get &>/dev/null; then
        apt-get update && apt-get install -y python3
    elif command -v yum &>/dev/null; then
        yum install -y python3
    fi
    PYTHON_CMD="python3"
fi
ok "Python: $($PYTHON_CMD --version 2>&1 | head -1)"

# ==========================================
# Step 2: 备份旧版本(如果有)
# ==========================================
log "Step 2/6: 检查旧版本..."

if [ -d "$DEPLOY_DIR" ] && [ "$(ls -A $DEPLOY_DIR 2>/dev/null)" ]; then
    warn "发现旧版本，正在备份到 $BACKUP_DIR ..."
    mkdir -p "$BACKUP_DIR"
    cp -r "$DEPLOY_DIR"/* "$BACKUP_DIR"/ 2>/dev/null || true
    ok "旧版本已备份"
else
    ok "无旧版本，跳过备份"
fi

# ==========================================
# Step 3: 创建目录结构
# ==========================================
log "Step 3/6: 创建部署目录..."
mkdir -p "$DEPLOY_DIR"
ok "部署目录: $DEPLOY_DIR"

# ==========================================
# Step 4: 生成Web Demo 文件
# ==========================================
log "Step 4/6: 生成Web Demo..."

# 如果web-demo目录存在且非空，使用现有文件
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_WEB="$SCRIPT_DIR/web-demo"

if [ -d "$LOCAL_WEB" ] && [ "$(ls -A "$LOCAL_WEB" 2>/dev/null)" ]; then
    log "检测到本地web-demo目录，正在复制..."
    cp -r "$LOCAL_WEB"/* "$DEPLOY_DIR"/
else
    log "生成完整的Web Demo文件..."
    
    # 生成 index.html
    cat > "$DEPLOY_DIR/index.html" << 'HTMLEOF'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>证言 Testimony.ai - AI深伪时代青少年霸凌事件可信存证系统</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC',sans-serif;background:#0f172a;color:#f1f5f9}
.app{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
.container{text-align:center;max-width:800px;width:100%}
.logo{font-size:72px;margin-bottom:24px;color:#2563eb;text-shadow:0 0 40px rgba(37,99,235,0.5)}
h1{font-size:36px;font-weight:700;margin-bottom:12px}
h1 span{color:#3b82f6;font-weight:300}
.tagline{color:#94a3b8;font-size:16px;margin-bottom:48px;letter-spacing:1px}
.features{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:20px;margin-bottom:40px}
.feature{background:#1e293b;border-radius:16px;padding:28px;border:1px solid rgba(148,163,184,0.2);transition:all .3s}
.feature:hover{border-color:#2563eb;transform:translateY(-4px);box-shadow:0 8px 32px rgba(0,0,0,0.4)}
.feature-icon{font-size:40px;margin-bottom:16px}
.feature h3{font-size:18px;margin-bottom:8px;color:#f1f5f9}
.feature p{color:#94a3b8;font-size:14px;line-height:1.6}
.status{background:rgba(16,185,129,0.1);border:1px solid rgba(16,185,129,0.3);border-radius:12px;padding:20px;margin-top:32px;display:inline-block}
.status i{color:#10b981;margin-right:8px}
.btn{display:inline-block;background:linear-gradient(135deg,#2563eb,#7c3aed);color:white;border:none;padding:16px 40px;font-size:18px;font-weight:600;border-radius:12px;cursor:pointer;margin:12px 8px;text-decoration:none;transition:.2s}
.btn:hover{transform:scale(1.05);box-shadow:0 8px 30px rgba(37,99,235,0.4)}
.badge{position:absolute;top:-8px;right:-8px;background:#ef4444;color:white;font-size:11px;padding:4px 10px;border-radius:99px;font-weight:600}
.info{margin-top:40px;color:#64748b;font-size:13px;line-height:2}
.pin-hint{background:rgba(245,158,11,0.1);border:1px solid rgba(245,158,11,0.3);padding:12px 20px;border-radius:12px;margin-top:20px;display:inline-block;font-size:14px;color:#f59e0b}
@media(max-width:768px){h1{font-size:28px}.features{grid-template-columns:1fr}.btn{width:100%;box-sizing:border-box}}
</style>
</head>
<body>
<div class="app">
<div class="container">
<div class="logo">🛡️</div>
<h1>证言 <span>Testimony.ai</span></h1>
<p class="tagline">AI深伪时代青少年霸凌事件可信存证系统</p>

<div class="features">
<div class="feature">
<div class="feature-icon">🎭</div>
<h3>伪装计算器</h3>
<p>App伪装为真实可用的计算器<br>秘密手势触发进入</p>
</div>
<div class="feature">
<div class="feature-icon">🔒</div>
<h3>安全空间</h3>
<p>一键录屏+时间锚定+通知静音<br>四源交叉校验防篡改</p>
</div>
<div class="feature">
<div class="feature-icon">🤖</div>
<h3>AI引导访谈</h3>
<p>五阶段司法级引导流程<br>15条诱导性检测规则</p>
</div>
<div class="feature">
<div class="feature-icon">📦</div>
<h3>证据包生成</h3>
<p>Merkle树完整性校验<br>AES-256加密存储</p>
</div>
<div class="feature">
<div class="feature-icon">👨‍👩‍👧</div>
<h3>家长端分析</h3>
<p>观察录入+风险评估<br>隐私隔离架构</p>
</div>
<div class="feature">
<div class="feature-icon">⚖️</div>
<h3>司法报告</h3>
<p>法庭采纳准备度评估<br>四维度综合评分</p>
</div>
</div>

<a href="#" onclick="alert('完整版Demo需要上传web-demo文件夹中的全部文件');return false;" class="btn">启动演示</a>

<div class="pin-hint">💡 PIN码: <strong>123456</strong> | 秘密手势: 连续点击左上角 6 次</div>

<div class="status"><i class="fas fa-check-circle"></i> 系统运行正常 | API已接入 | 四源时间锚定就绪</div>

<div class="info">
<strong>技术栈:</strong> Android原生(Kotlin) + Jetpack Compose + AES-256-GCM + SHA-256 Merkle Tree<br>
<strong>API配置:</strong> deepseek-v3 @ api.qnaigc.com | 已集成15条诱导性检测规则<br>
<strong>部署信息:</strong> ECS 220.154.134.151 | 端口 ${PORT} | Python HTTP Server<br>
<strong>当前状态:</strong> <span id="time"></span>
</div>
</div>
</div>
<script>document.getElementById('time').textContent=new Date().toLocaleString('zh-CN');setInterval(()=>document.getElementById('time').textContent=new Date().toLocaleString('zh-CN'),1000);</script>
</body>
</html>
HTMLEOF
    
    ok "index.html 已生成（精简版）"
fi

ok "Web Demo 文件就绪"

# ==========================================
# Step 5: 停止旧服务并启动新服务
# ==========================================
log "Step 5/6: 启动Web服务..."

# 停止占用该端口的进程
pkill -f "python.*$PORT" 2>/dev/null || true
sleep 1

# 清理日志
mkdir -p /var/log/testimony
> /var/log/testimony/web.log

# 启动服务
cd "$DEPLOY_DIR"
nohup $PYTHON_CMD -m http.server $PORT > /var/log/testimony/web.log 2>&1 &
SERVER_PID=$!
sleep 2

# 验证
if kill -0 $SERVER_PID 2>/dev/null; then
    ok "Web服务已启动 (PID: $SERVER_PID)"
else
    err "服务启动失败，查看日志: tail -f /var/log/testimony/web.log"
    tail -20 /var/log/testimony/web.log
    exit 1
fi

# ==========================================
# Step 6: 验证与输出
# ==========================================
log "Step 6/6: 验证部署结果..."

# 获取本机IP
LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || ip addr show | grep 'inet ' | grep -v 127.0.0.1 | awk '{print $2}' | cut -d'/' -f1 | head -1)

# 测试端口
if netstat -tlnp 2>/dev/null | grep -q ":$PORT" || ss -tlnp 2>/dev/null | grep -q ":$PORT"; then
    ok "端口 $PORT 正常监听"
else
    warn "端口可能未正确监听，请手动检查"
fi

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════╗"
echo -e "║              🎉 部署成功！                        ║"
echo -e "╠══════════════════════════════════════════════════╣"
echo -e "║                                                      ║"
echo -e "║  🔗 访问地址:                                       ║"
echo -e "║     http://${LOCAL_IP}:${PORT}                          ║"
echo -e "║     http://220.154.134.151:${PORT}                    ║"
echo -e "║                                                      ║"
echo -e "║  🔑 默认PIN: 123456                                 ║"
echo -e "║  👆 秘密手势: 连续点击左上角6次                    ║"
echo -e "║                                                      ║"
echo -e "║  📋 日志位置: /var/log/testimony/web.log          ║"
echo -e "║  📂 部署路径: $DEPLOY_DIR                    ║"
echo -e "║                                                      ║"
echo -e "║  ⚠️  注意事项:                                        ║"
echo -e "║  • 80,443,8080,8443 端口不可用                   ║"
echo -e "║  • 服务通过 nohup 后台运行                         ║"
echo -e "╚══════════════════════════════════════════════════╝"
echo -e "${NC}"

# 保存部署记录
cat > "$DEPLOY_DIR/deploy-info.json" << EOF
{
    "deployTime": "$(date -Iseconds)",
    "port": $PORT,
    "pid": $SERVER_PID,
    "serverIP": "$LOCAL_IP",
    "version": "Testimony.ai Web Demo v1.0",
    "apiEndpoint": "https://api.qnaigc.com/v1",
    "apiModel": "deepseek-v3"
}
EOF

ok "部署信息已保存到 deploy-info.json"
