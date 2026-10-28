# Course System — ghi chú ôn và làm PE

README này được tổng hợp từ source hiện tại của `CourseService`, `EnrollmentService` và `LearningGateway`. Hãy dùng nó làm bản đồ bắt đầu nhanh; nếu đề PE có yêu cầu khác, ưu tiên đề và schema được cấp.

## 1. Chiến lược làm PE nhanh, ít sai

### Thứ tự xử lý đề

1. **Đọc hết đề, tạo checklist**: service, endpoint/method, input-output, validation, HTTP status, paging/filter, quan hệ gọi service, database. Đánh dấu endpoint nào đi qua Gateway.
2. **Đối chiếu mẫu có sẵn**: CourseService là mẫu CRUD + paging; EnrollmentService là mẫu Feign + DTO chi tiết; Gateway là mẫu route + Swagger + Security. Tận dụng cấu trúc sẵn.
3. **Kiểm tra DB trước khi code**: tên bảng/cột, kiểu/nullability, khóa chính/identity, dữ liệu seed. `ddl-auto=none`, entity phải khớp schema; Hibernate không tự tạo bảng.
4. **Làm theo một luồng chức năng**: Entity → DTO → Repository → Mapper → Service/interface → Controller → Exception handler. Viết happy path trước, sau đó validation/not-found/duplicate.
5. **Xác minh theo tầng**: compile service vừa sửa; chạy service phụ thuộc trước; gọi endpoint trực tiếp; rồi qua Gateway. So request/response với đề, nhất là path, query param và HTTP status.
6. **Chừa thời gian cuối**: build, rà endpoint, port/URL, JSON mẫu, thay đổi thừa. Tránh refactor rộng khi chức năng chưa chạy.

### Chia thời gian tham khảo

- 10%: đọc đề, checklist, schema.
- 15%: kiểm tra project/config/database và build ban đầu.
- 50%: triển khai luồng chính theo thứ tự service → API.
- 15%: validation, exception, paging/filter, Feign/Gateway.
- 10%: build cuối và smoke test.

Với mỗi chức năng, tối thiểu kiểm tra thành công, ID không tồn tại, input sai; thêm duplicate, filter rỗng, page/size biên, lỗi service phụ thuộc nếu đề yêu cầu.

## 2. Kiến trúc và request flow

```text
Client → LearningGateway :8080
          ├─ /api/courses/**      → CourseService :8081 → SQL Server
          └─ /api/enrollments/**  → EnrollmentService :8082 → SQL Server
                                                   └─ Feign → CourseService :8081
```

Ba ứng dụng dùng chung database `MSS301_2026_PE`. Enrollment lưu `courseId` dạng scalar, không có JPA relationship; lúc tạo/cập nhật enrollment, service gọi CourseService xác nhận course. Gateway hiện `permitAll` mọi request và tắt CSRF.

| Service | Vai trò | Cổng | Điểm vào chính |
|---|---|---:|---|
| CourseService | CRUD/tìm kiếm khóa học | 8081 | `CourseController`, `CourseServiceImpl`, `CourseRepository` |
| EnrollmentService | Ghi danh; gọi Course bằng Feign | 8082 | `EnrollmentController`, `EnrollmentServiceImpl`, `EnrollmentRepository` |
| LearningGateway | Định tuyến API, gộp Swagger UI, Security | 8080 | `SecurityConfig`, `application.properties` |

## 3. Cấu trúc chung và quy ước

CourseService và EnrollmentService cùng chia theo tầng:

- `controller`: ánh xạ HTTP và gọi service.
- `service` / `service.impl`: nghiệp vụ và validation.
- `repository`: Spring Data JPA; truy vấn lọc dùng JPQL và `Pageable`.
- `entity`: ánh xạ bảng SQL Server.
- `dto`: request/response, không trả entity trực tiếp.
- `mapper`: MapStruct với `componentModel = "spring"`.
- `common`: `ApiResponseDTO`, `AppException`, `GlobalExceptionHandler`, `PageDTO`.
- `config`: OpenAPI metadata.

Quy ước đáng nhớ:

- `ApiResponseDTO` gồm `status`, `message`, `data`; Course controller hiện trả DTO/page trực tiếp ở một số endpoint, Enrollment bọc response bằng `ApiResponseDTO`.
- `GlobalExceptionHandler`: mã nghiệp vụ 2 → HTTP 406, 3 → 226, 4 → 404, 5 → 400; mặc định 500. Giữ mapping theo đề nếu đề quy định khác.
- Paging mặc định page `0`, size `10`, giới hạn size tối đa `100`; `PageDTO` gồm `page`, `size`, `totalPages`, `totalElements`, `first`, `last`, `content`.
- Course filter `title` và `status` hiện so khớp không phân biệt hoa thường nhưng là **exact match** (`=`), không phải `LIKE`.
- MapStruct/Lombok cần annotation processor trong POM của hai service nghiệp vụ. Khi thêm field mapping, xác minh mapper được generate sau compile.

## 4. API và logic theo service

### CourseService

Base path `/api/courses`; trực tiếp `http://localhost:8081`, qua Gateway `http://localhost:8080`.

| Method | Path | Chức năng |
|---|---|---|
| POST | `/api/courses` | Tạo khóa học; service gán `DRAFT` |
| GET | `/api/courses/{courseId}` | Lấy theo ID |
| PUT | `/api/courses/{courseId}` | Cập nhật; hiện gán lại `DRAFT` |
| GET | `/api/courses?page=0&size=10&title=...&status=...` | Lọc và phân trang |

`Course` ánh xạ bảng `course`: `courseId` identity, `courseCode`, `title`, `description`, `credits`, `status`, `startDate`, `endDate`, `instructorId`. Tín chỉ hợp lệ 1–10; service từ chối nếu `endDate` trước `startDate`. `CourseStatus` service dùng có `DRAFT` và `CLOSED`.

Service có `deleteCourse` kiểu soft delete (đổi status thành `CLOSED`), nhưng controller chưa expose DELETE endpoint.

### EnrollmentService

Controller hiện khai báo `/api/enrrollments` (thừa chữ `r`), còn Gateway route là `/api/enrollments/**`. Đây là sai khác cần xử lý khi tích hợp; Feign gọi Course tại `GET /api/courses/{courseId}`.

| Method | Path theo controller hiện tại | Chức năng |
|---|---|---|
| POST | `/api/enrrollments` | Tạo enrollment, xác nhận course qua Feign, đặt `ACTIVE` |
| GET | `/api/enrrollments/{enrollmentId}` | Lấy enrollment detail |
| PUT | `/api/enrrollments/{enrollmentId}` | Cập nhật và đặt `ACTIVE` |
| GET | `/api/enrrollments?page=0&size=10&studentEmail=...&status=...` | Lọc và phân trang |

`Enrollment` ánh xạ bảng `enrollments`: `enrollmentId` identity, `studentId`, `studentName`, `studentEmail`, `enrollmentDate`, `status`, `grade`, `courseId`. Status enum: `ACTIVE`, `COMPLETED`, `CANCELLED`. `cancelEnrollment` đổi trạng thái thành `CANCELLED` nhưng controller chưa expose endpoint cancel.

`EnrollmentDTO` yêu cầu student fields, ngày, status; `courseId` không có `@NotNull` nhưng service tạo bắt buộc. `EnrollmentDetailDTO` chứa `CourseDTO`, nhưng mapper hiện chỉ map entity sang DTO, không gọi Feign để lấp courseDTO.

### LearningGateway

- Port 8080; `/api/courses/**` → `http://localhost:8081`.
- `/api/enrollments/**` → `http://localhost:8082`.
- Gộp OpenAPI từ `/courses-service/v3/api-docs` và `/enrollments-service/v3/api-docs`.
- `SecurityConfig` hiện permit all và tắt CSRF. Matcher có `/course-servcie/v3/api-docs` (sai chính tả, không khớp `courses-service`); tuy vậy `anyRequest().permitAll()` cũng đang cho phép request đó.

## 5. Dependency theo service

Phiên bản lấy từ các `pom.xml`. Spring Boot parent `4.1.2`; ba project độc lập, mỗi service có Maven wrapper, không có root Maven aggregator.

### CourseService và EnrollmentService

| Dependency | Mục đích |
|---|---|
| `spring-boot-starter-data-jpa` | JPA/Hibernate, Spring Data repositories |
| `spring-boot-starter-validation` | Jakarta Bean Validation |
| `spring-boot-starter-webmvc` | REST API Spring MVC |
| `springdoc-openapi-starter-webmvc-ui:3.1.0` | OpenAPI/Swagger UI |
| `spring-boot-devtools` (runtime, optional) | Hỗ trợ phát triển |
| `mssql-jdbc` (runtime) | SQL Server JDBC driver |
| `lombok` (optional) | Sinh getter/builder/constructor |
| `spring-boot-starter-data-jpa-test` (test) | JPA test support |
| `spring-boot-starter-validation-test` (test) | Validation test support |
| `mapstruct:1.6.3` | Mapping Entity/DTO |
| `spring-cloud-starter-openfeign` | HTTP client; Enrollment dùng để gọi Course (Course cũng khai báo nhưng chưa cần theo luồng hiện tại) |

Compiler target Java 17; Spring Cloud BOM `2025.1.3`; compiler plugin `3.13.0`, Lombok `1.18.38`, `mapstruct-processor:1.6.3`, `lombok-mapstruct-binding:0.2.0`.

### LearningGateway

| Dependency | Mục đích |
|---|---|
| `spring-boot-starter-security` | Security filter chain |
| `spring-boot-starter-webflux` | WebFlux starter được khai báo |
| `spring-cloud-starter-gateway-server-webmvc` | Gateway trên Spring MVC |
| `spring-boot-starter-webmvc` | MVC web |
| `springdoc-openapi-starter-webflux-ui:3.1.0` | SpringDoc WebFlux UI được khai báo |
| `springdoc-openapi-starter-webmvc-ui:3.1.0` | Swagger UI MVC |
| `spring-boot-starter-security-test` (test) | Security test support |
| `spring-boot-starter-webflux-test` (test) | WebFlux test support |

Gateway dùng Java 21, Spring Cloud BOM `2025.1.3`. POM khai báo đồng thời WebFlux và MVC/WebMVC Gateway cùng hai SpringDoc UI starter; nếu gặp lỗi autoconfiguration/bean hoặc đề yêu cầu stack cụ thể, kiểm tra việc trộn MVC/WebFlux trước khi sửa logic route.

## 5.1. Giải thích chi tiết cấu hình `pom.xml`

`pom.xml` mô tả tọa độ project, phiên bản Java/framework, thư viện cần thiết và cách Maven compile/package. CourseService và EnrollmentService có cấu hình giống nhau về kỹ thuật; chỉ thay tọa độ/tên project. LearningGateway dùng Java 21 và bộ dependency khác.

### Khối cấu hình nguyên bản — CourseService và EnrollmentService

Các đoạn dưới đây có trong **cả hai POM**. Dòng tọa độ Maven (`artifactId`, `name`, `description`) khác service; dependency và build config trong các khối này giống nhau.

#### Properties

```xml
<properties>
    <java.version>17</java.version>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
    <spring-cloud.version>2025.1.3</spring-cloud.version>
</properties>
```

#### Dependency management — Spring Cloud BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>3.1.0</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${org.mapstruct.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

#### Build plugins và annotation processors

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>17</release>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.38</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

CourseService dùng các khối trên nhưng OpenFeign hiện chưa cần theo source; EnrollmentService dùng OpenFeign để gọi CourseService qua `CourseFeignClient`.

### Khối cấu hình nguyên bản — LearningGateway

#### Properties

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.1.3</spring-cloud.version>
</properties>
```

#### Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        <version>3.1.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>3.1.0</version>
    </dependency>
</dependencies>
```

#### Dependency management và build plugin

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

Các khối XML trên là nội dung rút từ POM tương ứng; phần tọa độ project và Spring Boot parent vẫn nằm ở đầu từng `pom.xml`. Gateway hiện khai báo cả WebFlux lẫn MVC/WebMVC Gateway, đúng như cấu hình trong POM.
### Cấu hình chung trong cả ba POM

| Cấu hình | Ý nghĩa |
|---|---|
| `<modelVersion>4.0.0</modelVersion>` | Phiên bản mô hình POM Maven, không phải phiên bản ứng dụng. |
| `spring-boot-starter-parent:4.1.2` | Parent cung cấp cấu hình build mặc định và dependency management của Spring Boot; do đó phần lớn starter không cần ghi version riêng. `<relativePath/>` yêu cầu Maven tìm parent trong repository thay vì tìm POM cha tương đối trong workspace. |
| `groupId=fu.mss301.se194638` | Namespace/tổ chức của project. |
| `artifactId` | Tên artifact Maven; dùng khi đặt tên package output/dependency. Mỗi service có artifactId riêng. |
| `version=0.0.1-SNAPSHOT` | Bản build đang phát triển; `SNAPSHOT` biểu thị chưa phải bản phát hành cố định. |
| `<name>` / `<description>` | Tên mô tả dễ đọc, không thay thế artifactId. |
| `<url/>`, `<licenses>`, `<developers>`, `<scm>` | Metadata scaffold hiện để trống; không ảnh hưởng luồng runtime của ứng dụng. |
| `spring-cloud-dependencies:2025.1.3` trong `dependencyManagement` | Import Spring Cloud BOM để quản lý version tương thích cho Gateway/OpenFeign. BOM quản lý version, không tự thêm thư viện; thư viện vẫn phải khai báo trong `<dependencies>`. |
| `spring-boot-maven-plugin` | Tích hợp Spring Boot với Maven, đóng gói ứng dụng chạy được (repackage) khi package. |

### CourseService và EnrollmentService

Hai POM giống nhau về dependencies và build. Chỉ khác `artifactId`, `name`, `description`. Cả hai đặt `<java.version>17</java.version>`, `org.mapstruct.version=1.6.3` và `spring-cloud.version=2025.1.3`.

| Dependency | Scope / version | Vì sao có trong POM |
|---|---|---|
| `spring-boot-starter-data-jpa` | Compile; version theo Boot parent | Entity, Hibernate, repository và thao tác database. |
| `spring-boot-starter-validation` | Compile; theo parent | Bean Validation như `@NotNull`, `@Min`, `@Max`. Lưu ý chỉ khai báo annotation không tự validate mọi request; controller thường cần `@Valid`. |
| `spring-boot-starter-webmvc` | Compile; theo parent | REST controller Spring MVC. |
| `springdoc-openapi-starter-webmvc-ui` | Version `3.1.0` | Sinh OpenAPI và giao diện Swagger cho MVC. |
| `spring-boot-devtools` | `runtime`, `optional=true` | Hỗ trợ vòng lặp phát triển; optional ngăn dependency này bị truyền như dependency bắt buộc cho project phụ thuộc. |
| `mssql-jdbc` | `runtime` | Driver JDBC SQL Server; cần khi chạy kết nối datasource, không cần import API driver trong source thông thường. |
| `lombok` | Compile, `optional=true`; processor version được chỉ rõ là `1.18.38` ở compiler plugin | Sinh getter/setter/constructor/builder trong lúc compile. `optional` tránh ép project downstream dùng Lombok. |
| `spring-boot-starter-data-jpa-test` | `test` | Các hỗ trợ kiểm thử JPA; không có trong classpath runtime. |
| `spring-boot-starter-validation-test` | `test` | Hỗ trợ test validation; không có trong classpath runtime. |
| `mapstruct` | Compile, version `${org.mapstruct.version}` | Annotation/API `@Mapper` được source sử dụng. |
| `spring-cloud-starter-openfeign` | Compile; version do Spring Cloud BOM quản lý | Tạo HTTP client Feign. Enrollment dùng `CourseFeignClient`; CourseService hiện chưa thấy client Feign trong source nhưng vẫn khai báo dependency và `@EnableFeignClients`. |

Trong `maven-compiler-plugin:3.13.0`, `<release>17</release>` đặt mức ngôn ngữ/API Java đích. `annotationProcessorPaths` cấu hình processor chạy lúc compile:

- `lombok:1.18.38`: xử lý annotation Lombok.
- `mapstruct-processor:${org.mapstruct.version}`: sinh implementation cho mapper.
- `lombok-mapstruct-binding:0.2.0`: giúp MapStruct nhận biết getter/setter được Lombok sinh ra.

Nếu thêm mapper mà implementation không được tạo, xem lỗi Maven compile trước; thường nguyên nhân nằm ở processor path hoặc mapping/type không khớp. Các dependencies test không thay thế việc chạy build/package của từng service.

### LearningGateway

Gateway POM đặt `java.version=21` (khác service nghiệp vụ dùng Java 17), import Spring Cloud BOM `2025.1.3`, dùng Spring Boot parent `4.1.2` và Spring Boot Maven Plugin. Không có MapStruct/JPA/SQL Server vì Gateway không truy cập database trực tiếp.

| Dependency | Scope / version | Tác dụng |
|---|---|---|
| `spring-boot-starter-security` | Compile; theo parent | Tạo security filter chain; hành vi permit/deny được cấu hình trong `SecurityConfig`, không được quyết định chỉ bởi dependency. |
| `spring-boot-starter-webflux` | Compile; theo parent | Reactive web stack. |
| `springdoc-openapi-starter-webflux-ui:3.1.0` | Compile | SpringDoc UI cho WebFlux. |
| `spring-cloud-starter-gateway-server-webmvc` | Compile; version theo Spring Cloud BOM | Gateway chạy trên Spring MVC; các route được khai báo trong `application.properties`. |
| `spring-boot-starter-security-test` | `test` | Tiện ích test Security. |
| `spring-boot-starter-webflux-test` | `test` | Tiện ích test WebFlux. |
| `spring-boot-starter-webmvc` | Compile; theo parent | MVC stack được khai báo thêm. |
| `springdoc-openapi-starter-webmvc-ui:3.1.0` | Compile | SpringDoc/Swagger UI cho MVC. |

POM hiện đồng thời khai báo WebFlux và MVC/WebMVC Gateway, cũng như SpringDoc UI cho cả hai stack. Đây là cấu hình thực tế cần biết khi đọc project; nếu PE yêu cầu xử lý lỗi khởi động hoặc chọn một web stack, kiểm tra sự tương thích và dependency tree theo stack đề bài trước khi thay đổi. Đừng xóa dependency chỉ vì có vẻ trùng khi chưa xác nhận Gateway đang khởi động bằng stack nào.

### Cách Maven resolve version và scope

- Dependency không có `<version>` thường được quản lý bởi Spring Boot parent hoặc Spring Cloud BOM đã import. Dependency ngoài BOM như SpringDoc và MapStruct được gắn version rõ ràng.
- `compile` là scope mặc định, dùng khi compile và runtime.
- `runtime` cần khi chạy nhưng không cần API đó để compile source.
- `test` chỉ có trong test compile/test runtime, không đi vào ứng dụng production.
- `optional=true` hạn chế việc dependency được truyền tiếp cho project phụ thuộc.
- Mỗi thư mục service là Maven project riêng; chạy `mvnw.cmd` trong đúng thư mục. Parent Boot không biến thư mục gốc thành multi-module build vì root không có `<packaging>pom</packaging>` và `<modules>`.
## 6. Cấu hình hiện tại (`application.properties`)

Ba service hiện dùng `src/main/resources/application.properties`. Các nội dung bên dưới là cấu hình dạng properties tương ứng.

### CourseService — `server.port=8081`

```properties
spring.application.name=CourseService
server.port=8081

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MSS301_2026_PE;encrypt=false;
spring.datasource.username=sa
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=none
services.course.url=http://localhost:8081

springdoc.swagger-ui.path=/swagger-ui
springdoc.api-docs.path=/v3/api-docs
```

### EnrollmentService — `server.port=8082`

```properties
spring.application.name=EnrollmentService
server.port=8082

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=MSS301_2026_PE;encrypt=false;
spring.datasource.username=sa
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=none

services.course.url=http://localhost:8081

springdoc.swagger-ui.path=/swagger-ui
springdoc.api-docs.path=/v3/api-docs
```

### LearningGateway — `server.port=8080`

```properties
spring.application.name=LearningGateway
server.port=8080

spring.cloud.gateway.server.webmvc.routes[0].id=course-service
spring.cloud.gateway.server.webmvc.routes[0].uri=http://localhost:8081
spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/api/courses/**

spring.cloud.gateway.server.webmvc.routes[1].id=enrollment-service
spring.cloud.gateway.server.webmvc.routes[1].uri=http://localhost:8082
spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=/api/enrollments/**

spring.cloud.gateway.server.webmvc.routes[2].id=course-service-docs
spring.cloud.gateway.server.webmvc.routes[2].uri=http://localhost:8081
spring.cloud.gateway.server.webmvc.routes[2].predicates[0]=Path=/courses-service/v3/api-docs
spring.cloud.gateway.server.webmvc.routes[2].filters[0]=SetPath=/v3/api-docs

spring.cloud.gateway.server.webmvc.routes[3].id=enrollment-service-docs
spring.cloud.gateway.server.webmvc.routes[3].uri=http://localhost:8082
spring.cloud.gateway.server.webmvc.routes[3].predicates[0]=Path=/enrollments-service/v3/api-docs
spring.cloud.gateway.server.webmvc.routes[3].filters[0]=SetPath=/v3/api-docs

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.disable-swagger-default-url=true
springdoc.swagger-ui.urls[0].name=Course Service
springdoc.swagger-ui.urls[0].url=/courses-service/v3/api-docs
springdoc.swagger-ui.urls[1].name=Enrollment Service
springdoc.swagger-ui.urls[1].url=/enrollments-service/v3/api-docs
```

**Database:** SQL Server cần chạy tại `localhost:1433`, database `MSS301_2026_PE` phải tồn tại, cập nhật tài khoản theo môi trường và schema PE. Không commit mật khẩu thật. Khi đổi Course port, cập nhật `services.course.url` của EnrollmentService và Gateway `uri` tương ứng.
## 7. Chạy và build nhanh

Chạy từng service trong thư mục tương ứng (Windows):

```powershell
cd CourseService
.\mvnw.cmd spring-boot:run
```

```powershell
cd EnrollmentService
.\mvnw.cmd spring-boot:run
```

```powershell
cd LearningGateway
.\mvnw.cmd spring-boot:run
```

Chạy service phụ thuộc trước: SQL Server → CourseService → EnrollmentService → Gateway. Gọi Course trực tiếp ở `http://localhost:8081`; Enrollment theo path controller hiện tại ở `http://localhost:8082/api/enrrollments`; Gateway ở `http://localhost:8080`. Swagger UI gateway: `/swagger-ui.html`.

Build độc lập từng module:

```powershell
cd CourseService; .\mvnw.cmd clean package
cd ..\EnrollmentService; .\mvnw.cmd clean package
cd ..\LearningGateway; .\mvnw.cmd clean package
```

## 8. Điểm dễ lỗi cần kiểm tra khi làm đề

Đây là quan sát từ source hiện tại để giúp định hướng debug; chỉ sửa theo yêu cầu PE:

1. **Enrollment path lệch Gateway**: controller là `/api/enrrollments`, route Gateway là `/api/enrollments/**`.
2. **Filter Enrollment lệch tên**: controller/service nhận `studentEmail` (service parameter được đặt tên `studentName`), repository JPQL lọc `e.studentEmail`. Xác nhận đề muốn email hay tên, đồng bộ xuyên suốt.
3. **Validation grade có thể NPE**: `Objects.requireNonNull(request.grade()).isBlank()` làm grade null ném lỗi dù grade có vẻ tùy chọn. Regex hiện tại cũng nên xem lại khi sửa.
4. **Enrollment detail có thể thiếu CourseDTO**: mapper không hydrate `courseDTO`; nếu response cần thông tin Course, service phải lấy qua Feign và lắp DTO theo đề.
5. **Create/update gán grade từ entity**: `setGrade(enrollment.getGrade())` không dùng grade trong request. Update cũng gọi `request.courseId().intValue()` mà chưa chặn null.
6. **Course update thiếu kiểm tra ID** trước khi dùng entity; duplicate check có thể từ chối chính courseCode hiện tại khi cập nhật.
7. **Nghiệp vụ chưa expose**: `CourseService.deleteCourse` và `EnrollmentService.cancelEnrollment` có ở service nhưng không có mapping tương ứng trong controller.
8. **Feign khai báo thừa ở CourseService**: POM có OpenFeign và app `@EnableFeignClients`, nhưng source không thấy Feign client dùng tại đây.
9. **Sai chính tả docs matcher**: `course-servcie` ở SecurityConfig khác `courses-service` ở Gateway properties; `permitAll` hiện làm matcher không gây chặn.
10. **Schema là nguồn sự thật**: `ddl-auto=none`; entity Course map bảng `course` số ít, Enrollment map `enrollments`.

## 9. Checklist trước khi nộp

- [ ] Đúng endpoint, HTTP method, path, query/body và response theo đề.
- [ ] Entity khớp table/column/identity; không trông chờ auto-DDL.
- [ ] Xử lý null, blank, range, enum/status, ngày tháng và duplicate.
- [ ] ID không tồn tại được kiểm tra trước update/delete.
- [ ] Paging zero-based, size và filter đúng yêu cầu.
- [ ] Feign URL/port/path khớp; có xử lý 404 và lỗi service phụ thuộc.
- [ ] Gọi trực tiếp từng service rồi mới kiểm tra qua Gateway.
- [ ] Cả ba module build được; Swagger hoạt động nếu đề yêu cầu.



