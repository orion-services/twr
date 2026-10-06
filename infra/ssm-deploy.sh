#!/bin/bash
# Pulls the requested git ref into /opt/twr and rebuilds the Compose stack.
# Invoked on the EC2 instance by AWS-RunShellScript (see .github/workflows/deploy.yml).
# Expects GH_TOKEN, REPO (owner/name) and SHA in the environment. /opt/twr/.env
# must already exist (created once by hand; gitignored, so checkout leaves it alone).
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/twr}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"

if [ -z "${GH_TOKEN:-}" ] || [ -z "${REPO:-}" ] || [ -z "${SHA:-}" ]; then
  echo "GH_TOKEN, REPO and SHA must be set" >&2
  exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
  echo "missing $ENV_FILE — create it once from .env.example before deploying" >&2
  exit 1
fi

mkdir -p "$APP_DIR"
cd "$APP_DIR"
git config --global --add safe.directory "$APP_DIR" || true

if [ ! -d .git ]; then
  git init
  git remote add origin "https://x-access-token:${GH_TOKEN}@github.com/${REPO}.git"
else
  git remote set-url origin "https://x-access-token:${GH_TOKEN}@github.com/${REPO}.git"
fi

git fetch --depth 1 origin "$SHA"
git checkout -f FETCH_HEAD
git remote set-url origin "https://github.com/${REPO}.git"
unset GH_TOKEN

# The first boot adds ec2-user to the docker group; SSM runs as root.
if ! groups ec2-user | grep -q ' docker'; then
  usermod -aG docker ec2-user
fi

chown -R ec2-user:ec2-user "$APP_DIR"
# Keep .env readable only by the deploy user.
chmod 600 "$ENV_FILE" 2>/dev/null || true

sudo -u ec2-user -H docker compose -p twr --env-file "$ENV_FILE" config --quiet
sudo -u ec2-user -H docker compose -p twr --env-file "$ENV_FILE" up -d --build twr orion-users
sudo -u ec2-user -H docker compose -p twr --env-file "$ENV_FILE" up -d --force-recreate caddy
