# 배포 운영 문서

EC2 한 대에 Docker Compose 로 앱·MySQL·Nginx 를 띄운다.
GitHub Actions 가 `main` 푸시를 받아 이미지를 만들고 EC2 에서 교체한다.

```
브라우저 → Nginx(80/443) → app(8080) → mysql(3306, 내부 전용)
                                    ↘ Gemini API (아웃바운드)
```

## 1. AWS 계정 (직접)

**배포를 시작할 때 만든다.** 무료 플랜은 크레딧이 남아 있어도 계정 생성 6개월이 지나면 끝난다.

1. 계정 생성 → $100 크레딧 자동 지급
2. 온보딩 과제 5개로 $100 추가 (EC2 시작·종료 / RDS 구성 / Lambda 배포 / Bedrock 프롬프트 / **Budgets 예산 설정**)
3. 루트 계정에 MFA 설정, 작업용 IAM 사용자 생성
4. Budgets 에서 월 한도 알림 설정 — 온보딩 과제이기도 하다

## 2. EC2 (직접)

| 항목 | 값 |
|---|---|
| AMI | Amazon Linux 2023 |
| 타입 | t3.micro |
| 스토리지 | gp3 20GB |
| 키 페어 | 새로 만들어 `.pem` 을 안전한 곳에 보관 |

보안 그룹 인바운드:

| 포트 | 소스 | 용도 |
|---|---|---|
| 22 | **내 IP** | SSH. 0.0.0.0/0 으로 열지 않는다 |
| 80 | 0.0.0.0/0 | HTTP (certbot 인증 포함) |
| 443 | 0.0.0.0/0 | HTTPS |

MySQL 3306 은 열지 않는다. 컨테이너 내부 네트워크로만 통신한다.

## 3. 서버 준비 (직접, SSH 접속 후)

```bash
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user      # 재접속해야 적용된다

# Amazon Linux 2023 의 docker 패키지에는 compose 플러그인이 없다. 따로 받는다
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version                # 버전이 나오면 된다

# t3.micro 는 메모리가 1GB 라 MySQL 과 JVM 을 같이 올리면 모자란다. 스왑 2GB 를 잡는다
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
free -h                               # Swap 줄에 2.0G 가 보이면 된다

mkdir -p ~/dalbit-tarot/{nginx,certbot/conf,certbot/www,backup}
```

저장소의 아래 파일을 서버 `~/dalbit-tarot/` 로 복사한다.

- `deploy/compose.prod.yaml` → `compose.prod.yaml`
- `deploy/nginx/dalbit-tarot.conf` → `nginx/dalbit-tarot.conf`
- `deploy/.env.example` → `.env` (값 채우기)

`.env` 에 넣을 값:

| 키 | 설명 |
|---|---|
| `APP_IMAGE` | `ghcr.io/yoohyebin/dalbit-tarot:latest` |
| `MYSQL_ROOT_PASSWORD` | 새로 만든 강한 값 |
| `DB_PASSWORD` | 새로 만든 강한 값. 로컬 개발값(`dalbit`)을 쓰지 않는다 |
| `GEMINI_API_KEY` | **유료 티어 키.** 무료 티어는 입력이 모델 학습에 쓰인다 |
| `ADMIN_USERNAME` | 관리자 화면 아이디. 기본 `admin` |
| `ADMIN_PASSWORD` | 관리자 화면 비밀번호. 로컬에서 쓰던 값을 그대로 쓰지 않는다 |

`ADMIN_PASSWORD` 를 비워 두면 컨테이너가 뜰 때마다 무작위 비밀번호를 만들어 로그에만 남긴다.
재시작하면 값이 바뀌므로 운영에서는 채워 둔다.

HTTPS 를 붙이기 전까지 `/admin` 로그인은 비밀번호가 평문으로 나간다. 6장을 끝낸 뒤에 쓴다.

## 4. GitHub Secrets (직접)

저장소 Settings → Secrets and variables → Actions:

| 이름 | 값 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | `.pem` 파일 **전체 내용** |

이미지 레지스트리는 GHCR 을 쓰며 `GITHUB_TOKEN` 이 자동으로 주어진다. 따로 설정할 것이 없다.

## 5. 첫 배포

`develop` 을 `main` 에 머지하면 Actions 가 돈다. 수동으로 돌리려면 Actions 탭에서 `Deploy` → Run workflow.

첫 실행은 이미지가 없어 조금 오래 걸린다. 끝나면 `http://<EC2 IP>` 로 접속된다.

## 6. HTTPS (도메인을 샀다면)

도메인 A 레코드를 EC2 퍼블릭 IP 로 연결한 뒤:

```bash
cd ~/dalbit-tarot
docker run --rm \
  -v ./certbot/conf:/etc/letsencrypt \
  -v ./certbot/www:/var/www/certbot \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d dalbittarot.kr -d www.dalbittarot.kr \
  --email hyebin218@naver.com --agree-tos --no-eff-email
```

발급 후 `nginx/dalbit-tarot.conf` 에 443 서버 블록을 추가하고 80 은 443 으로 넘긴다.
인증서는 90일마다 만료되므로 갱신을 crontab 에 넣는다.

```
0 3 * * 1 cd ~/dalbit-tarot && docker run --rm -v ./certbot/conf:/etc/letsencrypt -v ./certbot/www:/var/www/certbot certbot/certbot renew --quiet && docker compose -f compose.prod.yaml restart nginx
```

## 7. 백업

RDS 를 쓰지 않으므로 백업은 직접 챙긴다. EC2 에 crontab 으로 넣는다.

```
0 4 * * * cd ~/dalbit-tarot && docker compose -f compose.prod.yaml exec -T mysql \
  mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction dalbit_tarot \
  | gzip > backup/dalbit-$(date +\%Y\%m\%d).sql.gz && find backup -name '*.sql.gz' -mtime +14 -delete
```

**EC2 디스크에만 두면 인스턴스가 죽을 때 같이 사라진다.** S3 동기화를 붙이는 것이 안전하다.

## 8. 롤백

이미지에 커밋 해시 태그가 붙어 있다.

```bash
cd ~/dalbit-tarot
sed -i 's|^APP_IMAGE=.*|APP_IMAGE=ghcr.io/yoohyebin/dalbit-tarot:<되돌릴 해시>|' .env
docker compose -f compose.prod.yaml up -d app
```

마이그레이션이 포함된 배포는 이 방법으로 되돌아가지 않는다. Flyway 는 되돌리기를 하지 않으므로
스키마를 바꾸는 변경은 이전 버전 앱도 뜨도록(컬럼 추가만, 삭제는 다음 배포에서) 설계한다.

## 9. 상태 확인

```bash
docker compose -f compose.prod.yaml ps
docker compose -f compose.prod.yaml logs -f app
curl -s localhost/actuator/health

# LLM 호출 비용·실패 확인
docker compose -f compose.prod.yaml exec mysql \
  mysql -udalbit -p dalbit_tarot -e \
  "select llm_model, status, count(*), avg(response_time_ms) from request_log group by 1,2;"
```

사용자 피드백은 `/admin/feedback` 에서 본다. `.env` 의 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 로 로그인한다.

## 이미지 직접 만들기

`Dockerfile` 은 빌드된 jar 을 담기만 한다. 컨테이너 안에서 Gradle 을 돌리면 배포판과
의존성을 매번 새로 받아 훨씬 오래 걸리고, CI 가 이미 같은 빌드를 하므로 두 번 하는 셈이 된다.

```bash
./gradlew clean bootJar
docker build -t dalbit-tarot:local .
```

## 남은 것

- **이미지 빌드는 아직 실제로 돌려보지 못했다.** 작업 환경에서 Docker Hub 접근이 막혀
  베이스 이미지를 받지 못했다. 첫 배포 때 Actions 로그를 확인할 것
- CloudWatch 지표·알람은 8단계에서 붙인다
- 인스턴스를 늘릴 때는 요청 제한과 해석 캐시를 Redis 로 옮겨야 한다. 지금은 앱 메모리에 있어 인스턴스마다 따로 센다
