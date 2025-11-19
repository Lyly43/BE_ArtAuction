package com.auctionaa.backend.Service;

import com.auctionaa.backend.DTO.Request.KycSubmitRequest;
import com.auctionaa.backend.DTO.Response.KycSubmitResponse;
import com.auctionaa.backend.DTO.Response.KycVerifyResponse;
import com.auctionaa.backend.Entity.AuctionRegistration;
import com.auctionaa.backend.Entity.AuctionRoom;
import com.auctionaa.backend.Entity.KycVerification;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.AuctionRegistrationRepository;
import com.auctionaa.backend.Repository.AuctionRoomRepository;
import com.auctionaa.backend.Repository.KycVerificationRepository;
import com.auctionaa.backend.Repository.UserRepository;
import com.auctionaa.backend.VNPT.VnptKycClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final VnptKycClient vnpt;
    private final KycVerificationRepository kycRepo;
    private final AuctionRegistrationRepository regRepo;
    private final AuctionRoomRepository roomRepo;
    private final UserRepository userRepo;
    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kyc.face-match.threshold:0.75}")
    private double faceThreshold;

    @Value("${kyc.liveness.threshold:0.80}")
    private double liveThreshold;

    @Transactional
    public KycSubmitResponse submitAndVerify(KycSubmitRequest req) {

        String roomId = req.getRoomId() == null ? null
                : req.getRoomId().trim().replaceAll("\\p{C}", "");

        AuctionRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Nếu user đã là buyer (role = 1) và đã xác thực KYC (kycStatus = 1), cho phép
        // đăng ký đấu giá mà không cần KYC lại
        if (user.getRole() == 1 && user.getKycStatus() == 1) {
            log.info("User {} is already a buyer with verified KYC, skipping KYC verification for auction registration",
                    user.getId());

            // Tìm KYC verification gần nhất của user (profile verification)
            KycVerification existingKyc = kycRepo.findFirstByUserIdAndRoomIdIsNullOrderByCreatedAtDesc(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User has verified KYC but no KYC verification record found"));

            // Tạo AuctionRegistration với status VERIFIED, không cần verify lại
            AuctionRegistration reg = AuctionRegistration.builder()
                    .userId(user.getId())
                    .roomId(room.getId())
                    .kycId(existingKyc.getId())
                    .status("VERIFIED")
                    .note("User đã xác thực KYC trước đó, tự động duyệt")
                    .build();

            reg = regRepo.save(reg);

            // Gửi email thông báo đăng ký thành công
            try {
                String userName = user.getUsername();
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        userName,
                        reg.getId(),
                        room.getId());
                log.info("Registration success email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send registration success email to: {}", user.getEmail(), e);
            }

            return KycSubmitResponse.builder()
                    .registrationId(reg.getId())
                    .kycId(existingKyc.getId())
                    .status(1) // VERIFIED
                    .faceMatchScore(existingKyc.getFaceMatchScore())
                    .livenessScore(existingKyc.getLivenessScore())
                    .message("Đăng ký đấu giá thành công (bạn đã xác thực KYC trước đó)")
                    .build();
        }

        // 1) Convert Base64 -> Resource
        Resource frontRes = new ByteArrayResource(Base64.getDecoder().decode(req.getCccdFrontBase64())) {
            @Override
            public String getFilename() {
                return "front.jpg";
            }
        };
        Resource backRes = new ByteArrayResource(Base64.getDecoder().decode(req.getCccdBackBase64())) {
            @Override
            public String getFilename() {
                return "back.jpg";
            }
        };
        Resource selfieRes = new ByteArrayResource(Base64.getDecoder().decode(req.getSelfieBase64())) {
            @Override
            public String getFilename() {
                return "selfie.jpg";
            }
        };

        // 2) Upload -> hash (Minio hash của VNPT)
        String frontHash = vnpt.uploadFile(frontRes, "front.jpg", "ocr front", "auction kyc");
        String backHash = vnpt.uploadFile(backRes, "back.jpg", "ocr back", "auction kyc");
        String selfieHash = vnpt.uploadFile(selfieRes, "selfie.jpg", "selfie", "auction kyc");

        // 3) Tạo client_session + token
        String ts = String.valueOf(System.currentTimeMillis());
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientSession = "WEB_AUCTION_" + rnd + "_" + ts;
        String token = rnd;

        // 4) OCR front + back
        Map<String, Object> ocrFront = vnpt.ocrFront(frontHash, clientSession, token);
        Map<String, Object> ocrBack = vnpt.ocrBack(backHash, clientSession, token);

        Map<String, Object> frontObj = (Map<String, Object>) ocrFront.get("object");
        Map<String, Object> backObj = (Map<String, Object>) ocrBack.get("object");

        String fullName = frontObj == null ? null : (String) frontObj.get("name");
        String idNumber = frontObj == null ? null : (String) frontObj.get("id");
        // recent_location nằm ở mặt TRƯỚC trong tài liệu, nếu back không có thì
        // fallback từ front
        String addressFront = frontObj == null ? null : (String) frontObj.get("recent_location");
        String addressBack = backObj == null ? null : (String) backObj.get("recent_location");
        String address = addressBack != null ? addressBack : addressFront;

        String birthDay = frontObj == null ? null : (String) frontObj.get("birth_day");
        String issueDateS = backObj == null ? null : (String) backObj.get("issue_date");

        // 5) Card liveness (giấy tờ thật hay không)
        Map<String, Object> lv = vnpt.cardLiveness(frontHash, clientSession);
        Map<String, Object> lvObj = (Map<String, Object>) lv.get("object");
        String liveness = lvObj == null ? null : (String) lvObj.get("liveness");

        // API trả: face_swapping (boolean), fake_liveness (boolean), không có
        // fake_liveness_prob
        boolean fakeLiveness = false;
        boolean faceSwapping = false;
        if (lvObj != null) {
            Object fl = lvObj.get("fake_liveness");
            Object fs = lvObj.get("face_swapping");
            if (fl instanceof Boolean)
                fakeLiveness = (Boolean) fl;
            if (fs instanceof Boolean)
                faceSwapping = (Boolean) fs;
        }

        double liveScore = fakeLiveness ? 0.0 : 1.0;
        boolean livenessPassed = "success".equalsIgnoreCase(liveness)
                && liveScore >= liveThreshold
                && !faceSwapping;

        // 6) Face compare (so sánh khuôn mặt trên CCCD với selfie)
        // Theo tài liệu VNPT: response có object.prob (float) và object.msg
        // ("MATCH"/"NOMATCH")
        Map<String, Object> faceCompareResult = vnpt.faceCompare(frontHash, selfieHash, clientSession, token);
        Map<String, Object> fmObj = (Map<String, Object>) faceCompareResult.get("object");

        Double faceScore = null;
        String faceMatchMsg = null;
        if (fmObj != null) {
            // Parse prob (tỷ lệ khớp, ví dụ: 58.26)
            Object probObj = fmObj.get("prob");
            if (probObj instanceof Number) {
                faceScore = ((Number) probObj).doubleValue();
            } else if (probObj instanceof String) {
                try {
                    faceScore = Double.parseDouble((String) probObj);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse face prob: {}", probObj);
                }
            }
            // Parse msg ("MATCH" hoặc "NOMATCH")
            Object msgObj = fmObj.get("msg");
            if (msgObj instanceof String) {
                faceMatchMsg = (String) msgObj;
            }
        }

        // Face match passed nếu: prob >= threshold VÀ msg = "MATCH"
        boolean faceMatchPassed = faceScore != null
                && faceScore >= faceThreshold
                && "MATCH".equalsIgnoreCase(faceMatchMsg);

        // 7) Tổng hợp kết quả
        boolean passed = livenessPassed && faceMatchPassed;

        // 8) Parse ngày
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dob = null, issueDate = null;
        try {
            if (birthDay != null && birthDay.contains("/"))
                dob = LocalDate.parse(birthDay, f);
        } catch (Exception ignored) {
        }
        try {
            if (issueDateS != null && issueDateS.contains("/"))
                issueDate = LocalDate.parse(issueDateS, f);
        } catch (Exception ignored) {
        }

        // 9) Lưu raw JSON từ vendor
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(Map.of(
                    "ocrFront", ocrFront,
                    "ocrBack", ocrBack,
                    "liveness", lv,
                    "faceCompare", faceCompareResult));
        } catch (Exception e) {
            rawJson = "{}";
        }

        KycVerification kyc = KycVerification.builder()
                .userId(user.getId())
                .roomId(room.getId())
                .cccdFrontUrl(frontHash)
                .cccdBackUrl(backHash)
                .selfieUrl(selfieHash)
                .fullName(fullName)
                .cccdNumber(idNumber)
                .addressOnCard(address)
                .dateOfBirth(dob)
                .issueDate(issueDate)
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .passed(passed)
                .rawVendorResponse(rawJson)
                .status(passed ? 1 : 0)
                .build();

        kyc = kycRepo.save(kyc);

        AuctionRegistration reg = AuctionRegistration.builder()
                .userId(user.getId())
                .roomId(room.getId())
                .kycId(kyc.getId())
                .status(passed ? "VERIFIED" : "REJECTED")
                .note(null)
                .build();

        reg = regRepo.save(reg);

        // 10) Gửi email nếu xác thực thành công
        if (passed) {
            try {
                String userName = fullName != null ? fullName : user.getUsername();
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        userName,
                        reg.getId(),
                        room.getId());
                log.info("Registration success email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send registration success email to: {}", user.getEmail(), e);
                // Không throw exception để không ảnh hưởng đến response
            }
        }

        return KycSubmitResponse.builder()
                .registrationId(reg.getId())
                .kycId(kyc.getId())
                .status(kyc.getStatus())
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .message(passed ? "Đăng ký đã được xác thực thành công" : "Xác thực thất bại")
                .build();
    }

    /**
     * Đăng ký đấu giá với file ảnh (MultipartFile)
     * Tự động skip KYC nếu user đã là buyer (role = 1) và đã xác thực KYC
     * (kycStatus = 1)
     */
    @Transactional
    public KycSubmitResponse submitAndVerifyWithFiles(String userId, String roomId, MultipartFile cccdFront,
                                                      MultipartFile cccdBack, MultipartFile selfie) {
        String normalizedRoomId = roomId == null ? null : roomId.trim().replaceAll("\\p{C}", "");

        AuctionRoom room = roomRepo.findById(normalizedRoomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + normalizedRoomId));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Nếu user đã là buyer (role = 1) và đã xác thực KYC (kycStatus = 1), cho phép
        // đăng ký đấu giá mà không cần KYC lại (không cần gửi ảnh)
        if (user.getRole() == 1 && user.getKycStatus() == 1) {
            log.info("User {} is already a buyer with verified KYC, skipping KYC verification for auction registration",
                    user.getId());

            // Tìm KYC verification gần nhất của user (profile verification)
            KycVerification existingKyc = kycRepo.findFirstByUserIdAndRoomIdIsNullOrderByCreatedAtDesc(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User has verified KYC but no KYC verification record found"));

            // Tạo AuctionRegistration với status VERIFIED, không cần verify lại
            AuctionRegistration reg = AuctionRegistration.builder()
                    .userId(user.getId())
                    .roomId(room.getId())
                    .kycId(existingKyc.getId())
                    .status("VERIFIED")
                    .note("User đã xác thực KYC trước đó, tự động duyệt")
                    .build();

            reg = regRepo.save(reg);

            // Gửi email thông báo đăng ký thành công
            try {
                String userName = user.getUsername();
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        userName,
                        reg.getId(),
                        room.getId());
                log.info("Registration success email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send registration success email to: {}", user.getEmail(), e);
            }

            return KycSubmitResponse.builder()
                    .registrationId(reg.getId())
                    .kycId(existingKyc.getId())
                    .status(1) // VERIFIED
                    .faceMatchScore(existingKyc.getFaceMatchScore())
                    .livenessScore(existingKyc.getLivenessScore())
                    .message("Đăng ký đấu giá thành công (bạn đã xác thực KYC trước đó)")
                    .build();
        }

        boolean frontProvided = cccdFront != null && !cccdFront.isEmpty();
        boolean backProvided = cccdBack != null && !cccdBack.isEmpty();
        boolean selfieProvided = selfie != null && !selfie.isEmpty();

        // Role 0 chưa xác thực KYC: nếu chưa gửi đủ ảnh thì trả lời hướng dẫn thay vì
        // lỗi
        if (user.getRole() == 0 && (!frontProvided || !backProvided || !selfieProvided)) {
            return KycSubmitResponse.builder()
                    .registrationId(null)
                    .kycId(null)
                    .status(user.getKycStatus())
                    .faceMatchScore(null)
                    .livenessScore(null)
                    .message(
                            "Bạn cần xác thực CCCD (gửi đủ ảnh mặt trước, mặt sau và selfie) trước khi đăng ký đấu giá")
                    .build();
        }

        // Các trường hợp còn lại: bắt buộc phải có file ảnh đầy đủ
        if (!frontProvided) {
            throw new IllegalArgumentException(
                    "CCCD mặt trước không được để trống. Bạn cần xác thực KYC trước khi đăng ký đấu giá.");
        }
        if (!backProvided) {
            throw new IllegalArgumentException(
                    "CCCD mặt sau không được để trống. Bạn cần xác thực KYC trước khi đăng ký đấu giá.");
        }
        if (!selfieProvided) {
            throw new IllegalArgumentException(
                    "Ảnh selfie không được để trống. Bạn cần xác thực KYC trước khi đăng ký đấu giá.");
        }

        // 1) Convert MultipartFile -> Resource
        Resource frontRes;
        Resource backRes;
        Resource selfieRes;
        try {
            frontRes = new ByteArrayResource(cccdFront.getBytes()) {
                @Override
                public String getFilename() {
                    return cccdFront.getOriginalFilename() != null ? cccdFront.getOriginalFilename() : "front.jpg";
                }
            };
            backRes = new ByteArrayResource(cccdBack.getBytes()) {
                @Override
                public String getFilename() {
                    return cccdBack.getOriginalFilename() != null ? cccdBack.getOriginalFilename() : "back.jpg";
                }
            };
            selfieRes = new ByteArrayResource(selfie.getBytes()) {
                @Override
                public String getFilename() {
                    return selfie.getOriginalFilename() != null ? selfie.getOriginalFilename() : "selfie.jpg";
                }
            };
        } catch (Exception e) {
            log.error("Failed to read file bytes", e);
            throw new IllegalArgumentException("Không thể đọc file ảnh: " + e.getMessage());
        }

        // 2) Upload -> hash (Minio hash của VNPT)
        String frontHash = vnpt.uploadFile(frontRes, "front.jpg", "ocr front", "auction kyc");
        String backHash = vnpt.uploadFile(backRes, "back.jpg", "ocr back", "auction kyc");
        String selfieHash = vnpt.uploadFile(selfieRes, "selfie.jpg", "selfie", "auction kyc");

        // 3) Tạo client_session + token
        String ts = String.valueOf(System.currentTimeMillis());
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientSession = "WEB_AUCTION_" + rnd + "_" + ts;
        String token = rnd;

        // 4) OCR front + back
        Map<String, Object> ocrFront = vnpt.ocrFront(frontHash, clientSession, token);
        Map<String, Object> ocrBack = vnpt.ocrBack(backHash, clientSession, token);

        Map<String, Object> frontObj = (Map<String, Object>) ocrFront.get("object");
        Map<String, Object> backObj = (Map<String, Object>) ocrBack.get("object");

        String fullName = frontObj == null ? null : (String) frontObj.get("name");
        String idNumber = frontObj == null ? null : (String) frontObj.get("id");
        // recent_location nằm ở mặt TRƯỚC trong tài liệu, nếu back không có thì
        // fallback từ front
        String addressFront = frontObj == null ? null : (String) frontObj.get("recent_location");
        String addressBack = backObj == null ? null : (String) backObj.get("recent_location");
        String address = addressBack != null ? addressBack : addressFront;

        String birthDay = frontObj == null ? null : (String) frontObj.get("birth_day");
        String issueDateS = backObj == null ? null : (String) backObj.get("issue_date");

        // 5) Card liveness (giấy tờ thật hay không)
        Map<String, Object> lv = vnpt.cardLiveness(frontHash, clientSession);
        Map<String, Object> lvObj = (Map<String, Object>) lv.get("object");
        String liveness = lvObj == null ? null : (String) lvObj.get("liveness");

        // API trả: face_swapping (boolean), fake_liveness (boolean), không có
        // fake_liveness_prob
        boolean fakeLiveness = false;
        boolean faceSwapping = false;
        if (lvObj != null) {
            Object fl = lvObj.get("fake_liveness");
            Object fs = lvObj.get("face_swapping");
            if (fl instanceof Boolean)
                fakeLiveness = (Boolean) fl;
            if (fs instanceof Boolean)
                faceSwapping = (Boolean) fs;
        }

        double liveScore = fakeLiveness ? 0.0 : 1.0;
        boolean livenessPassed = "success".equalsIgnoreCase(liveness)
                && liveScore >= liveThreshold
                && !faceSwapping;

        // 6) Face compare (so sánh khuôn mặt trên CCCD với selfie)
        // Theo tài liệu VNPT: response có object.prob (float) và object.msg
        // ("MATCH"/"NOMATCH")
        Map<String, Object> faceCompareResult = vnpt.faceCompare(frontHash, selfieHash, clientSession, token);
        Map<String, Object> fmObj = (Map<String, Object>) faceCompareResult.get("object");

        Double faceScore = null;
        String faceMatchMsg = null;
        if (fmObj != null) {
            // Parse prob (tỷ lệ khớp, ví dụ: 58.26)
            Object probObj = fmObj.get("prob");
            if (probObj instanceof Number) {
                faceScore = ((Number) probObj).doubleValue();
            } else if (probObj instanceof String) {
                try {
                    faceScore = Double.parseDouble((String) probObj);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse face prob: {}", probObj);
                }
            }
            // Parse msg ("MATCH" hoặc "NOMATCH")
            Object msgObj = fmObj.get("msg");
            if (msgObj instanceof String) {
                faceMatchMsg = (String) msgObj;
            }
        }

        // Face match passed nếu: prob >= threshold VÀ msg = "MATCH"
        boolean faceMatchPassed = faceScore != null
                && faceScore >= faceThreshold
                && "MATCH".equalsIgnoreCase(faceMatchMsg);

        // 7) Tổng hợp kết quả
        boolean passed = livenessPassed && faceMatchPassed;

        // 8) Parse ngày
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dob = null, issueDate = null;
        try {
            if (birthDay != null && birthDay.contains("/"))
                dob = LocalDate.parse(birthDay, f);
        } catch (Exception ignored) {
        }
        try {
            if (issueDateS != null && issueDateS.contains("/"))
                issueDate = LocalDate.parse(issueDateS, f);
        } catch (Exception ignored) {
        }

        // 9) Lưu raw JSON từ vendor
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(Map.of(
                    "ocrFront", ocrFront,
                    "ocrBack", ocrBack,
                    "liveness", lv,
                    "faceCompare", faceCompareResult));
        } catch (Exception e) {
            rawJson = "{}";
        }

        KycVerification kyc = KycVerification.builder()
                .userId(user.getId())
                .roomId(room.getId())
                .cccdFrontUrl(frontHash)
                .cccdBackUrl(backHash)
                .selfieUrl(selfieHash)
                .fullName(fullName)
                .cccdNumber(idNumber)
                .addressOnCard(address)
                .dateOfBirth(dob)
                .issueDate(issueDate)
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .passed(passed)
                .rawVendorResponse(rawJson)
                .status(passed ? 1 : 0)
                .build();

        kyc = kycRepo.save(kyc);

        AuctionRegistration reg = AuctionRegistration.builder()
                .userId(user.getId())
                .roomId(room.getId())
                .kycId(kyc.getId())
                .status(passed ? "VERIFIED" : "REJECTED")
                .note(null)
                .build();

        reg = regRepo.save(reg);

        // 10) Gửi email nếu xác thực thành công
        if (passed) {
            try {
                String userName = fullName != null ? fullName : user.getUsername();
                emailService.sendRegistrationSuccessEmail(
                        user.getEmail(),
                        userName,
                        reg.getId(),
                        room.getId());
                log.info("Registration success email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send registration success email to: {}", user.getEmail(), e);
                // Không throw exception để không ảnh hưởng đến response
            }
        }

        return KycSubmitResponse.builder()
                .registrationId(reg.getId())
                .kycId(kyc.getId())
                .status(kyc.getStatus())
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .message(passed ? "Đăng ký đã được xác thực thành công" : "Xác thực thất bại")
                .build();
    }

    /**
     * Xác thực KYC cho profile (không cần roomId)
     * Sau khi xác thực thành công, tự động set role = 1 (buyer) và kycStatus = 1
     */
    @Transactional
    public KycVerifyResponse verifyKycForProfile(String userId, MultipartFile cccdFront, MultipartFile cccdBack,
                                                 MultipartFile selfie) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validate files
        if (cccdFront == null || cccdFront.isEmpty()) {
            throw new IllegalArgumentException("CCCD mặt trước không được để trống");
        }
        if (cccdBack == null || cccdBack.isEmpty()) {
            throw new IllegalArgumentException("CCCD mặt sau không được để trống");
        }
        if (selfie == null || selfie.isEmpty()) {
            throw new IllegalArgumentException("Ảnh selfie không được để trống");
        }

        // 1) Convert MultipartFile -> Resource
        Resource frontRes;
        Resource backRes;
        Resource selfieRes;
        try {
            frontRes = new ByteArrayResource(cccdFront.getBytes()) {
                @Override
                public String getFilename() {
                    return cccdFront.getOriginalFilename() != null ? cccdFront.getOriginalFilename() : "front.jpg";
                }
            };
            backRes = new ByteArrayResource(cccdBack.getBytes()) {
                @Override
                public String getFilename() {
                    return cccdBack.getOriginalFilename() != null ? cccdBack.getOriginalFilename() : "back.jpg";
                }
            };
            selfieRes = new ByteArrayResource(selfie.getBytes()) {
                @Override
                public String getFilename() {
                    return selfie.getOriginalFilename() != null ? selfie.getOriginalFilename() : "selfie.jpg";
                }
            };
        } catch (Exception e) {
            log.error("Failed to read file bytes", e);
            throw new IllegalArgumentException("Không thể đọc file ảnh: " + e.getMessage());
        }

        // 2) Upload -> hash (Minio hash của VNPT)
        String frontHash = vnpt.uploadFile(frontRes, "front.jpg", "ocr front", "profile kyc");
        String backHash = vnpt.uploadFile(backRes, "back.jpg", "ocr back", "profile kyc");
        String selfieHash = vnpt.uploadFile(selfieRes, "selfie.jpg", "selfie", "profile kyc");

        // 3) Tạo client_session + token
        String ts = String.valueOf(System.currentTimeMillis());
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String clientSession = "WEB_PROFILE_" + rnd + "_" + ts;
        String token = rnd;

        // 4) OCR front + back
        Map<String, Object> ocrFront = vnpt.ocrFront(frontHash, clientSession, token);
        Map<String, Object> ocrBack = vnpt.ocrBack(backHash, clientSession, token);

        Map<String, Object> frontObj = (Map<String, Object>) ocrFront.get("object");
        Map<String, Object> backObj = (Map<String, Object>) ocrBack.get("object");

        String fullName = frontObj == null ? null : (String) frontObj.get("name");
        String idNumber = frontObj == null ? null : (String) frontObj.get("id");
        String addressFront = frontObj == null ? null : (String) frontObj.get("recent_location");
        String addressBack = backObj == null ? null : (String) backObj.get("recent_location");
        String address = addressBack != null ? addressBack : addressFront;

        String birthDay = frontObj == null ? null : (String) frontObj.get("birth_day");
        String issueDateS = backObj == null ? null : (String) backObj.get("issue_date");

        // 5) Card liveness
        Map<String, Object> lv = vnpt.cardLiveness(frontHash, clientSession);
        Map<String, Object> lvObj = (Map<String, Object>) lv.get("object");
        String liveness = lvObj == null ? null : (String) lvObj.get("liveness");

        boolean fakeLiveness = false;
        boolean faceSwapping = false;
        if (lvObj != null) {
            Object fl = lvObj.get("fake_liveness");
            Object fs = lvObj.get("face_swapping");
            if (fl instanceof Boolean)
                fakeLiveness = (Boolean) fl;
            if (fs instanceof Boolean)
                faceSwapping = (Boolean) fs;
        }

        double liveScore = fakeLiveness ? 0.0 : 1.0;
        boolean livenessPassed = "success".equalsIgnoreCase(liveness)
                && liveScore >= liveThreshold
                && !faceSwapping;

        // 6) Face compare
        Map<String, Object> faceCompareResult = vnpt.faceCompare(frontHash, selfieHash, clientSession, token);
        Map<String, Object> fmObj = (Map<String, Object>) faceCompareResult.get("object");

        Double faceScore = null;
        String faceMatchMsg = null;
        if (fmObj != null) {
            Object probObj = fmObj.get("prob");
            if (probObj instanceof Number) {
                faceScore = ((Number) probObj).doubleValue();
            } else if (probObj instanceof String) {
                try {
                    faceScore = Double.parseDouble((String) probObj);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse face prob: {}", probObj);
                }
            }
            Object msgObj = fmObj.get("msg");
            if (msgObj instanceof String) {
                faceMatchMsg = (String) msgObj;
            }
        }

        boolean faceMatchPassed = faceScore != null
                && faceScore >= faceThreshold
                && "MATCH".equalsIgnoreCase(faceMatchMsg);

        // 7) Tổng hợp kết quả
        boolean passed = livenessPassed && faceMatchPassed;

        // 8) Parse ngày
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dob = null, issueDate = null;
        try {
            if (birthDay != null && birthDay.contains("/"))
                dob = LocalDate.parse(birthDay, f);
        } catch (Exception ignored) {
        }
        try {
            if (issueDateS != null && issueDateS.contains("/"))
                issueDate = LocalDate.parse(issueDateS, f);
        } catch (Exception ignored) {
        }

        // 9) Lưu raw JSON từ vendor
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(Map.of(
                    "ocrFront", ocrFront,
                    "ocrBack", ocrBack,
                    "liveness", lv,
                    "faceCompare", faceCompareResult));
        } catch (Exception e) {
            rawJson = "{}";
        }

        // 10) Lưu KycVerification (roomId = null cho profile verification)
        KycVerification kyc = KycVerification.builder()
                .userId(user.getId())
                .roomId(null) // Profile verification không cần roomId
                .cccdFrontUrl(frontHash)
                .cccdBackUrl(backHash)
                .selfieUrl(selfieHash)
                .fullName(fullName)
                .cccdNumber(idNumber)
                .addressOnCard(address)
                .dateOfBirth(dob)
                .issueDate(issueDate)
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .passed(passed)
                .rawVendorResponse(rawJson)
                .status(passed ? 1 : 0)
                .build();

        kyc = kycRepo.save(kyc);

        // 11) Nếu xác thực thành công, update user: set role = 1 (buyer) và kycStatus =
        // 1
        if (passed) {
            user.setRole(1); // Buyer
            user.setKycStatus(1); // Đã xác thực thành công
            user.setCccd(idNumber); // Lưu số CCCD vào profile
            if (address != null) {
                user.setAddress(address);
            }
            if (dob != null) {
                user.setDateOfBirth(dob);
            }
            userRepo.save(user);
            log.info("User {} verified KYC successfully, role updated to 1 (buyer)", user.getId());
        } else {
            // Nếu thất bại, chỉ update kycStatus = 0
            user.setKycStatus(0);
            userRepo.save(user);
            log.warn("User {} KYC verification failed", user.getId());
        }

        return KycVerifyResponse.builder()
                .kycId(kyc.getId())
                .kycStatus(passed ? 1 : 0)
                .role(user.getRole())
                .faceMatchScore(faceScore)
                .livenessScore(liveScore)
                .message(passed ? "Xác thực KYC thành công, bạn đã được nâng lên Buyer" : "Xác thực KYC thất bại")
                .build();
    }
}
