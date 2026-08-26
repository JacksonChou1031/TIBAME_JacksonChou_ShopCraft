# 小型多賣方電商系統：技術規格書

版本：1.0  
定位：Java／Spring Boot 面試展示版  
主要測試 seam：以 REST API 為核心、由 Thymeleaf 頁面驅動的端到端業務流程

## Problem Statement

目前需要從零建立一個可於面試展示的小型電商系統。系統必須讓一般會員瀏覽商品、加入購物車並完成模擬付款，也要讓核准後的賣方建立與管理商品、處理出貨及查看結算；管理員則負責商品審核、賣方管理、配送模擬、提領審核與稽核。

系統預計服務數百名使用者，商品限定為實體商品。第一版不串接真實金流或物流，但必須透過清楚的交易、庫存、權限與狀態設計，展示可維護且可測試的 Spring Boot 後端能力。

## Solution

建立一個模組化單體應用，使用 Spring Boot 同時提供 REST API 與 Thymeleaf 操作頁面。核心流程如下：

`註冊／登入 → 賣方申請 → 管理員核准 → 商品建立與審核 → 加入購物車 → 模擬付款 → 條件式扣庫存 → 訂單履約 → 賣方結算`

系統以 SQL Server 2022 保存交易資料，Flyway 管理 schema，Docker Compose 提供可重現的資料庫環境，SSMS 22 作為資料庫檢視與管理工具。付款與物流均以可重現的 mock 行為展示，不保存或處理真實付款資料。

### 7 天 MVP 範圍

本文件同時記錄未來完整產品藍圖與本次面試版 MVP。7 天 MVP 優先於完整藍圖，採用以下簡化：

- 會員註冊後即可建立商品，不實作賣方申請與審核。
- 商品建立後即可展示，不實作商品審核流程。
- 保留 ADMIN 角色與輕量管理員後台，但只提供會員、商品、訂單的基本查看與管理，以及 Dashboard 統計。
- 不實作賣方違規次數、禁售懲處、結算 ledger、餘額與提領。
- 訂單只實作基本狀態流與賣方出貨；不實作真實或管理員模擬物流、退款與自動完成排程。
- MVP 以單一註冊會員同時具備買方與商品建立能力為前提，仍檢查商品所有權。

完整藍圖中的賣方審核、商品審核、懲處、結算、提領、退款與進階稽核，列為 Future Improvements，不阻塞 MVP 交付。

### 驗收條件

1. 新使用者可以註冊、登入、修改個人資料與密碼。
2. 會員可以提出賣方申請；管理員核准後，會員重新登入即可使用賣方功能。
3. 賣方可以建立商品與圖片，商品經管理員核准後才出現在前台。
4. 買方可以搜尋、篩選、排序商品，加入購物車並使用固定測試資料完成付款。
5. 結帳必須以單一交易完成訂單、付款紀錄、訂單明細快照、庫存扣除及購物車清除。
6. 同一商品最後一件庫存被多人同時購買時，最多只有一筆付款成功。
7. 賣方只能處理自己的訂單；買方只能查看自己的購物車與訂單；管理員才能執行管理功能。
8. 訂單可由賣方出貨、管理員模擬配送、買方確認收貨，或在送達三天後自動完成。
9. 訂單完成後，商品金額才進入賣方結算 ledger；賣方可以申請模擬提領。
10. Repository 可由 README、Docker Compose、環境變數範例與 Flyway migration 重現。

## User Stories

### 會員與身分

1. As a visitor, I want to browse approved and published products, so that I can understand what the marketplace offers before registering.
2. As a visitor, I want to register with an email, username, password, display name and phone, so that I can become a buyer.
3. As a member, I want to log in using either my email or username, so that I can access my account conveniently.
4. As a member, I want to modify my display name and phone, so that my profile stays current.
5. As a member, I want to change my password after entering my current password, so that I can maintain account security.
6. As a member, I want my email and username to remain unique, so that login identity is unambiguous.
7. As a member, I want to apply to become a seller with shop name, seller introduction and contact phone, so that I can list products.
8. As a member, I want to revise and resubmit a rejected seller application, so that I can correct missing or unsuitable information.
9. As a member, I want a pending seller application to prevent duplicate submissions, so that the review queue remains clean.
10. As a suspended member, I want my existing cart and orders retained, so that reactivation does not erase my history.

### 管理員與權限

11. As an administrator, I want to approve or reject seller applications, so that only approved members can sell.
12. As an administrator, I want to create additional administrator accounts, so that the system is not dependent on one initial administrator.
13. As an administrator, I want to reset another account's password using a one-time temporary password, so that account recovery can be demonstrated without implementing email recovery.
14. As an administrator, I want to suspend a member without deleting history, so that access can be revoked while audit and order relationships remain intact.
15. As an administrator, I want to disable an administrator account without disabling the last active administrator, so that the platform always remains manageable.
16. As an administrator, I want to view immutable audit records, so that sensitive decisions can be traced.
17. As an administrator, I want failed and rejected administrative operations recorded, so that abnormal behavior can be investigated.
18. As a buyer and seller account, I want buyer and seller capabilities to coexist, so that one member can participate in both sides of the marketplace.

### 商品與分類

19. As a seller, I want to create a single-SKU physical product with name, description, price, stock, category and images, so that I can submit it for sale.
20. As a seller, I want to upload JPEG, PNG or WebP images up to 5 MB each and five images per product, so that my listing is visually useful.
21. As a seller, I want to edit price and stock directly, so that daily operations do not require review for every quantity or price change.
22. As a seller, I want edits to name, description, category or images to require reapproval, so that published content remains moderated.
23. As a seller, I want to soft-delete my own product, so that it disappears from the storefront without breaking historical orders.
24. As a buyer, I want sold-out products to remain visible but not purchasable, so that product availability is clear.
25. As an administrator, I want to approve, reject, publish, unpublish and soft-delete products, so that marketplace rules can be enforced.
26. As an administrator, I want to provide a reason for rejection or removal, so that sellers know how to correct their listing.
27. As an administrator, I want to manage product categories, so that sellers use a consistent taxonomy.
28. As an administrator, I want to deactivate a category without deleting products that use it, so that historical classification remains intact.

### 商品展示與購物車

29. As a buyer, I want to search product name and description without case sensitivity, so that I can find products naturally.
30. As a buyer, I want to filter by category and sort by newest, lowest price or highest price, so that I can compare products.
31. As a buyer, I want 12 products per page and one-based page numbers, so that the interface is predictable.
32. As a buyer, I want to add a product to my cart only when its current stock is positive, so that obviously unavailable products cannot be selected.
33. As a buyer, I want repeated additions of the same product to increase one cart line quantity, so that the cart remains compact.
34. As a buyer, I want a cart to contain products from only one seller, so that one order has one fulfillment owner.
35. As a buyer, I want a product that becomes unavailable to remain in my cart with a clear warning, so that I can remove it knowingly.
36. As a buyer, I want the latest price recalculated at checkout, so that the order reflects the current listing rather than stale cart data.
37. As a buyer, I want my cart cleared only after successful payment, so that a failed payment can be retried.

### 結帳、付款與庫存

38. As a buyer, I want to choose convenience-store pickup or home delivery, so that I can select a suitable fulfillment method.
39. As a buyer, I want fixed mock shipping fees of TWD 60 for convenience-store pickup and TWD 100 for home delivery, so that checkout totals are deterministic.
40. As a buyer, I want to enter the required recipient data for my selected method, so that the seller can fulfill the order.
41. As a buyer, I want a fixed test payment input to deterministically succeed or fail, so that the interview demo is repeatable.
42. As a buyer, I want to retry a failed payment on the same order, so that transient payment failure does not require rebuilding the cart.
43. As a buyer, I want duplicate checkout requests to return the original result, so that double-clicking cannot create duplicate orders.
44. As a buyer, I want an unpaid order to expire after 24 hours, so that abandoned orders do not accumulate indefinitely.
45. As a buyer, I want a paid order cancelled before shipment to be refunded and its inventory restored, so that cancellation has consistent financial and inventory effects.
46. As a platform, I want conditional inventory updates inside a transaction, so that concurrent buyers cannot oversell a product.

### 訂單與履約

47. As a buyer, I want an order snapshot to preserve product name, unit price, quantity and shipping fee, so that later product edits cannot change history.
48. As a buyer, I want to cancel an order before shipment, so that I can stop an order that has not entered fulfillment.
49. As a seller, I want to cancel before shipment with a reason, so that I can handle stock or fulfillment problems honestly.
50. As a seller, I want to mark my own paid waiting-shipment order as shipped with a unique mock tracking number, so that fulfillment progress is traceable.
51. As an administrator, I want to simulate transit and delivery only through legal state transitions, so that the demo reflects a real workflow.
52. As a buyer, I want to confirm receipt only for my delivered order, so that completion and settlement are trustworthy.
53. As a buyer, I want an order to complete automatically three days after delivery if I do not confirm, so that orders do not remain stuck.
54. As a buyer, I want shipped orders to continue fulfillment and not be cancelled by an ordinary buyer cancellation, so that shipment commitments are respected.
55. As a platform, I want seller suspension to cancel only that seller's unshipped unfinished orders, so that already shipped orders continue safely.

### 結算與提領

56. As a seller, I want the product subtotal of a completed order credited to my mock balance, so that completed sales become payable.
57. As a seller, I want shipping fees excluded from my revenue, so that the settlement rule is explicit.
58. As a seller, I want immutable settlement entries, so that every credit, reservation, return and withdrawal is auditable.
59. As a seller, I want to request a full or partial withdrawal without a minimum amount, so that I can demonstrate payout behavior.
60. As a seller, I want only mock account name and mock account number stored, so that no real banking data is collected.
61. As an administrator, I want to approve or reject withdrawal requests with a reason, so that payout control is explicit.
62. As a platform, I want a withdrawal request to reserve available balance immediately, so that concurrent withdrawal requests cannot exceed funds.

### 可重現與面試展示

63. As a developer, I want SQL Server to start through Docker Compose, so that another person can reproduce the database.
64. As a developer, I want Flyway migrations to be versioned and immutable, so that schema history is reproducible.
65. As a reviewer, I want dev seed data and documented demo accounts, so that I can inspect the three viewpoints quickly.
66. As a reviewer, I want Swagger UI, README instructions, health checks and CI, so that the project demonstrates engineering discipline.

## Implementation Decisions

### 1. 產品邊界與架構

- 採用模組化單體，不拆分微服務。
- 業務模組為會員、商品、分類、購物車、訂單、付款、配送、結算、管理員與稽核。
- 所有模組先在同一個 Spring Boot 應用與同一個 SQL Server database 中運作。
- 面向面試展示，先保證單一賣方購物車與單一賣方訂單；不支援跨賣方合併結帳。
- Java 21、Spring Boot 4.1.1、Maven Wrapper。
- 主要 CRUD 使用 Spring Data JPA／Hibernate；統計報表或示範 SQL 控制力時，可額外使用 JdbcTemplate，但同一功能不混用兩套資料存取方式。
- SQL Server 2022 為資料庫引擎；SSMS 22 為管理工具；Docker Desktop／Docker Compose 提供本機資料庫。
- 使用 HikariCP 預設連線池設定，先不做數值調校。
- 使用 Flyway versioned migration 管理 schema；已執行的 migration 不修改，只以新版本補充。
- 所有交易資料表使用 SQL Server 的交易能力、foreign key 與明確索引；資料庫欄位使用 snake_case，Java 欄位使用 camelCase。
- 主鍵使用 BIGINT identity；對外的訂單與付款使用獨立且可讀的唯一編號。
- 金額使用 Java BigDecimal 與 SQL Server decimal(19,2)，幣別固定為 TWD。
- 資料庫時間保存 UTC；「自然月」業務判斷使用 Asia/Taipei。

### 2. Web、API 與安全

- 同一應用同時提供 `/api/v1` REST API 與 Thymeleaf 頁面。
- Controller 使用 Request／Response DTO，不直接暴露 JPA Entity。
- 輸入使用 Bean Validation；錯誤由全域例外處理統一成 code、message、timestamp 與 path。
- 使用語意化 HTTP status：成功查詢 200、新增 201、無內容 204、輸入錯誤 400、未登入 401、無權限 403、不存在 404、狀態或唯一性衝突 409、業務驗證錯誤 422。
- API 分頁對外從 1 開始，後端轉換為 Spring Data 的 0-based page；商品預設每頁 12 筆。
- 使用 springdoc-openapi 提供 Swagger UI。
- 使用 Spring Security 與 JWT；JWT 存於 HttpOnly Cookie，有效期 30 分鐘，登出時清除 Cookie，過期後重新登入。
- 因 JWT 存於 Cookie，啟用 CSRF；Thymeleaf 表單與 AJAX 寫入請求都必須提供 CSRF token。Cookie 使用 Secure 與適當 SameSite 設定。
- 不開放寬鬆 CORS；同源頁面直接呼叫 API，若未來分離前端，只允許明確來源。
- 密碼至少 8 碼，包含英文與數字；使用 BCrypt，絕不保存明文。
- 登入支援 Email 或 username；兩者皆 trim 後以不分大小寫規則保存與驗證，資料庫各自 unique。
- 會員核准成為賣方後必須重新登入，重新簽發含 SELLER 權限的 JWT。
- 所有寫入操作除角色外都檢查資源所有權；前端隱藏按鈕不能取代後端授權。

### 3. 身分與帳號狀態

- 一般註冊只建立一般會員／買方，不可自行註冊成管理員。
- 初始管理員由環境變數與啟動初始化機制建立，密碼使用 BCrypt；既有管理員可從後台新增管理員。
- 管理員帳號採軟停用，不刪除帳號或相關稽核紀錄；不可停用最後一位啟用中的管理員。
- 管理員可軟停用一般會員；停用後不可登入、不可新增訂單，但既有訂單與購物車保留，重新啟用後可恢復使用。
- 省略 Email 驗證與忘記密碼；管理員可發出一次性臨時密碼，會員首次登入必須修改。
- 登入成功、失敗、登出與管理員密碼重設都保存安全紀錄；不保存密碼、JWT 或 Cookie 內容。

### 4. 賣方申請與違規

- 賣方申請欄位為店舖名稱、賣方簡介與聯絡電話。
- 申請狀態為待審核、已核准、已拒絕；待審核不可重複提交，拒絕後可修改並重送。
- 賣方可在出貨前取消訂單，必須填寫原因；該取消算入自然月取消次數。
- 自然月內達到第 3 次賣方取消出貨時，立即禁止該賣方當月上架與販售任何商品，並下架其全部商品。
- 禁售到自然月結束；下個月恢復賣方資格，但原商品不自動恢復，必須重新送審。
- 被禁售的會員仍可作為買方瀏覽、下單與付款。
- 賣方停權或達到禁售條件時，只取消尚未出貨的未完成訂單；已出貨訂單繼續履約。被取消且已付款的訂單自動退款並恢復庫存。

### 5. 商品、圖片與分類

- 商品為單一規格、單一價格、單一庫存，不支援變體。
- 商品欄位為名稱、描述、價格、庫存、分類、圖片與狀態。
- 商品狀態包含草稿、待審核、已上架、已下架；另以 deleted_at 表示軟刪除。
- 新商品需通過管理員審核才能上架。名稱、描述、分類或圖片修改後立即隱藏並重新審核；價格與庫存可直接修改。
- 商品軟刪除不刪除圖片檔案、不影響訂單快照；一般查詢明確排除 deleted_at 非 NULL 的資料。
- 圖片只允許 JPEG、PNG、WebP，單檔不超過 5 MB，每商品最多 5 張；檔名使用 UUID。使用者上傳檔案不提交 GitHub。
- 圖片由後端 Controller 依商品狀態與權限提供；已上架商品可公開查看，下架／軟刪除商品只允許商品所屬賣方與管理員查看。
- 分類由管理員新增、修改與停用；已有商品使用的分類不可永久刪除。停用分類仍顯示於既有商品，但不可被新商品選用。
- 前台只回傳已審核、已上架、未軟刪除商品；售罄商品仍顯示但不可加入購物車。
- 商品搜尋只處理名稱與描述，不分大小寫；排序只接受 newest、price_asc、price_desc 等白名單值。

### 6. 購物車與結帳

- 未登入訪客只能瀏覽；加入購物車與結帳要求登入。
- 購物車只能包含同一賣方商品。
- 購物車明細只保存 product_id 與 quantity，不保存價格快照；重複商品以 `(member_id, product_id)` unique constraint 合併數量。
- 加入或更新數量必須是正整數；加入時檢查目前庫存，結帳時再次檢查。
- 購物車不保留庫存；商品下架、軟刪除或售罄後保留明細並標記不可購買，買方移除後才能結帳。
- 結帳總額為商品小計加固定運費，不含稅金、折扣、優惠券與平台服務費。
- 超商取貨運費 TWD 60，保存門市名稱、門市代碼、取貨人姓名與電話。
- 宅配運費 TWD 100，保存收件人姓名、電話與完整地址。
- 收件資料只保存於訂單，不建立會員地址簿。
- 成功付款後才清空購物車；付款失敗保留購物車。

### 7. 付款、訂單與庫存

- 使用 PaymentGateway 介面與 mock 實作；固定測試付款資料決定成功或失敗，前端不能直接傳 success flag。
- 每次付款嘗試都有唯一 PAY 編號與不可修改的付款紀錄；退款為獨立紀錄。
- 使用獨立 payment_status 與 order_status，避免付款結果與履約狀態混在同一欄位。
- 結帳 Service 使用單一 transaction，涵蓋訂單、訂單明細快照、付款紀錄、條件式扣庫存與購物車清除。
- 扣庫存採條件式更新：只有 stock >= quantity 時才允許扣除；更新筆數為 0 即回傳庫存衝突並使付款失敗。
- 結帳支援 idempotency key；以 member_id 與 key 建立 unique constraint，保存 24 小時並回傳原始結果。
- 訂單使用資料庫自增主鍵及獨立 ORD 編號；付款使用獨立 PAY 編號。
- 待付款／付款失敗訂單 24 小時後自動取消；付款失敗可在期限內建立新的付款嘗試。
- 訂單明細至少保存商品名稱、單價、數量、商品小計與運費快照；商品後續修改或軟刪除不影響已付款訂單。
- 買方取消只允許在出貨前；賣方取消只允許在出貨前且必須填原因。已付款取消建立退款並恢復庫存。
- 自動取消與付款、買方取消與賣方出貨等競爭操作都使用條件式狀態更新；只有第一個成功更新者生效，另一方回傳 409。

訂單履約狀態：

`PENDING_PAYMENT → PAID → WAITING_SHIPMENT → SHIPPED → IN_TRANSIT → DELIVERED → COMPLETED`

例外轉換：

- `PENDING_PAYMENT／PAYMENT_FAILED → CANCELLED`：逾期或未付款取消。
- `PAID／WAITING_SHIPMENT → CANCELLED`：買方、賣方或平台在出貨前取消；若已付款，另將 payment_status 設為 REFUNDED。
- `DELIVERED → COMPLETED`：買方確認，或送達三天後排程自動完成。
- `SHIPPED` 之後不可由買方取消或退貨。

- 賣方只能對自己的、已付款且 WAITING_SHIPMENT 的訂單標記出貨；必須提供全平台唯一的 mock 物流單號。
- 管理員只能依序模擬 SHIPPED 到 IN_TRANSIT，再到 DELIVERED；所有操作都記錄稽核。
- 使用 Spring Scheduler 每小時處理付款逾期與送達三天自動完成；自動結算以 order_id 與 entry_type unique constraint 防止重複入帳。

### 8. 結算與提領

- 訂單完成後才建立賣方商品金額入帳；運費不算賣方收入，沒有平台抽成。
- 使用 wallet 保存目前 available_balance 與 pending_withdrawal；使用 append-only settlement ledger 保存每筆入帳、預扣、退回與提領。
- 結算入帳金額為商品小計，不包含運費。
- 訂單完成前取消不產生賣方入帳。
- 建立提領申請時鎖定賣方 wallet，在 transaction 中從 available_balance 預扣並轉入 pending_withdrawal；不可超過可用餘額，也不設定最低提領金額。
- 提領狀態為待審核、已核准、已拒絕、已提領；管理員核准後完成 mock 提領。
- 拒絕提領必須填理由，建立退回餘額 ledger 並恢復 available_balance；不得修改或刪除原紀錄。
- 只保存模擬帳戶名稱與模擬帳號，不保存真實銀行資料。

### 9. 稽核、記錄與管理

- 所有管理員成功、失敗與被拒絕操作都建立不可修改、不可刪除的 audit log。
- 稽核紀錄包含操作者、角色、動作、目標類型、目標 ID、原因、操作前／後狀態、IP、User-Agent 與時間。
- 至少稽核賣方審核、商品審核／下架、會員停用／啟用、管理員帳號管理、配送模擬與提領審核。
- 只有管理員可查看稽核資料。
- 一般應用 log 使用 SLF4J／Logback 與 INFO、WARN、ERROR 分級；不記錄密碼、JWT、Cookie 或完整付款資料。
- 使用 Actuator；health endpoint 區分 Liveness 與 Readiness，Readiness 檢查 SQL Server 連線。敏感 Actuator endpoint 不公開。

### 10. API 資源範圍

- 公開：註冊、登入、商品列表／詳情、分類與已上架商品圖片。
- BUYER：自己的購物車、結帳、付款重試、訂單、取消與確認收貨。
- SELLER：自己的商品、自己的賣方訂單、標記出貨、結算與提領。
- ADMIN：賣方審核、會員管理、商品與分類審核、配送模擬、稽核與提領審核。
- 每個寫入 API 都在 Service 層驗證角色、資源所有權、目前狀態與輸入資料。

### 11. 設定、資料庫與重現

- Spring profile 區分開發與其他環境；資料庫密碼、JWT secret、初始管理員密碼從環境變數取得。
- Docker Compose 啟動 SQL Server 2022，容器內使用 1433、主機對外使用 1434（避開本機 SQL Server port）；Spring Boot 連線使用專用 ecommerce_app 帳號，SA 僅用於初始化。
- 開發環境使用 encrypt=true 與 trustServerCertificate=true；正式環境改用可信任憑證驗證。
- 初始化機制建立 database、application login／user 與必要權限，再由 Flyway 建立 schema。
- 開發 profile 提供測試會員、買賣雙方帳號、管理員、分類與商品資料；正式環境不自動建立展示資料。
- README 必須說明 JDK 21、Docker Desktop、資料庫啟動、環境變數、應用程式啟動、測試帳號、Swagger 與完整 Demo 流程。
- GitHub Actions 執行編譯、測試與格式檢查；整合測試使用 Testcontainers 啟動 SQL Server 2022。

## Testing Decisions

測試只驗證外部可觀察行為，不測試 private method、JPA 實作細節或特定 SQL 文字；測試名稱應描述業務情境與預期結果。

### 測試 seam

主要 seam 是 REST API／Service 的完整業務流程。能在高層驗證的行為，不拆成過多低層 mock；只有外部付款 gateway、檔案儲存與時間／排程等邊界使用替身。

### 必測模組與行為

- 會員：註冊唯一性、BCrypt、Email／username 登入、停用帳號、密碼修改與管理員重設。
- Security：JWT Cookie、CSRF、未登入 401、角色不足 403、資源非本人 403。
- 賣方申請：待審核禁止重複、拒絕可重送、核准後需要新 JWT 權限。
- 商品：CRUD、軟刪除、商品審核、核心欄位重新審核、圖片限制、分類停用與前台可見性。
- 購物車：同賣方限制、重複商品合併、失效商品阻止結帳、付款成功清空、付款失敗保留。
- 結帳：最新價格、固定運費、訂單快照、mock payment success／failure、idempotency、24 小時逾期。
- 庫存：庫存不足不付款、不扣負數、兩個並發請求購買最後一件時只有一筆成功。
- 訂單：合法與非法狀態轉換、買方／賣方取消界線、已出貨不可取消、出貨所有權與物流單號唯一性。
- 配送：管理員合法推進、買方確認收貨、送達三天自動完成。
- 結算：完成後才入帳、入帳不可重複、提領餘額鎖定、拒絕退回餘額、ledger append-only。
- 稽核：成功、失敗與拒絕管理員操作皆有紀錄，敏感資訊不進 log。

### 測試工具與層級

- Service：JUnit 5＋Mockito，驗證純業務規則與 gateway 邊界。
- Controller：MockMvc，驗證 DTO、HTTP status、JWT／CSRF 與權限。
- Repository：`@DataJpaTest`，驗證 unique constraint、軟刪除條件、索引相關查詢與 SQL Server 相容性。
- 結帳與庫存：Testcontainers＋SQL Server 2022，驗證 transaction rollback、條件式扣庫存與並發競爭。
- 排程：以可控 Clock 或直接呼叫排程邏輯測試時間邊界，不讓測試真的等待三天或 24 小時。
- CI：使用 Maven Wrapper，所有測試在 GitHub Actions 的乾淨環境執行。

### 端到端 Demo smoke test

1. 啟動 SQL Server container。
2. 啟動 Spring Boot，確認 Flyway 完成 migration 與 Readiness healthy。
3. 註冊會員並登入。
4. 提交賣方申請，管理員核准，會員重新登入。
5. 賣方建立商品並上傳圖片，管理員核准商品。
6. 買方搜尋商品、加入購物車，使用成功測試付款。
7. 驗證訂單快照、付款紀錄、庫存減少與購物車清空。
8. 賣方標記出貨，管理員模擬配送，買方確認收貨。
9. 驗證訂單完成與賣方 ledger 入帳。
10. 重複執行付款失敗、庫存不足、權限不足與重複 checkout 案例。

## Out of Scope

- 真實信用卡、轉帳、第三方金流與真實退款。
- 真實物流、物流 API、物流追蹤與門市資料同步。
- 面交配送方式。
- 跨賣方購物車、拆單與多筆運費結算。
- 商品規格／變體、批次庫存與預購。
- 優惠券、折扣、稅金、平台抽成與多幣別。
- 商品評價、留言、收藏與願望清單。
- 買賣雙方聊天、Email、簡訊與推播通知。
- Email 驗證、忘記密碼與 Refresh Token。
- 多組會員地址簿與訪客購物車。
- 完整退貨、換貨、爭議與出貨後退款流程。
- Redis、搜尋引擎與微服務拆分。
- 高可用、多區域部署與正式雲端基礎設施。
- 真實銀行帳戶、提領與法規／身分驗證。
- 前端 SPA；展示頁使用 Thymeleaf。

## Further Notes

- 本規格以面試展示為優先，但交易、權限、庫存與歷史資料規則不因為是 mock 功能而省略。
- 五天開發時，優先順序應為：會員／權限、商品審核、購物車／結帳／庫存、訂單履約，再完成結算、稽核、測試與文件。
- 若時間不足，結算提領、自然月禁售排程與完整稽核 UI 可先保留後端模型與測試，前端只完成可展示的主流程。
- 圖片採本機檔案儲存；上傳目錄不提交 GitHub，展示用範例圖片需另以公開且不敏感的 seed 資料提供。
- SQL Server 初始化腳本只在空白資料卷首次執行；變更初始化設定時，應以可恢復方式重新建立開發資料卷，不把資料卷內容提交到 Repository。
- 技術版本應固定，尤其是 Spring Boot、SQL Server image、Microsoft JDBC Driver、Flyway、springdoc 與 Testcontainers，以降低面試官環境差異。
- Issue tracker connector 與 `ready-for-agent` triage label 在目前工作區未提供；本文件可直接提交 GitHub，待執行 `/setup-matt-pocock-skills` 或接上對應 issue tracker 後，再發布並套用該標籤。
