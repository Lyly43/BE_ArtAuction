package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.RegisterRequest;
import com.auctionaa.backend.DTO.Request.UserRequest;
import com.auctionaa.backend.DTO.Response.AuthResponse;
import com.auctionaa.backend.DTO.Response.UserAVTResponse;
import com.auctionaa.backend.DTO.Response.UserResponse;
import com.auctionaa.backend.DTO.Response.UserTradeStatsResponse;
import com.auctionaa.backend.Entity.Artwork;
import com.auctionaa.backend.Entity.Invoice;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Entity.Wallet;
import com.auctionaa.backend.Jwt.JwtUtil;
import com.auctionaa.backend.Repository.ArtworkRepository;
import com.auctionaa.backend.Repository.InvoiceRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.Repository.WalletRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private ArtworkRepository artworkRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private final ModelMapper mapper;
    private final CloudinaryService cloudinaryService;

    // private RegisterResponse registerResponse;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(ModelMapper mapper, CloudinaryService cloudinaryService) {
        this.mapper = mapper;
        this.cloudinaryService = cloudinaryService;
    }

    public AuthResponse register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new AuthResponse(0, "Incorrect password bro!!!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(0, "Email already existed!!!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setPhonenumber(request.getPhone());

        user.setCreatedAt(LocalDateTime.now());
        user.setStatus(1);
        // ✅ generate ID trước khi save
        user.generateId();

        User savedUser = userRepository.save(user);

        // ✅ Tự động tạo ví cho user mới
        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.generateId(); // Generate ID cho wallet
        walletRepository.save(wallet);

        return new AuthResponse(1, "Register Successfully");

    }

    public Optional<User> login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findById(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public ResponseEntity<?> getUserInfo(String authHeader) {
        // 1) Validate token sớm
        if (authHeader == null || !jwtUtil.validateToken(authHeader)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid or missing JWT");
        }

        // 2) Lấy userId từ token (subject=userId)
        String userId = jwtUtil.extractUserId(authHeader);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid JWT (no subject)");
        }

        // 3) Tìm theo ID (tùy kiểu ID của bạn)
        // Nếu ID là String:
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        // Nếu ID là ObjectId (Mongo):
        // User user = userRepository.findById(new ObjectId(userId))
        // .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        UserResponse userResponse = mapper.map(user, UserResponse.class);
        return ResponseEntity.ok(userResponse);
    }

    public UserResponse updateUserById(String userId, UserRequest dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Không cho đổi email tại endpoint này (nếu FE vô tình gửi kèm)
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be changed at this endpoint");
        }

        if (StringUtils.hasText(dto.getUsername()))
            user.setUsername(dto.getUsername());
        if (StringUtils.hasText(dto.getPhonenumber()))
            user.setPhonenumber(dto.getPhonenumber());
        if (StringUtils.hasText(dto.getCccd()))
            user.setCccd(dto.getCccd());
        if (StringUtils.hasText(dto.getAddress()))
            user.setAddress(dto.getAddress());
        if (dto.getDateOfBirth() != null)
            user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null)
            user.setGender(dto.getGender());

        User saved = userRepository.save(user);
        return mapper.map(saved, UserResponse.class);
    }

    public UserAVTResponse updateAvatarById(String userId, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            if (user.getAvtPublicId() != null) {
                cloudinaryService.deleteByPublicId(user.getAvtPublicId());
            }
            var up = cloudinaryService.uploadUserAvatar(user.getId(), avatarFile);
            user.setAvt(up.getUrl());
            user.setAvtPublicId(up.getPublicId());

            User saved = userRepository.save(user);
            return mapper.map(saved, UserAVTResponse.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload avatar lỗi", e);
        }
    }

    public UserTradeStatsResponse getTradingStats(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Invoice> paidBuyerInvoices = invoiceRepository.findByUserIdAndPaymentStatus(user.getId(), 1);
        long purchasedCount = paidBuyerInvoices.size();
        BigDecimal totalSpent = sumInvoiceTotals(paidBuyerInvoices);

        long soldCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        if (user.getRole() == 2) {
            List<String> artworkIds = artworkRepository.findByOwnerId(user.getId()).stream()
                    .map(Artwork::getId)
                    .filter(Objects::nonNull)
                    .toList();

            if (!artworkIds.isEmpty()) {
                List<Invoice> soldInvoices = invoiceRepository.findByArtworkIdInAndPaymentStatus(artworkIds, 1);
                soldCount = soldInvoices.size();
                totalRevenue = sumInvoiceTotals(soldInvoices);
            }
        }

        return UserTradeStatsResponse.builder()
                .role(user.getRole())
                .purchasedCount(purchasedCount)
                .totalSpent(totalSpent)
                .soldCount(user.getRole() == 2 ? soldCount : 0)
                .totalRevenue(user.getRole() == 2 ? totalRevenue : BigDecimal.ZERO)
                .build();
    }

    private BigDecimal sumInvoiceTotals(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return invoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // (Tùy chọn) Giữ compatibility — nhưng KHÔNG dùng nữa
    @Deprecated
    public UserResponse updateUserByEmail(String email, UserRequest dto) {
        throw new ResponseStatusException(HttpStatus.GONE, "Endpoint updated: use userId from JWT");
    }

    @Deprecated
    public UserResponse updateAvatar(String email, MultipartFile avatarFile) {
        throw new ResponseStatusException(HttpStatus.GONE, "Endpoint updated: use userId from JWT");
    }
}
