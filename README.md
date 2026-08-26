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
- 訂單查詢與狀態流程：已付款、待出貨、已出貨、已完成、取消
- 賣方訂單管理、模擬物流單號唯一性、買方確認收貨與管理員查看全部訂單
- 管理員 Dashboard：會員、商品與訂單數量統計
- Swagger UI／OpenAPI：主要 API、DTO、驗證錯誤格式與 Cookie JWT 權限說明
- GitHub Actions：使用 Maven Wrapper 自動編譯與測試

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
| GET | `/api/v1/admin/dashboard` | 管理員查看會員、商品與訂單統計 |
| GET | `/api/v1/cart` | 查看自己的購物車 |
| POST | `/api/v1/cart/items` | 加入商品或累加數量 |
| PATCH | `/api/v1/cart/items/{productId}` | 修改購物車數量 |
| DELETE | `/api/v1/cart/items/{productId}` | 移除購物車明細 |
| DELETE | `/api/v1/cart` | 清空購物車 |
| POST | `/api/v1/checkout` | 模擬付款、建立訂單並扣庫存 |
| GET | `/api/v1/orders` | 買方查看自己的訂單 |
| GET | `/api/v1/orders/{id}` | 買方查看自己的訂單明細與商品快照 |
| POST | `/api/v1/orders/{id}/confirm` | 買方確認收貨，`SHIPPED` → `COMPLETED` |
| POST | `/api/v1/orders/{id}/cancel` | 買方取消尚未出貨訂單 |
| GET | `/api/v1/seller/orders` | 賣方查看自己商品產生的訂單 |
| GET | `/api/v1/seller/orders/{id}` | 賣方查看自己的訂單明細 |
| POST | `/api/v1/seller/orders/{id}/prepare-shipment` | 賣方將 `PAID` → `PENDING_SHIPMENT` |
| POST | `/api/v1/seller/orders/{id}/ship` | 賣方填寫模擬物流單號並出貨 |
| GET | `/api/v1/admin/orders` | 管理員查看全部訂單 |
| GET | `/api/v1/admin/orders/{id}` | 管理員查看訂單明細 |

商品列表支援 `keyword`、`category`、`sort`（`newest`、`price_asc`、`price_desc`）、`page`（從 1 開始）與 `size`（1～50）。商品建立後直接為 `PUBLISHED`，庫存為 0 時仍會展示，但會顯示售罄狀態供後續購物車判斷。

本 MVP 沒有另外建立 `SELLER` 角色；註冊後的 `MEMBER` 可以同時作為買方與賣方。能建立商品的會員就是 Demo 賣方，另一個會員則可用來展示買方流程。管理員只有初始管理員帳號，不能透過一般註冊建立。

瀏覽器展示頁：`http://localhost:8080/products.html`。登入後可用賣方帳號建立商品並上傳一張圖片。

Swagger 文件：`http://localhost:8080/swagger-ui/index.html`；OpenAPI JSON：`http://localhost:8080/v3/api-docs`。Swagger 使用 `ECOMMERCE_AUTH` Cookie 表示 JWT 登入狀態；所有寫入 API 仍需 CSRF token。

結帳測試資料：`mockAccountNumber` 使用 `MOCK_SUCCESS` 代表付款成功，使用 `MOCK_FAILURE` 代表付款失敗；每次新的結帳請求都要帶唯一的 `Idempotency-Key` Header。宅配運費為 TWD 100，超商取貨運費為 TWD 60。

訂單流程展示：結帳成功後訂單為 `PAID`，賣方呼叫 `prepare-shipment` 變成 `PENDING_SHIPMENT`，再以 `ship` 搭配例如 `MOCK-TRACK-001` 變成 `SHIPPED`，買方最後呼叫 `confirm` 變成 `COMPLETED`。已出貨訂單不可取消。

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

建議的面試展示帳號：管理員使用 `.env` 的 `INITIAL_ADMIN_*`；另外依上方範例註冊 `seller@example.com / Seller123!` 與 `buyer@example.com / Buyer123!` 兩個一般會員。這兩個帳號不是預先寫死在資料庫，避免 GitHub 下載後與既有資料衝突。

## Interview demo smoke test

以下流程可在乾淨資料庫重現。建議使用 Postman，因為它能自動保存登入 Cookie；每一個 POST、PUT、PATCH、DELETE 前，先呼叫 `GET /api/v1/auth/csrf`，並將回傳 token 放入 `X-XSRF-TOKEN` Header。

1. 以管理員帳號登入，查看 `GET /api/v1/admin/dashboard` 與 `GET /api/v1/admin/products`。
2. 註冊並登入一個賣方帳號，呼叫 `POST /api/v1/products` 建立商品，例如價格 `99.90`、庫存 `5`；需要時以 multipart 呼叫圖片上傳 API。
3. 註冊並登入另一個一般會員帳號，呼叫 `GET /api/v1/products` 找到商品，再用 `POST /api/v1/cart/items` 加入購物車。
4. 呼叫 `POST /api/v1/checkout`，Header 使用新的 `Idempotency-Key`，付款帳號填 `MOCK_SUCCESS`；成功後訂單狀態為 `PAID`。
5. 切換回賣方帳號，依序呼叫 `prepare-shipment` 與 `ship`，物流單號可填 `MOCK-TRACK-001`。
6. 切換回買方帳號，呼叫 `POST /api/v1/orders/{id}/confirm`，訂單會變成 `COMPLETED`。
7. 重新用相同 `Idempotency-Key` 呼叫結帳，確認回傳 `replayed: true` 且不會重複扣庫存。

PowerShell 使用 `curl.exe` 傳 JSON 時容易遇到引號轉義問題；若出現 `INVALID_REQUEST`，請改用 Postman，或確認 Body 是真正的 `raw JSON`，不是被 PowerShell 移除引號的字串。

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

GitHub Actions 會在 push 與 pull request 時執行相同的 `mvnw verify`。測試使用 H2 相容模式，不需要在 CI 另外啟動 SQL Server；正式啟動仍使用 SQL Server + Flyway。

## Future Improvements

- 賣方商品審核與違規處分規則
- 結算帳本、提領與退款流程
- 真實金流、物流與超商門市 API 串接
- Email 驗證、忘記密碼、重新設定密碼
- 商品分類、評論、優惠券與更完整的管理員報表
