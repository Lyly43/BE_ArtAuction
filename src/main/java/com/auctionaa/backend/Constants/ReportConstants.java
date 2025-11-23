package com.auctionaa.backend.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Constants cho các loại tố cáo
 * Entity Type là int: 1=User, 2=Artwork, 3=Auction Room, 4=AI Artwork
 * Report Type là String: "Spam", "Fake Identity", "Copyright Violation", etc.
 */
public class ReportConstants {

    // Entity Types (INT)
    public static final int ENTITY_USER = 1;
    public static final int ENTITY_ARTWORK = 2;
    public static final int ENTITY_AUCTION_ROOM = 3;
    public static final int ENTITY_AI_ARTWORK = 4;

    // Report Status (INT)
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_RESOLVED = 2;
    public static final int STATUS_REJECTED = 3;

    // User Report Types (STRING)
    public static final String USER_FAKE_IDENTITY = "Fake Identity";
    public static final String USER_SUSPICIOUS_ACTIVITY = "Suspicious Activity";
    public static final String USER_SCAM_FRAUD = "Scam / Fraud";
    public static final String USER_HARASSMENT = "Harassment / Abusive Behavior";
    public static final String USER_POLICY_VIOLATION = "Policy Violation";
    public static final String USER_SPAM = "Spam / Unwanted Ads";
    public static final String USER_UNAUTHORIZED_ACCESS = "Unauthorized Access";
    public static final String USER_OTHER = "Other";

    // Artwork Report Types (STRING)
    public static final String ARTWORK_FAKE = "Fake Artwork";
    public static final String ARTWORK_COPYRIGHT = "Copyright Violation";
    public static final String ARTWORK_MISLEADING_INFO = "Wrong / Misleading Information";
    public static final String ARTWORK_INAPPROPRIATE = "Inappropriate Content";
    public static final String ARTWORK_RESTRICTED = "Restricted / Sensitive Artwork";
    public static final String ARTWORK_MANIPULATED_IMAGE = "Manipulated / Misleading Images";

    // Auction Room Report Types (STRING)
    public static final String ROOM_FRAUDULENT_BIDDING = "Fraudulent Bidding / Fake Bids";
    public static final String ROOM_HOST_VIOLATION = "Host Rule Violation";
    public static final String ROOM_UNFAIR_BEHAVIOR = "Unfair Behavior";
    public static final String ROOM_TECHNICAL_ERROR = "System / Technical Error";
    public static final String ROOM_MISLEADING_INFO = "Misleading Room Information";
    public static final String ROOM_DISRUPTIVE_PARTICIPANT = "Disruptive Participant";
    public static final String ROOM_UNUSUAL_RULE_CHANGES = "Unusual Rule Changes";

    // AI Artwork Report Type (STRING)
    public static final String AI_INACCURATE_RESULT = "Inaccurate AI Result";

    // Maps để validate và lấy danh sách report types hợp lệ cho mỗi entity type
    private static final Map<Integer, Set<String>> VALID_REPORT_TYPES = new HashMap<>();

    static {
        // User report types
        VALID_REPORT_TYPES.put(ENTITY_USER, Set.of(
                USER_FAKE_IDENTITY,
                USER_SUSPICIOUS_ACTIVITY,
                USER_SCAM_FRAUD,
                USER_HARASSMENT,
                USER_POLICY_VIOLATION,
                USER_SPAM,
                USER_UNAUTHORIZED_ACCESS,
                USER_OTHER));

        // Artwork report types
        VALID_REPORT_TYPES.put(ENTITY_ARTWORK, Set.of(
                ARTWORK_FAKE,
                ARTWORK_COPYRIGHT,
                ARTWORK_MISLEADING_INFO,
                ARTWORK_INAPPROPRIATE,
                ARTWORK_RESTRICTED,
                ARTWORK_MANIPULATED_IMAGE));

        // Auction Room report types
        VALID_REPORT_TYPES.put(ENTITY_AUCTION_ROOM, Set.of(
                ROOM_FRAUDULENT_BIDDING,
                ROOM_HOST_VIOLATION,
                ROOM_UNFAIR_BEHAVIOR,
                ROOM_TECHNICAL_ERROR,
                ROOM_MISLEADING_INFO,
                ROOM_DISRUPTIVE_PARTICIPANT,
                ROOM_UNUSUAL_RULE_CHANGES));

        // AI Artwork report types
        VALID_REPORT_TYPES.put(ENTITY_AI_ARTWORK, Set.of(
                AI_INACCURATE_RESULT));
    }

    /**
     * Lấy tên entity type
     */
    public static String getEntityTypeName(int entityType) {
        return switch (entityType) {
            case ENTITY_USER -> "User";
            case ENTITY_ARTWORK -> "Artwork";
            case ENTITY_AUCTION_ROOM -> "Auction Room";
            case ENTITY_AI_ARTWORK -> "AI Artwork";
            default -> "Không xác định";
        };
    }

    /**
     * Validate reportType có hợp lệ với entityType không
     */
    public static boolean isValidReportType(int entityType, String reportType) {
        Set<String> validTypes = VALID_REPORT_TYPES.get(entityType);
        if (validTypes == null) {
            return false;
        }
        return validTypes.contains(reportType);
    }

    /**
     * Lấy danh sách report types hợp lệ cho entity type
     */
    public static Set<String> getValidReportTypes(int entityType) {
        return VALID_REPORT_TYPES.getOrDefault(entityType, Set.of());
    }

    /**
     * Lấy tên status
     */
    public static String getStatusName(int status) {
        return switch (status) {
            case STATUS_PENDING -> "Chờ xử lý";
            case STATUS_PROCESSING -> "Đang xử lý";
            case STATUS_RESOLVED -> "Đã xử lý";
            case STATUS_REJECTED -> "Đã từ chối";
            default -> "Không xác định";
        };
    }
}
