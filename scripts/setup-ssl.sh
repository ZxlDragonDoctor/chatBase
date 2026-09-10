#!/bin/bash
# ChatBase SSL bootstrap for www.zxldragon.fun
# Usage: bash setup-ssl.sh
set -e
DOMAIN="${DOMAIN:-www.zxldragon.fun}"
EMAIL="${EMAIL:-2252406579@qq.com}"
WEBROOT="/var/www/certbot"

echo "==> install nginx + certbot"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq nginx certbot

mkdir -p "$WEBROOT"
mkdir -p /etc/nginx/conf.d

echo "==> temporary nginx for ACME"
cat > /etc/nginx/sites-available/default <<'NGINX'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 200 'ok';
        add_header Content-Type text/plain;
    }
}
NGINX
ln -sf /etc/nginx/sites-available/default /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx || systemctl restart nginx

echo "==> request certificate"
certbot certonly --webroot -w "$WEBROOT" -d "$DOMAIN" \
  --non-interactive --agree-tos -m "$EMAIL" --keep-until-expiring

echo "==> write chatbase nginx ssl conf"
cat > /etc/nginx/conf.d/chatbase.conf <<NGINX
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN};
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${DOMAIN};

    ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;

    client_max_body_size 100m;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 90s;
        proxy_send_timeout 90s;
        proxy_read_timeout 3600s;
        proxy_buffering off;
    }
}
NGINX

# avoid default site conflict on 80
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
systemctl enable nginx

echo "==> renewal hook"
mkdir -p /etc/letsencrypt/renewal-hooks/deploy
cat > /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'HOOK'
#!/bin/sh
systemctl reload nginx
HOOK
chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

echo "==> done. Check: https://${DOMAIN}"
certbot certificates || true
nginx -t
ss -lntp | grep -E ':80 |:443 ' || true
