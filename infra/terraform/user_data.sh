#!/bin/bash
# Bootstraps a fresh Amazon Linux 2023 (arm64) instance to run dora's production
# Docker Compose stack. Idempotent-ish: safe to re-run (e.g. after a
# user_data_replace_on_change instance replacement).
set -euxo pipefail

exec > >(tee -a /var/log/dora-user-data.log) 2>&1
echo "=== dora user_data starting at $(date -u) ==="

# ------------------------------------------------------------------
# Docker + Compose plugin
# ------------------------------------------------------------------
dnf update -y
dnf install -y docker git

systemctl enable --now docker
usermod -aG docker ec2-user

if [ ! -x /usr/libexec/docker/cli-plugins/docker-compose ]; then
  mkdir -p /usr/libexec/docker/cli-plugins
  ARCH=$(uname -m) # aarch64 on t4g
  case "$ARCH" in
    aarch64) COMPOSE_ARCH="aarch64" ;;
    x86_64)  COMPOSE_ARCH="x86_64" ;;
    *) echo "unsupported arch: $ARCH" >&2; exit 1 ;;
  esac
  curl -fsSL \
    "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-${COMPOSE_ARCH}" \
    -o /usr/libexec/docker/cli-plugins/docker-compose
  chmod +x /usr/libexec/docker/cli-plugins/docker-compose
fi

# ------------------------------------------------------------------
# Extra EBS data volume -> /data (Postgres/Redis/Ollama persistent data +
# Docker's data-root, so it's independent from the smaller root volume).
# ------------------------------------------------------------------
DATA_DEVICE=""
for candidate in /dev/nvme1n1 /dev/xvdf /dev/sdf; do
  if [ -b "$candidate" ]; then
    DATA_DEVICE="$candidate"
    break
  fi
done

DATA_MOUNT="/data"
mkdir -p "$DATA_MOUNT"

if [ -n "$DATA_DEVICE" ]; then
  if ! blkid "$DATA_DEVICE" >/dev/null 2>&1; then
    mkfs -t xfs "$DATA_DEVICE"
  fi

  UUID=$(blkid -s UUID -o value "$DATA_DEVICE")
  if ! grep -q "$UUID" /etc/fstab; then
    echo "UUID=$UUID $DATA_MOUNT xfs defaults,nofail 0 2" >> /etc/fstab
  fi
  mount -a
else
  echo "WARNING: no extra data device found; falling back to root volume for /data" >&2
fi

# Point Docker's data-root at the mounted data volume.
mkdir -p "$DATA_MOUNT/docker"
if [ ! -f /etc/docker/daemon.json ]; then
  systemctl stop docker
  cat > /etc/docker/daemon.json <<EOF
{
  "data-root": "$DATA_MOUNT/docker"
}
EOF
  systemctl start docker
fi

# ------------------------------------------------------------------
# App directory — the repo gets cloned/copied here manually on first deploy
# (see docs/DEPLOY-AWS.md). SSM Session Manager is used for shell access.
# ------------------------------------------------------------------
mkdir -p /opt/dora
chown ec2-user:ec2-user /opt/dora

echo "=== dora user_data finished at $(date -u) ==="
