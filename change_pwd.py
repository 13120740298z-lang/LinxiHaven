import os

import paramiko


HOST = os.getenv("ECS_HOST", "")
PORT = int(os.getenv("ECS_PORT", "22"))
USER = os.getenv("ECS_USER", "root")
OLD_PASSWORD = os.getenv("ECS_OLD_PASSWORD", "")
NEW_PASSWORD = os.getenv("ECS_NEW_PASSWORD", "")


def change_password():
    if not HOST or not OLD_PASSWORD or not NEW_PASSWORD:
        raise RuntimeError("请设置 ECS_HOST、ECS_OLD_PASSWORD、ECS_NEW_PASSWORD 环境变量")

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(HOST, port=PORT, username=USER, password=OLD_PASSWORD, timeout=10)
        command = f"echo '{USER}:{NEW_PASSWORD}' | chpasswd"
        _, stdout, stderr = ssh.exec_command(command)
        err = stderr.read().decode("utf-8")
        if err:
            print(f"Error changing password: {err}")
        else:
            print("Password changed successfully!")
        ssh.close()
    except Exception as exc:
        print(f"Connection failed: {exc}")
        raise


if __name__ == "__main__":
    change_password()
