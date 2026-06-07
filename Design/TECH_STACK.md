# 2. Techstack công nghệ sử dụng trong hệ thống

## 2.1. ReactJS (cho admin_web)

### 2.1.1. Tổng quan
ReactJS là một thư viện JavaScript mã nguồn mở được phát triển bởi Meta (Facebook), chuyên dụng để xây dựng giao diện người dùng (UI) động, có tính tương tác và hiệu năng cao, đặc biệt cho các ứng dụng đơn trang (SPA - Single Page Application). Kiến trúc cốt lõi của React dựa trên mô hình Component-based (phát triển dựa trên thành phần), cho phép phân rã các giao diện phức tạp thành những mảnh giao diện nhỏ hơn, có tính đóng gói cao, độc lập và dễ dàng tái sử dụng. Bằng cách sử dụng cơ chế Virtual DOM (DOM ảo), React tối ưu hóa việc cập nhật giao diện bằng cách chỉ render lại những phần thay đổi thực tế trên màn hình thay vì tải lại toàn bộ cây DOM vật lý. Điều này giúp tăng đáng kể tốc độ phản hồi và nâng cao trải nghiệm người dùng đối với các ứng dụng Web quản trị vốn chứa nhiều bảng dữ liệu, bộ lọc và các luồng thao tác phức tạp.

### 2.1.2. Đặc điểm chính
Về mặt kỹ thuật, ReactJS sở hữu một số đặc điểm cốt lõi vô cùng mạnh mẽ. Trước hết là cú pháp JSX (JavaScript XML), cho phép các nhà phát triển viết mã cấu trúc HTML trực tiếp bên trong các khối mã JavaScript, tạo ra sự trực quan và liên kết chặt chẽ trong quá trình phát triển UI. Phiên bản React hiện đại áp dụng triệt để cơ chế React Hooks (như `useState`, `useEffect`, `useContext`, `useMemo`, `useCallback`) để quản lý trạng thái (state) và các tác vụ phụ (side effects) trực tiếp trong các Functional Components mà không cần sử dụng cấu trúc Class Components phức tạp như trước. Luồng dữ liệu một chiều (Unidirectional Data Flow) của React giúp luồng đi của dữ liệu trong ứng dụng trở nên dễ dự đoán, nhất quán và đơn giản hóa quá trình gỡ lỗi (debugging). Ngoài ra, hệ sinh thái phong phú đi kèm như React Router để định tuyến, Redux Toolkit để quản lý trạng thái toàn cục, và Axios hoặc React Query để giao tiếp API và quản lý cache dữ liệu giúp ReactJS trở thành một giải pháp vô cùng linh hoạt và dễ mở rộng.

### 2.1.3. Vai trò trong hệ thống
Trong hệ thống VitalAI, ReactJS đóng vai trò là công nghệ nền tảng để xây dựng phân hệ Trang Quản trị (admin-web). Đây là nơi các quản trị viên hệ thống, chuyên gia dinh dưỡng và lập trình viên vận hành thực hiện các công việc cốt lõi bao gồm: cập nhật và quản lý cơ sở dữ liệu thực phẩm chuẩn, điều chỉnh và phê duyệt công thức nấu ăn, giám sát các chỉ số sức khỏe tổng hợp của người dùng, cấu hình các tham số cho mô hình AI và theo dõi, đánh giá kết quả từ hệ thống RAG chatbot. Giao diện admin-web được xây dựng bằng ReactJS giúp trực quan hóa các bảng thống kê dữ liệu lớn, cung cấp biểu đồ báo cáo thời gian thực về hoạt động của hệ thống, và tối ưu hóa quy trình kiểm duyệt nội dung nhờ khả năng xử lý bất đồng bộ mượt mà và giao diện phản hồi nhanh nhạy.

---

## 2.2. Kotlin (cho mobile_client)

### 2.2.1. Tổng quan
Kotlin là ngôn ngữ lập trình tĩnh (statically typed) hiện đại chạy trên máy ảo Java (JVM), được phát triển bởi JetBrains và được Google chính thức công nhận là ngôn ngữ ưu tiên hàng đầu (Kotlin-first) cho việc phát triển ứng dụng di động Android kể từ năm 2017. Kotlin ra đời nhằm khắc phục những hạn chế cố hữu của Java bằng cách cung cấp một cú pháp ngắn gọn, an toàn hơn và tương thích hoàn toàn 100% với mã nguồn Java hiện có. Triết lý thiết kế của Kotlin tập trung vào khả năng bảo vệ ứng dụng khỏi các lỗi lập trình phổ biến (như lỗi tham chiếu Null) mà không làm suy giảm hiệu năng thực thi của hệ thống, đồng thời hỗ trợ cả hai mô hình lập trình hướng đối tượng (OOP) và lập trình hướng hàm (FP).

### 2.2.2. Đặc điểm chính
Đặc điểm kỹ thuật nổi bật nhất của Kotlin là tính năng "Null Safety" được tích hợp trực tiếp vào hệ thống kiểu (type system), giúp phát hiện và ngăn chặn các lỗi NullPointerException từ giai đoạn biên dịch. Kotlin cũng tối giản hóa lượng mã lặp (boilerplate code) thông qua các tính năng tiên tiến như Data Classes, Extension Functions, Type Inference và Smart Casts. Đặc biệt, Kotlin Coroutines cung cấp giải pháp lập trình bất đồng bộ (asynchronous) vô cùng gọn nhẹ và hiệu quả, cho phép thực hiện các tác vụ nặng như gọi API từ máy chủ hay đọc ghi cơ sở dữ liệu cục bộ SQLite (thông qua Room Database) mà không làm nghẽn luồng xử lý giao diện chính (UI Thread). Thêm vào đó, sự kết hợp giữa ngôn ngữ Kotlin và bộ công cụ khai báo giao diện Jetpack Compose mang lại quy trình phát triển UI linh hoạt, có tính tương tác cao, mượt mà và dễ bảo trì hơn rất nhiều so với mô hình XML truyền thống.

### 2.2.3. Vai trò trong hệ thống
Kotlin là ngôn ngữ chủ lực được sử dụng để xây dựng ứng dụng di động phía khách hàng (mobile_client). Ứng dụng này là kênh tương tác trực tiếp và duy nhất của người dùng cuối với hệ thống VitalAI. Thông qua ứng dụng Kotlin, người dùng có thể thực hiện chụp ảnh thực phẩm để nhận diện dinh dưỡng bằng camera, ghi chép nhật ký ăn uống hằng ngày (meal logs), theo dõi chỉ số thể chất (body metrics), đếm số bước chân và đồng bộ dữ liệu hoạt động thể chất. Nhờ vào cơ chế Kotlin Coroutines, các tác vụ tải ảnh món ăn lên máy chủ AI, đồng bộ dữ liệu ngoại tuyến, và gửi các thông báo nhắc nhở (push notifications) được xử lý chạy ngầm mượt mà, giúp duy trì trải nghiệm người dùng cực tốt, phản hồi nhanh nhạy ngay cả trên các thiết bị di động có cấu hình phần cứng hạn chế.

---

## 2.3. NestJS (cho backend)

### 2.3.1. Tổng quan
NestJS là một framework Node.js tiến tiến được thiết kế để xây dựng các ứng dụng phía máy chủ (backend) có cấu trúc chặt chẽ, khả năng mở rộng cao, đáng tin cậy và dễ bảo trì. Được viết bằng ngôn ngữ TypeScript (hỗ trợ hoàn chỉnh Javascript), NestJS kết hợp các nguyên lý thiết kế phần mềm tốt nhất từ cộng đồng lập trình như Lập trình hướng đối tượng (OOP), Lập trình hướng hàm (FP), và Lập trình phản ứng chức năng (FRP). Kiến trúc của NestJS chịu ảnh hưởng mạnh mẽ từ Angular, cung cấp một cấu trúc thư mục và mô hình phát triển được định nghĩa rõ ràng, giúp các đội ngũ phát triển dễ dàng duy trì tính nhất quán và cấu trúc mã nguồn sạch sẽ (Clean Architecture) khi hệ thống phình to theo thời gian.

### 2.3.2. Đặc điểm chính
NestJS cung cấp cơ chế Dependency Injection (DI) cực kỳ mạnh mẽ để quản lý sự phụ thuộc giữa các thành phần phần mềm, nâng cao khả năng kiểm thử (unit test). Hệ thống Module của NestJS phân tách ứng dụng thành các khối tính năng độc lập và có tính đóng gói cao, giúp tối ưu hóa khả năng tái sử dụng mã nguồn. NestJS hỗ trợ cả Express (mặc định) và Fastify làm máy chủ HTTP nền tảng, cho phép linh hoạt cấu hình theo nhu cầu hiệu năng. Framework này tích hợp sẵn các công cụ hữu ích như Guards (bảo mật/phân quyền), Interceptors (can thiệp luồng yêu cầu/phản hồi), Pipes (kiểm thử và biến đổi dữ liệu đầu vào thông qua class-validator) và Exception Filters (xử lý lỗi tập trung). NestJS cũng hỗ trợ xuất sắc các giao thức truyền thông đa dạng từ HTTP REST, GraphQL cho đến microservices dựa trên gRPC hay Apache Kafka.

### 2.3.3. Vai trò trong hệ thống
Trong hệ thống VitalAI, NestJS đóng vai trò là Máy chủ Trung tâm (Backend Gateway & Business Logic Core). NestJS là nơi điều phối toàn bộ luồng nghiệp vụ của hệ thống: xử lý đăng ký/đăng nhập người dùng (sử dụng Passport JWT và mã hóa bcrypt), quản lý dữ liệu lịch sử tập luyện và ăn uống, kiểm soát chuỗi ngày hoạt động liên tục (streaks), điều hành hệ thống thông báo đẩy (notifications), và thực hiện các tác vụ lập lịch định kỳ (cron jobs). Đồng thời, NestJS đóng vai trò là "cầu nối dữ liệu" trung chuyển, nhận yêu cầu từ ứng dụng Kotlin trên di động, tương tác với PostgreSQL để lưu trữ trạng thái, ghi nhận cache vào Redis, gửi các thông điệp sự kiện bất đồng bộ qua Apache Kafka, và gọi các API chuyên biệt (FastAPI) để xử lý các nghiệp vụ AI/RAG.

---

## 2.4. FastAPI (cho AI services)

### 2.4.1. Tổng quan
FastAPI là một framework web Python hiện đại, có hiệu năng cực cao, chuyên dùng để xây dựng các RESTful API dựa trên nền tảng Python 3.8+. FastAPI tận dụng các tiêu chuẩn mở như OpenAPI (để sinh tài liệu API tự động) và JSON Schema. Nhờ sử dụng nền tảng Starlette (cho phần web) và Pydantic (cho phần xác thực dữ liệu), FastAPI mang lại tốc độ thực thi tương đương với Go hoặc Node.js, vượt trội hoàn toàn so với các framework Python truyền thống như Django hay Flask. Triết lý thiết kế của FastAPI tập trung vào việc tăng tốc độ phát triển mã nguồn, giảm thiểu lỗi logic và dễ dàng tích hợp với các thư viện xử lý dữ liệu và học máy (machine learning) trong hệ sinh thái Python.

### 2.4.2. Đặc điểm chính
Điểm mạnh vượt trội của FastAPI là hỗ trợ lập trình bất đồng bộ native (`async`/`await`) giúp tối ưu hiệu năng xử lý hàng nghìn kết nối đồng thời với mức tài nguyên phần cứng tối thiểu. Quá trình kiểm tra kiểu dữ liệu đầu vào và đầu ra được thực hiện tự động bằng Pydantic, giúp loại bỏ các lỗi sai định dạng dữ liệu ngay từ tầng tiếp nhận yêu cầu. FastAPI tự động tạo ra trang tài liệu tương tác Swagger UI và Redoc giúp việc tích hợp và thử nghiệm API giữa các đội ngũ phát triển backend và AI trở nên vô cùng nhanh chóng. Ngoài ra, việc viết bằng Python giúp FastAPI tương thích trực tiếp và tối ưu nhất với các thư viện AI/ML hàng đầu như PyTorch, TensorFlow, Transformers (Hugging Face) và OpenCV.

### 2.4.3. Vai trò trong hệ thống
FastAPI được sử dụng làm nền tảng cho các dịch vụ Trí tuệ Nhân tạo (AI services), cụ thể là `calories_tracker_service` (dịch vụ tính toán calo qua ảnh bằng mô hình YOLOv8) và `rag-service` (dịch vụ chatbot tư vấn sức khỏe/dinh dưỡng thông minh). Dịch vụ RAG (`rag-service`) sử dụng FastAPI để nhận các yêu cầu trò chuyện, thực hiện quá trình nhúng dữ liệu (embedding) bằng mô hình local `multilingual-e5-base`, tìm kiếm ngữ cảnh thích hợp trong PostgreSQL qua pgvector, xây dựng prompt và gửi đến các mô hình ngôn ngữ lớn (LLM) như OpenAI hoặc Gemini để tạo câu trả lời dạng Server-Sent Events (SSE) để truyền phát câu trả lời trực tiếp thời gian thực cho người dùng di động. Dịch vụ phân tích calo nhận ảnh món ăn, gọi mô hình YOLOv8 để định vị, phân đoạn thực phẩm và tính toán thể tích cũng như lượng calo tương ứng một cách nhanh chóng.

---

## 2.5. PostgreSQL & pgvector (Database & Vector Search cho RAG)

### 2.5.1. Tổng quan
PostgreSQL là một hệ quản trị cơ sở dữ liệu quan hệ đối tượng mã nguồn mở mạnh mẽ, đáng tin cậy nhất hiện nay với lịch sử phát triển hơn 30 năm. PostgreSQL nổi tiếng với tính toàn vẹn dữ liệu cực cao (hỗ trợ đầy đủ các tính chất ACID), khả năng mở rộng mạnh mẽ và khả năng tương thích cao với các tiêu chuẩn SQL. `pgvector` là một extension (phần mở rộng) mã nguồn mở của PostgreSQL, cho phép lưu trữ trực tiếp các vector nhúng (vector embeddings) và thực hiện các truy vấn tìm kiếm tương đồng (semantic similarity search) trực tiếp trong lòng cơ sở dữ liệu quan hệ mà không cần phải triển khai một hệ thống Vector Database chuyên biệt (như Pinecone hay Qdrant).

### 2.5.2. Đặc điểm chính
PostgreSQL cung cấp khả năng xử lý các giao dịch phức tạp (transactions), hỗ trợ lập chỉ mục (indexing) đa dạng như B-Tree, GIST, GIN và khả năng mở rộng thông qua các cấu trúc hàm tự định nghĩa bằng nhiều ngôn ngữ. Khi tích hợp `pgvector`, cơ sở dữ liệu được trang bị thêm kiểu dữ liệu `vector` (hỗ trợ lên đến 16,000 chiều). Nó cho phép tính toán khoảng cách vector sử dụng các độ đo phổ biến như Khoảng cách Cosine (Cosine Distance), Khoảng cách Euclid (L2 Distance), và Tích vô hướng (Inner Product). `pgvector` hỗ trợ các giải thuật lập chỉ mục tìm kiếm xấp xỉ lân cận như HNSW (Hierarchical Navigable Small World) và IVFFlat, giúp duy trì tốc độ truy vấn mili-giây trên tập dữ liệu hàng triệu bản ghi vector.

### 2.5.3. Vai trò trong hệ thống
PostgreSQL đóng vai trò là kho lưu trữ dữ liệu chính (Source of Truth) cho toàn bộ hệ thống VitalAI, lưu trữ thông tin tài khoản người dùng, nhật ký ăn uống, nhật ký tập luyện, lịch sử cân nặng và cấu hình hệ thống. Đồng thời, schema `rag` trong cơ sở dữ liệu này được cấu hình phần mở rộng `pgvector` để lưu trữ các vector nhúng 768 chiều được sinh ra từ các tài liệu kiến thức y khoa, thực đơn dinh dưỡng và cẩm nang tập luyện. Khi người dùng hỏi chatbot AI, dịch vụ RAG sẽ gửi truy vấn tìm kiếm độ tương đồng cosine lên PostgreSQL thông qua `pgvector` để lấy ra các đoạn văn bản có ý nghĩa gần nhất với câu hỏi làm ngữ cảnh (context) cho LLM, đảm bảo câu trả lời của AI luôn chính xác và có độ tin cậy khoa học cao.

---

## 2.6. Redis (Caching & Token Blacklist)

### 2.6.1. Tổng quan
Redis (Remote Dictionary Server) là một kho lưu trữ cấu trúc dữ liệu lưu trong bộ nhớ (in-memory) mã nguồn mở, được sử dụng làm cơ sở dữ liệu, bộ nhớ đệm (cache) và môi giới tin nhắn (message broker). Bằng cách lưu trữ toàn bộ dữ liệu trên RAM thay vì ổ đĩa cứng, Redis mang lại tốc độ đọc ghi ở mức cực kỳ ấn tượng (dưới 1 mili-giây) với khả năng xử lý hàng trăm nghìn yêu cầu mỗi giây. Redis hỗ trợ nhiều cấu trúc dữ liệu khác nhau như Strings, Hashes, Lists, Sets, Sorted Sets với các truy vấn phạm vi, Bitmaps và HyperLogLogs.

### 2.6.2. Đặc điểm chính
Điểm cốt lõi của Redis là cơ chế đơn luồng (single-threaded event loop) giúp loại bỏ xung đột tranh chấp tài nguyên (race conditions) mà vẫn đạt hiệu năng cực cao nhờ non-blocking I/O. Redis hỗ trợ cơ chế lưu trữ dữ liệu xuống đĩa cứng (persistence) bất đồng bộ thông qua RDB (Redis Database snapshots) hoặc AOF (Append Only File) để tránh mất dữ liệu khi hệ thống gặp sự cố. Redis cũng hỗ trợ cơ chế thiết lập thời gian sống của khóa (Time-To-Live - TTL), tự động giải phóng các vùng nhớ hết hạn. Cơ chế Pub/Sub (Xuất bản / Đăng ký) tích hợp sẵn cho phép giao tiếp nhẹ nhàng giữa các dịch vụ.

### 2.6.3. Vai trò trong hệ thống
Trong VitalAI, Redis đóng vai trò then chốt trong việc tối ưu hóa hiệu năng và bảo mật hệ thống. Cụ thể, Redis được dùng để lưu trữ bộ nhớ đệm (cache) cho danh sách món ăn và thông tin thực phẩm dinh dưỡng vốn ít thay đổi nhưng có tần suất truy vấn rất cao từ phía người dùng di động, giúp giảm tải trực tiếp cho PostgreSQL. Về mặt bảo mật, Redis đóng vai trò làm kho lưu trữ danh sách các JWT bị thu hồi (token blacklist) thông qua cơ chế lưu khóa theo mã `jti` (JWT ID) có TTL khớp với thời gian hết hạn của token, phục vụ nghiệp vụ đăng xuất tức thì khỏi tất cả thiết bị. Ngoài ra, Redis còn được dùng để lưu mã OTP xác thực tạm thời và mã trao đổi đăng nhập một lần (Short-lived OAuth code) để đảm bảo an toàn tuyệt đối cho luồng đăng nhập mạng xã hội (Google, Facebook).

---

## 2.7. Apache Kafka (Message Broker & Event-Driven)

### 2.7.1. Tổng quan
Apache Kafka là một nền tảng truyền phát sự kiện phân tán (distributed event streaming platform) mã nguồn mở có khả năng xử lý hàng nghìn tỷ sự kiện mỗi ngày. Ban đầu được phát triển bởi LinkedIn và sau đó được hiến tặng cho Apache Software Foundation, Kafka được thiết kế theo mô hình hàng đợi log phân tán (distributed commit log) với hiệu năng thông lượng cực lớn, độ trễ thấp, có khả năng chịu lỗi cao và khả năng mở rộng theo chiều ngang xuất sắc thông qua việc phân vùng (partitioning).

### 2.7.2. Đặc điểm chính
Kiến trúc của Kafka dựa trên mô hình Pub/Sub (Publish/Subscribe) cải tiến, tổ chức các dòng sự kiện theo các Chủ đề (Topics). Mỗi topic được chia nhỏ thành nhiều Phân vùng (Partitions) phân tán trên nhiều máy chủ (Brokers), cho phép ghi và đọc song song. Dữ liệu trong Kafka được lưu trữ bền vững trên đĩa cứng và được sao chép (replicated) trên nhiều broker để đảm bảo không bị mất dữ liệu. Người tiêu dùng dữ liệu (Consumers) được quản lý theo Nhóm người tiêu dùng (Consumer Groups), cho phép phân tải xử lý các thông điệp một cách tự động và cân bằng. Kafka duy trì thứ tự thông điệp ở mức phân vùng và hỗ trợ lưu trữ vết lịch sử sự kiện dài hạn.

### 2.7.3. Vai trò trong hệ thống
Apache Kafka đóng vai trò là "Trục Xương sống Giao tiếp Bất đồng bộ" (Asynchronous Messaging Backbone) kết nối các dịch vụ trong hệ thống VitalAI theo kiến trúc hướng sự kiện (Event-Driven Architecture). Khi xảy ra các hành động quan trọng từ phía người dùng (ví dụ: đăng ký tài khoản thành công, hoàn thành onboarding, cập nhật chỉ số cơ thể, hoặc ghi nhật ký ăn uống), NestJS backend (Producer) sẽ phát một sự kiện tương ứng vào các Kafka Topics như `vitalai.user.events` hay `vitalai.activity.events`. Các consumer chuyên biệt sẽ tiêu thụ các sự kiện này bất đồng bộ: `MailerConsumer` gửi email chào mừng/báo cáo; `RagConsumer` cập nhật dữ liệu vector nhúng cá nhân hóa; `StreaksConsumer` cập nhật chuỗi ngày rèn luyện; và `NotificationsConsumer` kiểm tra các cột mốc mục tiêu calo/nước để gửi thông báo. Cơ chế này giúp cô lập lỗi giữa các dịch vụ, giải phóng NestJS backend khỏi các xử lý nặng nề và đảm bảo hệ thống hoạt động ổn định kể cả khi lượng truy cập tăng đột biến.

---

## 2.8. YOLOv8 (AI Object Detection & Instance Segmentation)

### 2.8.1. Tổng quan
YOLOv8 là phiên bản cải tiến mới nhất của dòng mô hình phát hiện đối tượng thời gian thực (real-time object detection) You Only Look Once nổi tiếng, được phát triển và phát hành bởi Ultralytics vào đầu năm 2023. YOLOv8 thiết lập một tiêu chuẩn mới về cả độ chính xác và tốc độ xử lý cho các bài toán thị giác máy tính phức tạp bao gồm: Phát hiện đối tượng (Object Detection), Phân đoạn thể hiện (Instance Segmentation), Phân loại hình ảnh (Image Classification) và Ước lượng tư thế (Pose Estimation). Mô hình được tối ưu hóa để có thể chạy mượt mà trên cả các thiết bị biên (Edge Devices) có năng lực tính toán hạn chế cho đến các máy chủ GPU hiệu năng cao.

### 2.8.2. Đặc điểm chính
YOLOv8 sử dụng kiến trúc mạng thần kinh tích chập (CNN) tiên tiến với khối xương sống (backbone) là CSPDarknet53 được cải tiến, tích hợp cơ chế C2f (cross-stage partial bottleneck với hai kết nối tắt) giúp tăng cường khả năng trích xuất đặc trưng hình ảnh ở nhiều quy mô khác nhau. YOLOv8 áp dụng phương pháp thiết kế "Anchor-free" (không sử dụng khung neo neo trước) giúp trực tiếp dự đoán tâm và kích thước của đối tượng, giảm thiểu số lượng hộp neo dư thừa và tăng đáng kể tốc độ hội tụ khi huấn luyện. Đặc biệt, biến thể Instance Segmentation (như `yolov8n-seg.pt`) cho phép không chỉ xác định hộp giới hạn (bounding box) mà còn vẽ ra mặt nạ pixel (mask) chính xác của từng đối tượng thực phẩm trong ảnh, đây là cơ sở cốt lõi để ước lượng thể tích vật lý của thức ăn.

### 2.8.3. Vai trò trong hệ thống
Trong VitalAI, mô hình YOLOv8 (cụ thể là mô hình phân đoạn `yolov8n-seg.pt` chạy trong dịch vụ FastAPI `calories_tracker_service`) đóng vai trò là "Đôi mắt AI" giúp tự động hóa quá trình ghi chép dinh dưỡng. Khi người dùng chụp ảnh bữa ăn của họ qua ứng dụng Kotlin di động, ảnh sẽ được gửi lên FastAPI. Tại đây, YOLOv8 tiến hành phân đoạn hình ảnh để nhận diện chính xác từng loại thực phẩm riêng biệt có trên đĩa (ví dụ: cơm, thịt bò, rau cải) và trích xuất diện tích bề mặt/mặt nạ pixel của chúng. Dữ liệu phân đoạn này sau đó được kết hợp với các thuật toán hình học để ước tính thể tích thực tế của từng món ăn, từ đó tra cứu cơ sở dữ liệu để quy đổi ra trọng lượng (gram) và tự động tính toán lượng Calorie cũng như các chất đa lượng (Carbs, Protein, Fat, Sodium, Sugar) tương ứng một cách chính xác mà không yêu cầu người dùng phải ước lượng hay nhập thủ công từng thành phần.
