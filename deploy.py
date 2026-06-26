import os
import posixpath
import time

import paramiko


HOST = os.getenv("ECS_HOST", "")
PORT = int(os.getenv("ECS_PORT", "22"))
USER = os.getenv("ECS_USER", "root")
PASSWORD = os.getenv("ECS_PASSWORD", "")
REMOTE_DIR = os.getenv("REMOTE_DIR", "/opt/linxihaven_demo")
LOCAL_DIR = os.getenv(
    "LOCAL_DIR",
    r"C:\Users\18201\Desktop\LinxiHaven-main\web-demo"
)
# API 密钥（环境变量优先，内置密钥兜底）
DEFAULT_API_KEY = "sk-f6438ea6f506a07b009e4a950aae88be2af3256605ee475ee0f78258e5e586aa"
QINIU_API_KEY = os.getenv("QINIU_API_KEY", DEFAULT_API_KEY)
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.qnaigc.com/v1")
AI_MODEL = os.getenv("AI_MODEL", "deepseek-v3")
PORT_TO_EXPOSE = int(os.getenv("DEMO_PORT", "9000"))


def require_env(name, value):
    if not value:
        raise RuntimeError(f"缺少环境变量: {name}")


def upload_dir(sftp, local_dir, remote_dir):
    try:
        sftp.mkdir(remote_dir)
    except OSError:
        pass

    for item in os.listdir(local_dir):
        local_path = os.path.join(local_dir, item)
        remote_path = posixpath.join(remote_dir, item)
        if os.path.isdir(local_path):
            upload_dir(sftp, local_path, remote_path)
        else:
            print(f"Uploading {local_path} -> {remote_path}")
            sftp.put(local_path, remote_path)


def deploy():
    require_env("ECS_HOST", HOST)
    require_env("ECS_PASSWORD", PASSWORD)
    require_env("QINIU_API_KEY", QINIU_API_KEY)

    print("Connecting to ECS...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        ssh.connect(HOST, port=PORT, username=USER, password=PASSWORD, timeout=10)
        print("Connected successfully!")
        ssh.exec_command(f"mkdir -p {REMOTE_DIR}")

        sftp = ssh.open_sftp()
        upload_dir(sftp, LOCAL_DIR, REMOTE_DIR)

        env_path = posixpath.join(REMOTE_DIR, ".env")
        env_content = (
            f"QINIU_API_KEY={QINIU_API_KEY}\n"
            f"OPENAI_BASE_URL={OPENAI_BASE_URL}\n"
            f"AI_MODEL={AI_MODEL}\n"
        )
        with sftp.file(env_path, "w") as env_file:
            env_file.write(env_content)
        sftp.chmod(env_path, 0o600)
        sftp.close()

        print("Starting Web Service on port", PORT_TO_EXPOSE)
        ssh.exec_command(f"fuser -k {PORT_TO_EXPOSE}/tcp")
        time.sleep(2)

        start_cmd = (
            f"cd {REMOTE_DIR} && "
            "set -a && source .env && set +a && "
            f"nohup python3 server.py {PORT_TO_EXPOSE} > server.log 2>&1 &"
        )
        ssh.exec_command(start_cmd)
        time.sleep(3)

        _, stdout, stderr = ssh.exec_command(
            f"(netstat -tlnp | grep {PORT_TO_EXPOSE}) || (ss -tlnp | grep {PORT_TO_EXPOSE})"
        )
        output = stdout.read().decode("utf-8")
        error_output = stderr.read().decode("utf-8")
        if str(PORT_TO_EXPOSE) in output:
            print(f"Web service started successfully on port {PORT_TO_EXPOSE}!")
        else:
            print("Failed to verify service status.")
            print(output)
            print(error_output)
        ssh.close()
    except Exception as exc:
        print(f"Error during deployment: {exc}")
        raise


if __name__ == "__main__":
    deploy()
