package com.auctionaa.backend.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI cho dự án Art Auction Backend
 * 
 * HƯỚNG DẪN SỬ DỤNG SWAGGER:
 * 
 * 1. SAU KHI CHẠY ỨNG DỤNG, TRUY CẬP:
 *    - Swagger UI: http://localhost:8081/swagger-ui.html
 *    - Hoặc: http://localhost:8081/swagger-ui/index.html
 *    - API Docs (JSON): http://localhost:8081/v3/api-docs
 *    - API Docs (YAML): http://localhost:8081/v3/api-docs.yaml
 * 
 * 2. CÁCH SỬ DỤNG SWAGGER UI:
 *    - Mở trình duyệt và vào địa chỉ trên
 *    - Bạn sẽ thấy danh sách tất cả các API endpoints
 *    - Click vào một endpoint để xem chi tiết (request/response)
 *    - Click "Try it out" để test API trực tiếp trên Swagger
 *    - Điền thông tin vào form và click "Execute" để gửi request
 * 
 * 3. XÁC THỰC JWT TOKEN:
 *    - Ở đầu trang Swagger UI, bạn sẽ thấy nút "Authorize" (khóa màu xanh)
 *    - Click vào nút đó
 *    - Nhập JWT token của bạn vào ô "Value" (không cần gõ "Bearer", chỉ cần token)
 *    - Click "Authorize" và "Close"
 *    - Bây giờ bạn có thể test các API cần authentication
 * 
 * 4. CÁC API SẼ ĐƯỢC TỰ ĐỘNG HIỂN THỊ:
 *    - Tất cả các Controller có annotation @RestController sẽ được Swagger tự động scan
 *    - Các method có @GetMapping, @PostMapping, @PutMapping, @DeleteMapping sẽ hiển thị
 *    - Request/Response DTO sẽ được tự động hiển thị dựa trên các class bạn đã định nghĩa
 * 
 * 5. TÙY CHỈNH THÔNG TIN API (nếu cần):
 *    - Sửa các thông tin trong method openAPI() bên dưới
 *    - Thay đổi title, description, version, contact info...
 * 
 * LƯU Ý:
 * - Swagger chỉ hoạt động trong môi trường development (mặc định)
 * - Để tắt Swagger trong production, thêm vào application.properties:
 *   springdoc.api-docs.enabled=false
 *   springdoc.swagger-ui.enabled=false
 */
@Configuration
public class SwaggerConfig {

    /**
     * Cấu hình thông tin API và bảo mật JWT cho Swagger
     * 
     * @return OpenAPI object chứa metadata của API
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // Thông tin chung về API
                .info(new Info()
                        .title("Art Auction Backend API")
                        .description("""
                                API Documentation cho hệ thống Art Auction Backend
                                
                                Hệ thống này cung cấp các API cho:
                                - Quản lý người dùng (User Management)
                                - Quản lý tác phẩm nghệ thuật (Artwork Management)
                                - Quản lý phòng đấu giá (Auction Room Management)
                                - Quản lý đấu thầu (Bidding)
                                - Quản lý thanh toán (Payment)
                                - Quản lý hóa đơn (Invoice)
                                - Quản lý admin (Admin Management)
                                - Và nhiều tính năng khác...
                                
                                Để sử dụng các API cần authentication:
                                1. Đăng nhập để lấy JWT token
                                2. Click nút "Authorize" ở đầu trang Swagger UI
                                3. Nhập token vào (không cần "Bearer")
                                4. Click "Authorize" để xác thực
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Art Auction Team")
                                .email("support@artauction.com")
                                .url("https://artauction.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                
                // Cấu hình bảo mật JWT - cho phép Swagger UI test các API cần token
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                
                // Định nghĩa schema cho JWT Bearer token
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                Nhập JWT token của bạn vào đây.
                                                
                                                Cách lấy token:
                                                1. Gọi API /api/auth/login hoặc /api/admin/auth/login
                                                2. Copy token từ response
                                                3. Dán vào đây (không cần gõ "Bearer")
                                                
                                                Token sẽ được tự động thêm vào header Authorization
                                                với format: "Bearer {your-token}"
                                                """)));
    }
}

