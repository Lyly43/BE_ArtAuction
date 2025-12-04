package AdminBackend.Service;

import AdminBackend.DTO.Request.AddUserRequest;
import AdminBackend.DTO.Request.UpdateUserRequest;
import AdminBackend.DTO.Request.UserFilterRequest;
import AdminBackend.DTO.Response.AdminBasicResponse;
import AdminBackend.DTO.Response.AdminUserResponse;
import AdminBackend.DTO.Response.MonthlyComparisonResponse;
import AdminBackend.DTO.Response.UpdateResponse;
import AdminBackend.DTO.Response.UserStatisticsResponse;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Entity.Wallet;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MonthlyStatisticsService monthlyStatisticsService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Admin thêm người dùng mới
     */
    public ResponseEntity<AdminBasicResponse<AdminUserResponse>> addUser(AddUserRequest request) {
        // Validate email unique
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "Email already exists", null));
        }

        // Validate username unique
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AdminBasicResponse<>(0, "Username already exists", null));
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhonenumber(request.getPhonenumber());
        user.setCccd(request.getCccd());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        user.setAvt(request.getAvt()); // Set avatar
        user.setCreatedAt(LocalDateTime.now());
        user.generateId();

        User savedUser = userRepository.save(user);

        // Create wallet for new user with balance 0
        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.generateId();
        walletRepository.save(wallet);

        AdminUserResponse response = mapToAdminUserResponse(savedUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminBasicResponse<>(1, "User created successfully", response));
    }

    /**
     * Tìm kiếm người dùng theo ID, username, phonenumber, cccd
     */
    public ResponseEntity<List<AdminUserResponse>> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return ResponseEntity.ok(getAllUsersData());
        }

        List<User> users = userRepository.searchUsers(searchTerm.trim());
        List<AdminUserResponse> responses = users.stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy tất cả người dùng với đầy đủ thông tin
     */
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> responses = getAllUsersData();
        return ResponseEntity.ok(responses);
    }

    /**
     * Lấy thống kê người dùng (bao gồm so sánh tháng này vs tháng trước)
     */
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(1); // status = 1 là đang hoạt động
        long totalSellers = userRepository.countByRole(2); // role = 2 là seller
        long totalBlockedUsers = userRepository.countByStatus(2); // status = 2 là bị khóa

        // Lấy thống kê so sánh tháng này vs tháng trước
        MonthlyComparisonResponse monthlyComparison = monthlyStatisticsService.getMonthlyComparison("users", "createdAt");
        MonthlyComparisonResponse.MonthlyComparisonData compData = monthlyComparison.getData();
        
        // Tạo monthly comparison data
        UserStatisticsResponse.MonthlyComparison monthlyComp = new UserStatisticsResponse.MonthlyComparison(
            compData.getCurrentMonth().getTotal(),
            compData.getPreviousMonth().getTotal(),
            compData.getChange().getAmount(),
            compData.getChange().getPercentage(),
            compData.getChange().isIncrease(),
            compData.getCurrentMonth().getMonth(),
            compData.getPreviousMonth().getMonth()
        );

        UserStatisticsResponse statistics = new UserStatisticsResponse(
                totalUsers,
                activeUsers,
                totalSellers,
                totalBlockedUsers,
                monthlyComp
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * Thống kê so sánh tháng này vs tháng trước cho users
     */
    public ResponseEntity<MonthlyComparisonResponse> getUserMonthlyComparison() {
        MonthlyComparisonResponse response = monthlyStatisticsService.getMonthlyComparison("users", "createdAt");
        return ResponseEntity.ok(response);
    }

    /**
     * Admin cập nhật thông tin người dùng
     */
    public ResponseEntity<?> updateUser(String userId, UpdateUserRequest request) {
        // Tìm user theo ID
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "User not found with ID: " + userId, null));
        }

        User user = userOpt.get();

        // Validate email unique (nếu email thay đổi)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "Email already exists", null));
            }
            user.setEmail(request.getEmail());
        }

        // Validate username unique (nếu username thay đổi)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AdminBasicResponse<>(0, "Username already exists", null));
            }
            user.setUsername(request.getUsername());
        }

        // Cập nhật các trường khác
        if (request.getPhonenumber() != null) {
            user.setPhonenumber(request.getPhonenumber());
        }
        if (request.getCccd() != null) {
            user.setCccd(request.getCccd());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        // Cập nhật password nếu có
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Cập nhật avatar nếu có
        if (request.getAvt() != null) {
            user.setAvt(request.getAvt());
        }

        try {
            User updatedUser = userRepository.save(user);
            AdminUserResponse response = mapToAdminUserResponse(updatedUser);

            UpdateResponse<AdminUserResponse> successResponse = new UpdateResponse<>(
                1, "User updated successfully", response);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            UpdateResponse<Object> errorResponse = new UpdateResponse<>(0, 
                "Failed to update user: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Admin xóa người dùng
     */
    public ResponseEntity<AdminBasicResponse<Void>> deleteUser(String userId) {
        // Tìm user theo ID
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AdminBasicResponse<>(0, "User not found with ID: " + userId, null));
        }

        // Xóa wallet của user trước
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        if (walletOpt.isPresent()) {
            walletRepository.delete(walletOpt.get());
        }

        // Xóa user
        userRepository.delete(userOpt.get());

        return ResponseEntity.ok(new AdminBasicResponse<>(1, "User deleted successfully", null));
    }

    /**
     * Helper method: Lấy tất cả users và map sang AdminUserResponse
     */
    private List<AdminUserResponse> getAllUsersData() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lọc người dùng theo các tiêu chí
     * Tất cả các trường filter đều optional (null = bỏ qua filter đó)
     */
    public ResponseEntity<List<AdminUserResponse>> filterUsers(UserFilterRequest request) {
        // Nếu request null, trả về tất cả users
        if (request == null) {
            return getAllUsers();
        }
        
        List<User> allUsers = userRepository.findAll();
        
        List<User> filteredUsers = allUsers.stream()
                .filter(user -> {
                    // Filter by role (null = bỏ qua filter)
                    if (request.getRole() != null && user.getRole() != request.getRole()) {
                        return false;
                    }

                    // Filter by status (null = bỏ qua filter)
                    if (request.getStatus() != null && user.getStatus() != request.getStatus()) {
                        return false;
                    }
                    
                    // Filter by gender (null = bỏ qua filter)
                    // Nếu request có gender nhưng user không có gender thì bỏ qua user đó
                    if (request.getGender() != null) {
                        if (user.getGender() == null || !user.getGender().equals(request.getGender())) {
                            return false;
                        }
                    }
                    
                    // Filter by province (null hoặc empty = bỏ qua filter)
                    if (request.getProvince() != null && !request.getProvince().trim().isEmpty()) {
                        String province = request.getProvince().trim();
                        if (user.getAddress() == null || 
                            !user.getAddress().toLowerCase().contains(province.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by date of birth range (null = bỏ qua filter)
                    // dateOfBirthFrom: user.getDateOfBirth() >= dateOfBirthFrom
                    if (request.getDateOfBirthFrom() != null) {
                        if (user.getDateOfBirth() == null || 
                            user.getDateOfBirth().isBefore(request.getDateOfBirthFrom())) {
                            return false;
                        }
                    }
                    // dateOfBirthTo: user.getDateOfBirth() <= dateOfBirthTo
                    if (request.getDateOfBirthTo() != null) {
                        if (user.getDateOfBirth() == null || 
                            user.getDateOfBirth().isAfter(request.getDateOfBirthTo())) {
                            return false;
                        }
                    }
                    
                    // Filter by account creation date (null hoặc empty = bỏ qua filter)
                    if (request.getCreatedAtFilter() != null && !request.getCreatedAtFilter().trim().isEmpty()) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime filterDate = null;
                        
                        switch (request.getCreatedAtFilter().toLowerCase().trim()) {
                            case "last7days":
                                // Lấy users được tạo trong 7 ngày gần nhất: createdAt >= (now - 7 days)
                                filterDate = now.minus(7, ChronoUnit.DAYS);
                                if (user.getCreatedAt() == null || user.getCreatedAt().isBefore(filterDate)) {
                                    return false;
                                }
                                break;
                            case "thismonth":
                                // Lấy users được tạo trong tháng hiện tại: createdAt >= đầu tháng
                                filterDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                                if (user.getCreatedAt() == null || user.getCreatedAt().isBefore(filterDate)) {
                                    return false;
                                }
                                break;
                            default:
                                // Nếu giá trị không hợp lệ, bỏ qua filter này
                                break;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
        
        List<AdminUserResponse> responses = filteredUsers.stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Helper method: Map User entity sang AdminUserResponse
     */
    private AdminUserResponse mapToAdminUserResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setFullname(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhonenumber(user.getPhonenumber());
        response.setGender(user.getGender());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setAddress(user.getAddress());
        response.setCccd(user.getCccd());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAvt(user.getAvt()); // Set avatar
        response.setCreatedAt(user.getCreatedAt());

        // Lấy balance từ Wallet
        Optional<Wallet> walletOpt = walletRepository.findByUserId(user.getId());
        BigDecimal balance = walletOpt.map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        response.setBalance(balance);

        return response;
    }

}

