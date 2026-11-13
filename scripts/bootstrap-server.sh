#!/usr/bin/env bash
# Run only on the approved EC2, after reviewing installation/settings with its owner.
set -euo pipefail
[[ ${1:-} == --approved ]] || { echo 'Review changes and pass --approved after owner approval.' >&2; exit 1; }
[[ $EUID == 0 && $(uname -m) == aarch64 ]] || { echo 'Requires root on ARM64 Linux.' >&2; exit 1; }
# shellcheck source=/dev/null
. /etc/os-release
[[ $ID == ubuntu && $VERSION_ID == 24.04 ]] || { echo 'Requires Ubuntu 24.04.' >&2; exit 1; }
# Refuse to replace another swap configuration or overwrite an existing file.
if swapon --show=NAME --noheadings | grep -qv '^/swapfile$'; then
  echo 'Existing swap differs; review before changing it.' >&2; exit 1
fi
if awk '$1 !~ /^#/ && $3 == "swap" && ($1 != "/swapfile" || $4 != "sw") {found=1} END {exit !found}' /etc/fstab; then
  echo 'Existing fstab swap configuration differs; review before changing it.' >&2; exit 1
fi
if [[ -e /swapfile ]]; then
  [[ ! -L /swapfile && $(stat -c %s /swapfile) == 1073741824 && $(blkid -p -s TYPE -o value /swapfile) == swap ]] || {
    echo 'Existing /swapfile differs; refusing to overwrite.' >&2; exit 1;
  }
fi
apt-get update
apt-get install -y ca-certificates curl gnupg util-linux
if ! command -v docker >/dev/null; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  echo 'deb [arch=arm64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu noble stable' > /etc/apt/sources.list.d/docker.list
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
docker compose version
systemctl enable --now docker
if systemctl list-unit-files amazon-ssm-agent.service --no-legend | grep -q amazon-ssm-agent; then
  systemctl enable --now amazon-ssm-agent
else
  command -v snap >/dev/null || apt-get install -y snapd
  snap list amazon-ssm-agent >/dev/null 2>&1 || snap install amazon-ssm-agent --classic
  snap start --enable amazon-ssm-agent
fi
if [[ ! -e /swapfile ]]; then
  umask 077
  dd if=/dev/zero of=/swapfile bs=1M count=1024 status=none
  chmod 600 /swapfile
  mkswap /swapfile
fi
chmod 600 /swapfile
swapon --show=NAME --noheadings | grep -qx /swapfile || swapon /swapfile
grep -Eq '^/swapfile[[:space:]]' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
printf 'vm.swappiness=10\n' > /etc/sysctl.d/90-daiso-swap.conf
sysctl -p /etc/sysctl.d/90-daiso-swap.conf
install -d -m 700 /opt/daiso /opt/daiso/releases /opt/daiso/state
swapon --show
printf 'Prepare /opt/daiso/.env (root:root, 600) and root GHCR login separately. No deployment has been started.\n'
