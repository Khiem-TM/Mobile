# Admin Web Dashboard Summary

Ngay cap nhat: 2026-05-31

## Muc tieu

Xay dung mot client Web Dashboard rieng cho Admin Role de quan tri he thong VitalAI/Calories Tracker. Dashboard nay dung backend NestJS hien co lam API nen pham vi ban dau nen tap trung vao:

- Theo doi tong quan he thong: user, food, exercise, blog, activity/training, notification, AI/RAG.
- Quan ly du lieu dung chung: food global/public, exercise library, blog public.
- Moderation noi dung: blog, comment, food cho duyet, canh bao/nhac nho user.
- Quan tri user: tra cuu, xem chi tiet, khoa/mo khoa, xac minh email, ve sau co the them/sua/xoa/gan role.
- Van hanh: xem hang doi pending, loi tich hop, trang thai service, cache/event/notification.

## Tinh trang codebase hien tai

### Tong quan repo

- Hien chua co client Web/Admin frontend. Repo hien co:
  - `backend/`: NestJS + TypeORM + PostgreSQL, Swagger o `/api/docs`.
  - `mobile/`: Android app Compose/Hilt/Retrofit.
  - `rag-service/`: FastAPI RAG service.
  - `docker-compose.yml`: Postgres, Redis, Kafka, rag-service, backend.
- Backend khong dung global API prefix; route chay truc tiep nhu `/admin/stats`, `/foods`, `/dashboard`.
- Response duoc wrap boi `ResponseInterceptor`: `{ success, statusCode, data }`.
- CORS trong dev cho phep all origins; production doc can cau hinh `ALLOWED_ORIGINS` them domain admin web.

### Admin/auth hien co

Backend da co `AdminModule`:

- `POST /admin/auth/login`: dang nhap admin bang `ADMIN_EMAIL` va `ADMIN_PASSWORD` tu env, default hien la `admin@gmail.com` / `admin`.
- JWT payload admin co `sub: "admin"`, `role: "admin"`, het han 7 ngay.
- Cac route admin dung `JwtAuthGuard + RolesGuard + @Roles(UserRole.ADMIN)`.

Rui ro/can nang cap:

- Admin credential dang theo env hardcoded, chua phai tai khoan admin trong bang `users`.
- Chua co refresh token, logout, doi mat khau, 2FA, session/device management rieng cho admin.
- Can bo sung audit log cho cac hanh dong nguy hiem: delete, ban, verify, reject, warning.

### Dashboard/thong ke hien co

Admin hien co:

- `GET /admin/stats`: tra ve `totalUsers`, `activeUsers`, `totalFoods`, `pendingFoods`, `totalBlogs`, `totalExercises`.

User dashboard hien co:

- `GET /dashboard?date=YYYY-MM-DD`: tong hop nutrition/activity/body/streak/recent sessions theo user dang dang nhap.
- `GET /dashboard/weekly?weekStart=YYYY-MM-DD`.
- `GET /dashboard/monthly?year=YYYY&month=M`.

Khoang trong cho Admin Web:

- Chua co dashboard analytics cap he thong theo thoi gian: DAU/WAU/MAU, user growth, meal logs, training sessions, AI scans, chatbot usage, blog engagement.
- Chua co endpoint aggregate theo date range cho admin.
- Chua co system health endpoint cho DB/Redis/Kafka/RAG/AI service.

### Quan ly users hien co

Admin endpoints:

- `GET /admin/users?page&limit&search`: danh sach user, search theo email/display name.
- `GET /admin/users/:id`: chi tiet user kem health profile va 5 training session gan nhat.
- `PATCH /admin/users/:id/ban`: set `is_active=false`, gui notification system.
- `PATCH /admin/users/:id/unban`: set `is_active=true`, gui notification system.
- `PATCH /admin/users/:id/verify-email`: set `is_verified=true`.

Khoang trong so voi yeu cau them/xoa/sua user:

- Chua co admin create user.
- Chua co admin update profile/health profile cua user.
- Chua co admin delete/deactivate hard/soft theo policy ro rang.
- Chua co role management, reset password, force logout/revoke token.
- Chua co filter theo role, status, verified, created date.

### Quan ly food global/public hien co

Admin endpoints:

- `GET /admin/foods?page&limit&search`: list all foods.
- `GET /admin/foods/pending?page&limit`: list food pending verification.
- `POST /admin/foods`: tao food global, default `is_custom=false`, `is_verified=true`, `is_active=true`, gui notification den active users.
- `PATCH /admin/foods/:id`: sua food.
- `PATCH /admin/foods/:id/verify`: approve food.
- `PATCH /admin/foods/:id/reject`: soft-disable bang `is_active=false`.
- `DELETE /admin/foods/:id`: xoa vinh vien.

Food public/user endpoints hien co:

- `GET /foods`: search system food public.
- `GET /foods/explore`: explore dishes.
- User co the tao custom food qua `POST /foods`, data la `is_custom=true`, `is_verified=false`.

Luu y codebase:

- Pending food admin hien loc `is_custom=false, is_verified=false, is_active=true`. Trong khi custom food do user tao dang la `is_custom=true`, nen hang doi pending hien tai co the khong gom food user-created.
- AdminService thao tac truc tiep repository food, khong goi `FoodsService.invalidateFoodCache`, nen sau create/update/verify/reject/delete co nguy co cache public food bi stale trong Redis.
- Hard delete food co the nguy hiem neu da duoc tham chieu boi meal logs.

### Quan ly exercise hien co

Admin endpoints:

- `GET /admin/exercises?page&limit&search`.
- `POST /admin/exercises`: tao exercise, gui notification den active users.
- `PATCH /admin/exercises/:id`: sua exercise.
- `DELETE /admin/exercises/:id`: xoa exercise.

Exercise public/user endpoints:

- `GET /exercises`: filter by query.
- `GET /exercises/popular`.
- `GET /exercises/:id`.
- Upload/remove image exercise dang nam o `/exercises/:id/image/...`, yeu cau JWT nhung chua gan `RolesGuard` admin.

Khoang trong:

- Chua co soft delete policy thong nhat cho exercise.
- Chua co filter admin theo type, category, muscle group, difficulty, active status.
- Image management cua exercise nen dua vao admin role hoac tach route admin.

### Quan ly blog/moderation hien co

Admin blog endpoints dang nam trong `BlogModule`:

- `GET /admin/blogs?page&limit&status&tag`: list blog, hien chi chap nhan status `approved` hoac `draft`.
- `POST /admin/blogs`: tao blog public ngay.
- `PATCH /admin/blogs/:id`: sua blog.
- `DELETE /admin/blogs/:id`: xoa blog.

Blog public/user endpoints:

- `GET /blogs`, `GET /blogs/tags`, `GET /blogs/:id`.
- User create/update/delete blog o `/user/blogs`.
- Blog co like/comment; admin co the xoa comment bang `DELETE /blogs/:id/comments/:commentId` neu token role admin.

Luu y codebase:

- Entity `Blog` da co status `pending | approved | rejected | draft` va `rejectionReason`.
- DTO `RejectBlogDto`, `BatchBlogActionDto`, `BatchRejectBlogDto` da ton tai.
- Service hien tai user blog mac dinh auto-publish `approved` hoac `draft`; chua co flow pending approval.
- Admin list blog khong cho filter `pending/rejected`.
- Chua co endpoint approve/reject blog, batch approve/reject, warning user, report abuse, hide/unhide, pin/feature.
- Trong `backend/src/modules/blog/` co cac file controller/service duplicate o root module va trong `controllers/`/`services/`. `BlogModule` dang import ban trong `controllers/` va `services/`, nen can don dep de tranh nham lan khi phat trien.

### Notification, warning va audit

Hien co:

- Notification user: list, unread count, mark read, delete.
- Device token FCM registration.
- Admin create food/exercise va ban/unban user co tao notification system.
- Blog like/comment co event qua Kafka den notification consumer.

Can bo sung cho Admin Web:

- Endpoint admin gui warning den 1 user hoac nhom user.
- Mau warning cho blog/food/comment violation.
- Audit log cho moi action admin.
- Notification delivery status neu can van hanh.

### AI/RAG/Chatbot lien quan Admin

Hien co:

- Chatbot sessions/messages endpoints theo user.
- RAG service rieng, co health/report APIs trong `rag-service`.
- Food AI scan endpoint `POST /ai-scan/analyze`.

Khoang trong:

- Backend chua co admin endpoint tong hop usage AI scan/chatbot/RAG.
- Chua co man hinh admin xem AI errors, latency, provider fallback, safety events.

## De xuat pham vi chuc nang Admin Web

### 1. Admin Shell

- Login, token storage, route guard theo role admin.
- Layout gom sidebar, topbar, search global, notification/action log.
- Chuan response/error handler theo backend `{ success, statusCode, data }`.

### 2. Overview Dashboard

Ban dau dung `/admin/stats`, sau do mo rong:

- KPI cards: total/active users, foods, pending foods, blogs, exercises.
- Moderation queue: food pending, blog pending, comment/report pending.
- Growth charts: users, meal logs, training sessions, blog posts theo ngay/tuan/thang.
- Engagement: top foods, top exercises, top blogs, active users.
- System health: backend, DB, Redis, Kafka, RAG, AI food service.

### 3. User Management

- Table users: search, pagination, status, verified, role, created date.
- User detail: profile, health profile, goals, recent sessions, meal/activity summary, blogs/comments.
- Actions: ban/unban, verify email.
- Phase sau: create/edit/delete user, reset password, role change, force logout, export CSV.

### 4. Food Management

- Global food table: search/filter by category/type/verified/active/custom.
- Create/edit food form: nutrition per 100g, serving, image URLs, brand/category.
- Review queue: approve/reject food pending.
- Safer deletion: prefer soft delete; show dependency warning neu food da duoc dung trong meal logs.
- Cache invalidation backend can duoc bo sung de admin action phan anh ngay o mobile/public endpoints.

### 5. Exercise Management

- Exercise table: search/filter by type/category/muscle/difficulty/active.
- Create/edit exercise form: SPORT/GYM fields, MET, default sets/reps/duration/intensity, equipment, media.
- Image gallery/avatar management nen chuyen thanh admin-only.
- Soft delete/deactivate thay vi hard delete neu exercise da nam trong training sessions.

### 6. Blog Moderation & CMS

- Blog table: filter status/tag/author/date, sort by view/like/comment.
- Blog editor: title, thumbnail, tags, text/image blocks.
- Moderation queue:
  - pending/rejected/approved/draft statuses.
  - approve/reject with reason.
  - batch approve/reject/delete.
  - warning author.
- Comment moderation: list comments by blog/user, delete, warning.
- Report abuse flow neu mobile/web sau nay cho user report content.

### 7. Notification & Warning Center

- Gui warning den user tu user/blog/comment/food context.
- Template warning: spam, misinformation, inappropriate content, unsafe health advice.
- Lich su warning va audit.
- Optional: broadcast notification cho tat ca active users.

### 8. Operations

- Health page: backend, DB, Redis, Kafka, RAG, Cloudinary, Firebase.
- AI usage: ai-scan logs, chatbot sessions, RAG report/latency/error.
- Admin audit: ai lam gi, luc nao, target nao, payload diff co rut gon.

## Roadmap phat trien de xuat

### Phase 0 - Chot API contract va policy

- Xac dinh frontend stack: nen tach `admin-web/` rieng, co the dung React + Vite + TypeScript.
- Chot auth admin: tam dung `/admin/auth/login`, nhung can plan chuyen sang user role admin.
- Chot destructive policy: hard delete hay soft delete cho food/exercise/blog/user.
- Chot moderation policy: blog user-created co auto-publish hay pending approval.

### Phase 1 - Admin Web MVP dung endpoint hien co

- Scaffold web client.
- Login admin.
- Overview KPI bang `/admin/stats`.
- User list/detail + ban/unban/verify email.
- Food list/pending/create/update/verify/reject/delete.
- Exercise list/create/update/delete.
- Blog list/create/update/delete.

Ket qua phase nay da co gia tri van hanh co ban, khong can sua backend qua nhieu.

### Phase 2 - Dong bo backend cho CRUD/moderation dung yeu cau

- Them admin create/update/delete user.
- Them filters cho user/food/exercise/blog.
- Sua food pending logic neu muon review custom food cua user.
- Them cache invalidation khi admin mutate food.
- Them blog pending/approve/reject/batch/warning endpoints.
- Bao ve exercise media endpoints bang admin role.
- Them audit log entity/service/controller.

### Phase 3 - Analytics cap he thong

- Them admin analytics endpoints:
  - `/admin/analytics/users`
  - `/admin/analytics/nutrition`
  - `/admin/analytics/training`
  - `/admin/analytics/blogs`
  - `/admin/analytics/ai`
- Ho tro date range, granularity ngay/tuan/thang.
- Them top lists va trend data de ve chart.

### Phase 4 - Hardening

- Admin account trong DB, refresh token, revoke session, 2FA.
- RBAC chi tiet: super admin, content moderator, nutrition manager, support.
- Rate limit rieng cho admin auth.
- E2E tests cho admin flow.
- Production CORS/secure cookie/token policy.

## Cac viec backend can uu tien truoc/khi lam song song

1. Sua admin auth tu env credential sang user role admin hoac it nhat them password rotation/2FA.
2. Them audit log cho action admin.
3. Bo sung admin blog moderation dung status `pending/rejected` da co san trong entity.
4. Sua food pending queue de dung voi custom food do user tao, neu san pham can duyet food user-created.
5. Chuyen admin food/exercise delete sang soft delete hoac block delete khi co dependency.
6. Dam bao admin mutation invalidate Redis cache lien quan.
7. Admin-only hoa media upload/remove cua exercise.
8. Them endpoints analytics cap he thong thay vi chi co `/admin/stats`.

## Ket luan

Codebase backend da co nen mong cho Admin Web: auth admin, role guard, stats co ban, CRUD food/exercise, user ban/unban/verify, admin blog CMS. Tuy nhien no moi o muc quan tri du lieu co ban, chua du cho dashboard admin day du ve analytics, moderation, warning, audit va security.

Huong phat trien tot nhat la lam Admin Web MVP truoc tren cac endpoint da co, dong thoi bo sung backend theo cac khoang trong co rui ro cao: blog moderation, food pending, audit log, cache invalidation, soft delete va admin auth hardening.
