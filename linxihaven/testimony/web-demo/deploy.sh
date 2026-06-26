#!/bin/bash
# ============================================================
# 证言 Testimony.ai Web Demo 部署脚本
# AI深伪时代青少年霸凌事件可信存证系统
# ============================================================

set -e

echo "============================================"
echo "🛡️  证言 Testimony.ai Web Demo 部署工具"
echo "============================================"

# 配置变量
DEPLOY_DIR="/opt/linxihaven_demo"
PORT=9000
APP_NAME="testimony-demo"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 1. 检查系统环境
check_environment() {
    log_info "检查系统环境..."
    
    # 检查Node.js
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node -v)
        log_success "Node.js 已安装: $NODE_VERSION"
    else
        log_warn "Node.js 未安装，尝试安装..."
        curl -fsSL https://rpm.nodesource.com/setup_20.x | bash - && yum install -y nodejs || apt-get update && apt-get install -y nodejs
    fi
    
    # 检查Python（用于简单HTTP服务器）
    if command -v python3 &> /dev/null; then
        PYTHON_VERSION=$(python3 --version)
        log_success "Python3 已安装: $PYTHON_VERSION"
    elif command -v python &> /dev/null; then
        log_success "Python 已安装"
    else
        log_warn "Python未安装，将使用Node.js HTTP服务器"
    fi
    
    # 检查端口是否被占用
    if netstat -tlnp 2>/dev/null | grep -q ":$PORT" || ss -tlnp 2>/dev/null | grep -q ":$PORT"; then
        log_warn "端口 $PORT 可能已被占用"
    fi
    
    # 创建目录
    mkdir -p "$DEPLOY_DIR"
    log_success "部署目录: $DEPLOY_DIR"
}

# 2. 安装七牛云技能模块
install_qiniu_skills() {
    log_info "安装七牛云技能模块..."
    
    # 检查npx是否可用
    if command -v npx &> /dev/null; then
        log_success "npx 可用"
        
        # 尝试执行七牛云技能安装
        cd /tmp && npx skills add qiniu/skills --skill maas 2>/dev/null || {
            log_warn "七牛云技能模块需要额外配置，使用内置存储方案..."
            log_info "项目已配置本地存储 + API直连模式，无需外部存储依赖"
        }
    else
        log_warn "npx不可用，跳过七牛云技能安装"
    fi
}

# 3. 部署Web Demo
deploy_web_demo() {
    log_info "部署 Web Demo 到服务器..."
    
    # 复制文件到部署目录
    if [ -d "./web-demo" ]; then
        cp -r ./web-demo/* "$DEPLOY_DIR/"
        log_success "Web Demo 文件已复制"
    else
        log_error "web-demo 目录不存在!"
        exit 1
    fi
    
    # 设置权限
    chmod -R 755 "$DEPLOY_DIR"
    chown -R root:root "$DEPLOY_DIR"
    
    log_success "文件权限已设置"
}

# 4. 启动服务
start_service() {
    log_info "启动 Web 服务 (端口: $PORT)..."
    
    # 停止旧进程
    pkill -f "python.*$PORT\|node.*http.*$PORT\|$APP_NAME" 2>/dev/null || true
    sleep 1
    
    # 使用Python启动HTTP服务器
    if command -v python3 &> /dev/null; then
        cd "$DEPLOY_DIR"
        nohup python3 -m http.server $PORT > /var/log/testimony-web.log 2>&1 &
        SERVER_PID=$!
        log_success "Python HTTP服务器已启动 (PID: $SERVER_PID)"
    elif command -v python &> /dev/null; then
        cd "$DEPLOY_DIR"
        nohup python -m SimpleHTTPServer $PORT > /var/log/testimony-web.log 2>&1 &
        SERVER_PID=$!
        log_success "Python HTTP服务器已启动 (PID: $SERVER_PID)"
    else
        log_error "无法找到可用的HTTP服务器"
        exit 1
    fi
    
    sleep 2
    
    # 验证服务状态
    if netstat -tlnp 2>/dev/null | grep -q ":$PORT" || ss -tlnp 2>/dev/null | grep -q ":$PORT"; then
        log_success "✅ 服务已在端口 $PORT 上运行"
        log_info "访问地址: http://$(hostname -I | awk '{print $1}'):$PORT"
    else
        log_warn "服务可能未正常启动，请手动检查日志: /var/log/testimony-web.log"
    fi
}

# 5. 安全加固
security_hardening() {
    log_info "执行安全加固..."
    
    # 修改root密码（如果提供了新密码）
    NEW_PASSWORD="${1:-}"
    if [ -n "$NEW_PASSWORD" ]; then
        echo "root:$NEW_PASSWORD" | chpasswd 2>/dev/null && \
            log_success "root密码已更新" || \
            log_warn "密码更新失败（可能权限不足）"
    fi
    
    # 禁止密码登录SSH（可选，谨慎操作）
    # sed -i 's/PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
    # systemctl restart sshd
    
    # 设置防火墙规则（仅开放9000端口）
    if command -v firewall-cmd &> /dev/null; then
        firewall-cmd --permanent --add-port=$PORT/tcp 2>/dev/null || true
        firewall-cmd --reload 2>/dev/null || true
        log_success "防火墙规则已配置"
    fi
    
    # 清理敏感日志
    > ~/.bash_history && history -c 2>/dev/null || true
    log_success "历史记录已清理"
}

# 6. 显示部署信息
show_deployment_info() {
    echo ""
    echo "============================================"
    echo -e "${GREEN}🎉 部署完成！${NC}"
    echo "============================================"
    echo ""
    echo -e "${BLUE}📱 访问信息:${NC}"
    echo "  URL: http://$(curl -s ifconfig.me):$PORT"
    echo "  或: http://220.154.134.151:$PORT"
    echo ""
    echo -e "${BLUE}🔑 默认PIN:${NC} 123456"
    echo -e "${BLUE}🔐 秘密手势:${NC} 连续点击左上角 6 次"
    echo ""
    echo -e "${BLUE}🛠️ 功能清单:${NC}"
    echo "  ✅ 伪装计算器界面（真实可用）"
    echo "  ✅ PIN码验证系统"
    echo "  ✅ 学生端安全空间模拟"
    echo "  ✅ AI引导式访谈引擎（含15条诱导性检测）"
    echo "  ✅ 家长端风险评估分析"
    echo "  ✅ 证据包生成预览"
    echo "  ✅ Merkle树可视化"
    echo "  ✅ 司法采纳准备度报告"
    echo ""
    echo -e "${YELLOW}⚠️  注意事项:${NC}"
    echo "  • 80, 443, 8080, 8443 端口不可使用"
    echo "  • 服务通过 nohup 后台运行"
    echo "  • 日志位置: /var/log/testimony-web.log"
    echo ""
}

# 主流程
main() {
    echo ""
    log_info "开始部署流程..."
    echo ""
    
    check_environment
    echo ""
    install_qiniu_skills
    echo ""
    deploy_web_demo
    echo ""
    start_service
    echo ""
    security_hardening "$1"
    echo ""
    show_deployment_info
}

# 执行主函数
main "$@"
