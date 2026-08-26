# Ecommerce MVP

面試展示用的 Spring Boot 電商／市集 MVP，使用 Java 21、Spring Boot 4.1.1、SQL Server 2022、Flyway、JdbcTemplate、Spring Security 與 JWT Cookie。

## 目前完成

- Spring Boot 基礎環境、Actuator、HikariCP、Flyway
- SQL Server Docker Compose（主機 `1435` 對應容器 `1433`）
- 會員註冊、登入、登出、查詢自己資料、修改密碼
- JWT 存放於 HttpOnly Cookie，有效期 30 分鐘
- BCrypt 密碼雜湊、Email／username 小寫正規化與唯一性
- 一般會員與管理員權限分離
- 初始管理員由環境變數建立，一般註冊不能建立管理員
- 商品 CRUD、公開展示、搜尋／分類／排序／分頁
- 商品圖片上傳與本機檔案儲存（JPEG、PNG、WebP，單檔 5 MB，每商品最多 5 張）
- 商品所有權檢查與軟刪除，管理員可查看全部商品並軟刪除
- 會員購物車：同賣方限制、數量檢查、失效商品標記與清空

目前設定與範例帳密僅供本機面試展示；正式環境請使用不同密碼、HTTPS 與 `COOKIE_SECURE=true`。

## Prerequisites

- JDK 21
- Docker Desktop
- Git
- （選用）SQL Server Management Studio 22，用來查看資料庫

## First-time setup

1. 複製 `.env.example` 為 `.env`。`.env` 已被 Git 忽略，不要把正式密碼提交到 GitHub。
2. 開啟 Docker Desktop。
3. 在專案根目錄執行：

   ```powershell
   docker compose --env-file .env up -d
   docker compose --env-file .env ps
   ```

4. 在啟動 Spring Boot 的同一個 PowerShell 視窗設定環境變數（至少要設定以下值）：

   ```powershell
   $env:DB_PASSWORD="ChangeMe_App_123!"
   $env:JWT_SECRET="ChangeMe_Local_Jwt_Secret_At_Least_32_Characters_123!"
   $env:INITIAL_ADMIN_EMAIL="admin@example.com"
   $env:INITIAL_ADMIN_USERNAME="admin"
   $env:INITIAL_ADMIN_PASSWORD="ChangeMe_Admin_123!"
   $env:INITIAL_ADMIN_DISPLAY_NAME="System Administrator"
   $env:INITIAL_ADMIN_PHONE="0900000000"
   ```

5. 啟動 Spring Boot：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

啟動後 Flyway 會自動建立或更新資料表。之後若修改 Java 設定，請在終端機按 `Ctrl+C` 停止，再重新執行啟動指令。

## Member API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/v1/auth/csrf` | 取得 CSRF token 與 `XSRF-TOKEN` Cookie |
| POST | `/api/v1/auth/register` | 註冊會員 |
| POST | `/api/v1/auth/login` | Email 或 username 登入 |
| POST | `/api/v1/auth/logout` | 清除登入 Cookie |
| GET | `/api/v1/auth/me` | 查詢目前登入會員 |
| PUT | `/api/v1/auth/me` | 修改顯示名稱與電話 |
| PUT | `/api/v1/auth/me/password` | 使用目前密碼修改新密碼 |
| GET | `/api/v1/products` | 公開商品列表，預設每頁 12 筆 |
| GET | `/api/v1/products/{id}` | 公開商品詳情 |
| POST | `/api/v1/products` | 登入會員建立商品 |
| PUT | `/api/v1/products/{id}` | 商品建立者修改商品 |
| DELETE | `/api/v1/products/{id}` | 商品建立者軟刪除商品 |
| GET | `/api/v1/seller/products` | 查看自己的商品（含已軟刪除） |
| POST | `/api/v1/products/{id}/images` | 上傳商品圖片，multipart 欄位 `file` |
| DELETE | `/api/v1/products/{id}/images/{imageId}` | 刪除自己的商品圖片 |
| GET | `/api/v1/admin/products` | 管理員查看全部商品 |
| DELETE | `/api/v1/admin/products/{id}` | 管理員軟刪除商品 |
| GET | `/api/v1/admin/me` | 管理員測試 API |
| GET | `/api/v1/cart` | 查看自己的購物車 |
| POST | `/api/v1/cart/items` | 加入商品或累加數量 |
| PATCH | `/api/v1/cart/items/{productId}` | 修改購物車數量 |
| DELETE | `/api/v1/cart/items/{productId}` | 移除購物車明細 |
| DELETE | `/api/v1/cart` | 清空購物車 |
| POST | `/api/v1/checkout` | 模擬付款、建立訂單並扣庫存 |

商品列表支援 `keyword`、`category`、`sort`（`newest`、`price_asc`、`price_desc`）、`page`（從 1 開始）與 `size`（1～50）。商品建立後直接為 `PUBLISHED`，庫存為 0 時仍會展示，但會顯示售罄狀態供後續購物車判斷。

瀏覽器展示頁：`http://localhost:8080/products.html`。登入後可用賣方帳號建立商品並上傳一張圖片。

結帳測試資料：`mockAccountNumber` 使用 `MOCK_SUCCESS` 代表付款成功，使用 `MOCK_FAILURE` 代表付款失敗；每次請求都要帶不同的 `Idempotency-Key` Header。宅配運費為 TWD 100，超商取貨運費為 TWD 60。

所有寫入操作都需要先呼叫 `/api/v1/auth/csrf`，再把回傳 token 放到 `X-XSRF-TOKEN` Header。JWT Cookie 是 HttpOnly，前端 JavaScript 不能直接讀取它；CSRF Cookie 則可由前端讀取。

註冊 JSON 範例：

```json
{
  "email": "buyer@example.com",
  "username": "buyer01",
  "password": "Member123!",
  "displayName": "Demo Buyer",
  "phone": "0912345678"
}
```

## Health endpoints

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/health/readiness`
- `http://localhost:8080/actuator/health/liveness`

## SQL Server / SSMS connection

- Server：`localhost,1435`
- Authentication：SQL Server Authentication
- Database：`ecommerce`
- Application user：`ecommerce_app`

## Stop the database

```powershell
docker compose --env-file .env down
```

不要使用 `down -v`，以免刪除 SQL Server 的資料卷。

## Verification

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```
