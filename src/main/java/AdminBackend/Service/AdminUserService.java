package AdminBackend.Service;

import AdminBackend.DTO.Request.AddUserRequest;
import AdminBackend.DTO.Request.UpdateUserRequest;
import AdminBackend.DTO.Response.AdminUserResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Admin thêm người dùng mới
     */
    public ResponseEntity<?> addUser(AddUserRequest request) {
        // Validate email unique
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Email already exists");
        }

        // Validate username unique
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Username already exists");
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User created successfully with ID: " + savedUser.getId());
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
     * Lấy thống kê người dùng
     */
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        long totalUsers = userRepository.count();
        long totalSellers = userRepository.countByRole(3); // role = 3 là seller
        long totalBlockedUsers = userRepository.countByStatus(2); // status = 2 là bị khóa

        UserStatisticsResponse statistics = new UserStatisticsResponse(
                totalUsers,
                totalSellers,
                totalBlockedUsers
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * Admin cập nhật thông tin người dùng
     */
    public ResponseEntity<?> updateUser(String userId, UpdateUserRequest request) {
        // Tìm user theo ID
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with ID: " + userId);
        }

        User user = userOpt.get();

        // Validate email unique (nếu email thay đổi)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        // Validate username unique (nếu username thay đổi)
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Username already exists");
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

        try {
            User updatedUser = userRepository.save(user);
            AdminUserResponse response = mapToAdminUserResponse(updatedUser);

            UpdateResponse<AdminUserResponse> successResponse = new UpdateResponse<>(
                true, "User updated successfully", response);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            UpdateResponse<Object> errorResponse = new UpdateResponse<>(false, 
                "Failed to update user: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Admin xóa người dùng
     */
    public ResponseEntity<?> deleteUser(String userId) {
        // Tìm user theo ID
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with ID: " + userId);
        }

        // Xóa wallet của user trước
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        if (walletOpt.isPresent()) {
            walletRepository.delete(walletOpt.get());
        }

        // Xóa user
        userRepository.delete(userOpt.get());

        return ResponseEntity.ok("User deleted successfully with ID: " + userId);
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
        response.setCreatedAt(user.getCreatedAt());

        // Lấy balance từ Wallet
        Optional<Wallet> walletOpt = walletRepository.findByUserId(user.getId());
        BigDecimal balance = walletOpt.map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        response.setBalance(balance);

        return response;
    }

}

