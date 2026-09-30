#!/usr/bin/env bash
# MySQL 컨테이너의 DB 를 덤프해 backup/ 에 gzip 으로 남기고, 오래된 백업을 지운다.
# systemd 타이머(dalbit-backup.timer)가 매일 새벽에 돌린다. 손으로 돌려도 된다.
#
# 덤프가 중간에 끊긴 파일은 남아 있어도 복구에 못 쓴다. 끝까지 됐는지 확인한 뒤에만
# 이름을 붙여 백업으로 인정한다.
set -euo pipefail

BASE_DIR="${BASE_DIR:-$(cd "$(dirname "$0")" && pwd)}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.prod.yaml}"
BACKUP_DIR="${BACKUP_DIR:-backup}"
KEEP_DAYS="${KEEP_DAYS:-14}"
DB_NAME="dalbit_tarot"

cd "$BASE_DIR"
mkdir -p "$BACKUP_DIR"

target="$BACKUP_DIR/${DB_NAME}-$(date +%Y%m%d-%H%M%S).sql.gz"
partial="$target.partial"
trap 'rm -f "$partial"' ERR

# 비밀번호는 컨테이너 안의 환경변수로 넘긴다. 명령줄에 쓰면 프로세스 목록에 보인다
docker compose -f "$COMPOSE_FILE" exec -T mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --routines --triggers --default-character-set=utf8mb4 '"$DB_NAME" \
    | gzip > "$partial"

gzip -t "$partial"
# mysqldump 는 끝까지 쓰면 마지막 줄에 "Dump completed" 를 남긴다
if ! gzip -dc "$partial" | tail -n 1 | grep -q "Dump completed"; then
    echo "덤프가 끝까지 되지 않았다: $partial" >&2
    rm -f "$partial"
    exit 1
fi
mv "$partial" "$target"

find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -mtime +"$KEEP_DAYS" -delete

echo "백업 완료: $target ($(du -h "$target" | cut -f1))"
