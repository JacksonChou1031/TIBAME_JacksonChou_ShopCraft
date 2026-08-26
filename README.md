# Ecommerce MVP

Java 21、Spring Boot 4.1.1、SQL Server 2022 與 Flyway 的面試展示用電商專案。

## Prerequisites

- JDK 21
- Docker Desktop
- Git

SSMS 22 是選用工具，可用來查看 Docker 中的 SQL Server；應用程式本身不依賴 SSMS。

## First-time setup

1. 複製環境變數範例並修改密碼。不要把 `.env` 提交到 GitHub。
2. 啟動 Docker Desktop。
3. 啟動 SQL Server：`docker compose --env-file .env up -d`。
4. 確認 SQL Server 與初始化服務完成：`docker compose --env-file .env ps`。
5. 在啟動 Spring Boot 的同一個 shell 載入資料庫環境變數。PowerShell 範例：`$env:DB_PASSWORD="ChangeMe_App_123!"`；Bash 範例：`export DB_PASSWORD='ChangeMe_App_123!'`。
6. 啟動 Spring Boot：Windows 使用 `.\mvnw.cmd spring-boot:run`；其他環境使用 `./mvnw spring-boot:run`。

`.env` 是 Docker Compose 使用的環境檔，不會自動成為 Spring Boot 的環境變數；請依照你的 shell 載入相同的 `DB_PASSWORD`。

## Useful endpoints

- Application health: `http://localhost:8080/actuator/health`
- Readiness: `http://localhost:8080/actuator/health/readiness`
- Liveness: `http://localhost:8080/actuator/health/liveness`

## SQL Server / SSMS connection

- Server: `localhost,1434`
- Authentication: SQL Server Authentication
- User: `sa` or `ecommerce_app`
- Database: `ecommerce`

只將 `sa` 用於資料庫初始化；Spring Boot 使用 `ecommerce_app`。

## Stop the database

```text
docker compose --env-file .env down
```

SQL Server 資料會保存在 Docker volume。只有在確定不需要本機資料時，才移除該 volume。

## Verification

```text
.\mvnw.cmd test
.\mvnw.cmd verify
```

啟動應用後，Readiness 必須顯示正常，且 Flyway 必須成功建立 `app_schema_metadata` 與自己的 migration history。
