package com.auctionaa.backend.VNPT;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class VnptKycClient {

    private final WebClient webClient;
    private final String accessToken;
    private final String tokenId;
    private final String tokenKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public VnptKycClient(
            @Value("${vnpt.base-url}") String baseUrl,
            @Value("${vnpt.access-token}") String accessTokenRaw,
            @Value("${vnpt.token-id}") String tokenId,
            @Value("${vnpt.token-key}") String tokenKey,
            Environment environment) {

        // Chuẩn hóa access token: loại bỏ "bearer " prefix và whitespace
        String token = accessTokenRaw == null ? "" : accessTokenRaw.trim();

        // Debug: log raw token để kiểm tra (không log full token vì security)
        log.info("[VNPT] Raw access token from properties - length: {}, first 50 chars: {}",
                token.length(),
                token.length() > 50 ? token.substring(0, 50) : token);
        log.info("[VNPT] Raw tokenId from properties: {}", tokenId);
        log.info("[VNPT] Raw tokenKey from @Value injection - length: {}, first 50 chars: {}",
                tokenKey != null ? tokenKey.length() : 0,
                tokenKey != null && tokenKey.length() > 50 ? tokenKey.substring(0, 50) : tokenKey);

        // Kiểm tra environment variable (Spring Boot ưu tiên env var hơn properties)
        String envTokenKey = System.getenv("VNPT_TOKEN_KEY");
        String finalTokenKey = tokenKey;

        if (envTokenKey != null) {
            if (envTokenKey.trim().isEmpty()) {
                log.warn("[VNPT] ⚠️ Environment variable VNPT_TOKEN_KEY exists but is EMPTY. " +
                        "This would override properties file with empty value! Reading directly from properties file instead.");
                // Nếu env var rỗng, đọc trực tiếp từ properties file bằng Java I/O
                try {
                    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    Resource resource = resolver.getResource("classpath:application.properties");
                    Properties props = new Properties();
                    try (InputStream is = resource.getInputStream()) {
                        props.load(is);
                        finalTokenKey = props.getProperty("vnpt.token-key");
                        if (finalTokenKey != null && !finalTokenKey.trim().isEmpty()) {
                            log.info(
                                    "[VNPT] ✅ Successfully read tokenKey from properties file - length: {}, first 50: {}",
                                    finalTokenKey.length(),
                                    finalTokenKey.length() > 50 ? finalTokenKey.substring(0, 50) : finalTokenKey);
                            // Nếu giá trị bị cắt (chỉ có 128 ký tự), thử đọc trực tiếp từ file
                            if (finalTokenKey.length() < 150) {
                                log.warn(
                                        "[VNPT] ⚠️ Token key from Properties.load() is too short ({}). Trying to read raw from file...",
                                        finalTokenKey.length());
                                try (InputStream rawIs = resource.getInputStream()) {
                                    String content = new String(rawIs.readAllBytes(),
                                            java.nio.charset.StandardCharsets.UTF_8);
                                    log.debug("[VNPT] Raw file content length: {} bytes", content.length());
                                    // Tìm dòng chứa vnpt.token-key và đọc giá trị (có thể wrap trên nhiều dòng)
                                    String[] lines = content.split("\r?\n");
                                    StringBuilder tokenKeyBuilder = null;
                                    for (int i = 0; i < lines.length; i++) {
                                        String line = lines[i].trim();
                                        if (line.startsWith("vnpt.token-key=")) {
                                            // Bắt đầu đọc giá trị
                                            String value = line.substring("vnpt.token-key=".length());
                                            log.debug("[VNPT] Found vnpt.token-key line, initial value length: {}",
                                                    value.length());
                                            tokenKeyBuilder = new StringBuilder(value);
                                            // Đọc tiếp các dòng nếu giá trị bị wrap (dòng tiếp theo không bắt đầu với
                                            // key khác)
                                            for (int j = i + 1; j < lines.length; j++) {
                                                String nextLine = lines[j].trim();
                                                // Nếu dòng tiếp theo là key mới hoặc comment, dừng lại
                                                if (nextLine.isEmpty() || nextLine.startsWith("#")
                                                        || (nextLine.contains("=") && !nextLine.startsWith(" "))) {
                                                    break;
                                                }
                                                // Nếu dòng tiếp theo là phần tiếp của giá trị (có thể có space ở đầu)
                                                tokenKeyBuilder.append(nextLine);
                                            }
                                            String rawValue = tokenKeyBuilder.toString().trim();
                                            log.info(
                                                    "[VNPT] Raw file tokenKey value - length: {}, first 50: {}, last 20: {}",
                                                    rawValue.length(),
                                                    rawValue.length() > 50 ? rawValue.substring(0, 50) : rawValue,
                                                    rawValue.length() > 20 ? rawValue.substring(rawValue.length() - 20)
                                                            : rawValue);
                                            if (rawValue.length() > finalTokenKey.length()) {
                                                log.info(
                                                        "[VNPT] ✅ Found longer value in raw file - length: {}, first 50: {}",
                                                        rawValue.length(),
                                                        rawValue.length() > 50 ? rawValue.substring(0, 50) : rawValue);
                                                finalTokenKey = rawValue;
                                            } else {
                                                log.warn(
                                                        "[VNPT] ⚠️ Raw file value length ({}) is not longer than Properties value ({}). "
                                                                +
                                                                "File in target/classes may be truncated. Trying to read from source file...",
                                                        rawValue.length(), finalTokenKey.length());
                                                // Thử đọc từ file source trực tiếp
                                                try {
                                                    // Tìm file source (src/main/resources/application.properties)
                                                    Path sourceFile = Paths
                                                            .get("src/main/resources/application.properties");
                                                    if (!Files.exists(sourceFile)) {
                                                        // Thử đường dẫn tuyệt đối từ working directory
                                                        String workingDir = System.getProperty("user.dir");
                                                        sourceFile = Paths.get(workingDir,
                                                                "src/main/resources/application.properties");
                                                    }
                                                    if (Files.exists(sourceFile)) {
                                                        log.info("[VNPT] Found source file at: {}",
                                                                sourceFile.toAbsolutePath());
                                                        String sourceContent = Files.readString(sourceFile);
                                                        log.info("[VNPT] Source file content length: {} bytes",
                                                                sourceContent.length());
                                                        // Tìm giá trị bằng cách tìm index của "vnpt.token-key=" và đọc
                                                        // đến hết dòng
                                                        String sourceValue = null;
                                                        int keyIndex = sourceContent.indexOf("vnpt.token-key=");
                                                        if (keyIndex >= 0) {
                                                            // Tìm vị trí bắt đầu giá trị (sau dấu =)
                                                            int valueStart = keyIndex + "vnpt.token-key=".length();
                                                            // Tìm vị trí kết thúc dòng (\r\n hoặc \n)
                                                            int lineEnd = sourceContent.indexOf("\r\n", valueStart);
                                                            if (lineEnd < 0) {
                                                                lineEnd = sourceContent.indexOf("\n", valueStart);
                                                            }
                                                            if (lineEnd < 0) {
                                                                lineEnd = sourceContent.length();
                                                            }
                                                            sourceValue = sourceContent.substring(valueStart, lineEnd)
                                                                    .trim();
                                                            log.info(
                                                                    "[VNPT] Direct substring value - length: {}, first 50: {}, last 20: {}, valueStart: {}, lineEnd: {}",
                                                                    sourceValue.length(),
                                                                    sourceValue.length() > 50
                                                                            ? sourceValue.substring(0, 50)
                                                                            : sourceValue,
                                                                    sourceValue.length() > 20
                                                                            ? sourceValue.substring(
                                                                            sourceValue.length() - 20)
                                                                            : sourceValue,
                                                                    valueStart, lineEnd);
                                                            // Log một phần nội dung xung quanh để debug
                                                            int contextStart = Math.max(0, keyIndex - 20);
                                                            int contextEnd = Math.min(sourceContent.length(),
                                                                    lineEnd + 20);
                                                            log.info(
                                                                    "[VNPT] Context around token-key (first 200 chars): {}",
                                                                    sourceContent
                                                                            .substring(contextStart,
                                                                                    Math.min(contextEnd,
                                                                                            contextStart + 200))
                                                                            .replace("\r", "\\r").replace("\n", "\\n"));
                                                        } else {
                                                            log.warn(
                                                                    "[VNPT] ⚠️ Could not find 'vnpt.token-key=' in source file!");
                                                        }
                                                        if (sourceValue == null) {
                                                            // Fallback: dùng cách cũ
                                                            String[] sourceLines = sourceContent.split("\r?\n");
                                                            for (int k = 0; k < sourceLines.length; k++) {
                                                                String sourceLine = sourceLines[k].trim();
                                                                if (sourceLine.startsWith("vnpt.token-key=")) {
                                                                    StringBuilder sourceBuilder = new StringBuilder(
                                                                            sourceLine
                                                                                    .substring("vnpt.token-key="
                                                                                            .length()));
                                                                    // Đọc tiếp nếu wrap
                                                                    for (int l = k + 1; l < sourceLines.length; l++) {
                                                                        String nextSourceLine = sourceLines[l].trim();
                                                                        if (nextSourceLine.isEmpty()
                                                                                || nextSourceLine.startsWith("#")
                                                                                || (nextSourceLine.contains("=")
                                                                                && !nextSourceLine
                                                                                .startsWith(" "))) {
                                                                            break;
                                                                        }
                                                                        sourceBuilder.append(nextSourceLine);
                                                                    }
                                                                    sourceValue = sourceBuilder.toString().trim();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        if (sourceValue != null) {
                                                            log.info(
                                                                    "[VNPT] Source file tokenKey value - length: {}, first 50: {}, last 20: {}",
                                                                    sourceValue.length(),
                                                                    sourceValue.length() > 50
                                                                            ? sourceValue.substring(0, 50)
                                                                            : sourceValue,
                                                                    sourceValue.length() > 20
                                                                            ? sourceValue.substring(
                                                                            sourceValue.length() - 20)
                                                                            : sourceValue);
                                                            if (sourceValue.length() > rawValue.length()) {
                                                                log.info(
                                                                        "[VNPT] ✅ Found full value in source file - length: {}, first 50: {}",
                                                                        sourceValue.length(),
                                                                        sourceValue.length() > 50
                                                                                ? sourceValue.substring(0, 50)
                                                                                : sourceValue);
                                                                finalTokenKey = sourceValue;
                                                            } else {
                                                                log.warn(
                                                                        "[VNPT] ⚠️ Source file value length ({}) is not longer than raw value ({}). "
                                                                                +
                                                                                "Source file may also be truncated or value is correct but Properties.load() truncated it.",
                                                                        sourceValue.length(), rawValue.length());
                                                            }
                                                        } else {
                                                            log.error(
                                                                    "[VNPT] ❌ ERROR: Could not find vnpt.token-key in source file!");
                                                        }
                                                    } else {
                                                        log.warn(
                                                                "[VNPT] ⚠️ Source file not found. Please rebuild project with 'mvn clean compile'.");
                                                    }
                                                } catch (Exception e) {
                                                    log.error("[VNPT] ❌ ERROR: Cannot read from source file: {}",
                                                            e.getMessage());
                                                }
                                            }
                                            break;
                                        }
                                    }
                                    if (tokenKeyBuilder == null) {
                                        log.error("[VNPT] ❌ ERROR: Could not find vnpt.token-key in raw file content!");
                                    }
                                } catch (Exception e) {
                                    log.error("[VNPT] ❌ ERROR: Exception while reading raw file: {}", e.getMessage(),
                                            e);
                                }
                            }
                        } else {
                            log.error("[VNPT] ❌ ERROR: Token key not found in properties file!");
                        }
                    }
                } catch (IOException e) {
                    log.error("[VNPT] ❌ ERROR: Cannot read properties file: {}", e.getMessage());
                }
            } else {
                log.warn("[VNPT] ⚠️ Environment variable VNPT_TOKEN_KEY detected - length: {}. " +
                                "This will override properties file! First 50 chars: {}",
                        envTokenKey.length(),
                        envTokenKey.length() > 50 ? envTokenKey.substring(0, 50) : envTokenKey);
                // Nếu env var có giá trị, Spring đã inject nó vào tokenKey rồi, dùng giá trị đó
                finalTokenKey = tokenKey;
            }
        } else {
            // Không có env var, dùng giá trị từ properties (đã được inject vào tokenKey)
            finalTokenKey = tokenKey;
        }

        // Warning nếu token key quá ngắn (RSA public key thường > 150 ký tự)
        if (finalTokenKey == null || finalTokenKey.trim().isEmpty()) {
            log.error("[VNPT] ❌ ERROR: Token key is NULL or EMPTY! Cannot proceed without token key.");
        } else if (finalTokenKey.length() < 150) {
            log.error("[VNPT] ❌ ERROR: Token key length is too short ({}). Expected > 150 for RSA public key. " +
                            "Token key may be truncated or environment variable may be overriding properties file. " +
                            "Please check IntelliJ Run Configuration → Environment variables and remove VNPT_TOKEN_KEY if exists.",
                    finalTokenKey.length());
        }

        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        // Loại bỏ tất cả whitespace (bao gồm \n, \r, space)
        token = token.replaceAll("\\s+", "");
        this.accessToken = token;
        this.tokenId = tokenId == null ? "" : tokenId.trim();
        // Token-key cũng cần loại bỏ whitespace (có thể bị wrap trong properties)
        this.tokenKey = finalTokenKey == null ? "" : finalTokenKey.trim().replaceAll("\\s+", "");

        // Chuẩn hóa baseUrl
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");

        this.webClient = WebClient.builder()
                .baseUrl(normalizedBaseUrl)
                .build();

        // Log để debug (không log full token vì security)
        log.info("[VNPT] Initialized - baseUrl={}, tokenId={}, tokenLength={}, tokenPrefix={}..., tokenKeyLength={}",
                normalizedBaseUrl, this.tokenId, this.accessToken.length(),
                this.accessToken.length() > 20 ? this.accessToken.substring(0, 20) : "too short",
                this.tokenKey.length());

        // Warning nếu token quá ngắn
        if (this.accessToken.length() < 100) {
            log.warn(
                    "[VNPT] WARNING: Access token length is too short ({}). Expected > 500 for JWT token. Token may be truncated or incorrect.",
                    this.accessToken.length());
        }
    }

    // ================== 1. UPLOAD FILE -> HASH ==================
    public String uploadFile(Resource fileResource,
                             String fileName,
                             String title,
                             String description) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource); // key phải là "file" theo spec
        body.add("title", title);
        body.add("description", description);

        try {
            String raw = webClient.post()
                    .uri("/file-service/v1/addFile")
                    .headers(h -> {
                        h.setBearerAuth(accessToken); // Authorization: Bearer xxx
                        h.add("Token-id", tokenId);
                        h.add("Token-key", tokenKey);
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class).flatMap(b -> {
                        log.error("[VNPT] Upload failed - Status: {}, Response: {}", res.statusCode(), b);
                        if (res.statusCode().value() == 401) {
                            log.error(
                                    "[VNPT] 401 Unauthorized - Token có thể đã hết hạn hoặc không hợp lệ. Vui lòng refresh token từ VNPT eKYC dashboard.");
                        }
                        return Mono.error(new RuntimeException("VNPT upload error: " + b));
                    }))
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> map = mapper.readValue(raw, new TypeReference<>() {
            });
            Map<String, Object> object = (Map<String, Object>) map.get("object");
            if (object == null) {
                throw new RuntimeException("VNPT upload response missing object: " + raw);
            }
            return (String) object.get("hash"); // trả về hash để dùng cho OCR / face

        } catch (WebClientResponseException e) {
            log.error("VNPT uploadFile 401/4xx: {}", e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("VNPT uploadFile error", e);
            throw new RuntimeException("VNPT uploadFile error", e);
        }

    }

    // ================== 2. OCR FRONT ==================
    public Map<String, Object> ocrFront(String imgHash,
                                        String clientSession,
                                        String token) {

        Map<String, Object> body = new HashMap<>();
        body.put("img_front", imgHash);
        body.put("client_session", clientSession);
        body.put("type", -1); // -1: CMT cũ, mới, CCCD
        body.put("validate_postcode", true);
        body.put("token", token);

        return postJson("/ai/v1/ocr/id/front", body);
    }

    // ================== 3. OCR BACK ==================
    public Map<String, Object> ocrBack(String imgHash,
                                       String clientSession,
                                       String token) {

        Map<String, Object> body = new HashMap<>();
        body.put("img_back", imgHash);
        body.put("client_session", clientSession);
        body.put("type", -1);
        body.put("token", token);

        return postJson("/ai/v1/ocr/id/back", body);
    }

    // ================== 4. CARD LIVENESS ==================
    public Map<String, Object> cardLiveness(String imgHash,
                                            String clientSession) {

        Map<String, Object> body = new HashMap<>();
        body.put("img", imgHash);
        body.put("client_session", clientSession);

        return postJson("/ai/v1/card/liveness", body);
    }

    // ================== 5. FACE COMPARE ==================
    // Theo tài liệu VNPT: API face/compare sử dụng img_front và img_face
    public Map<String, Object> faceCompare(String imgCardHash,
                                           String selfieHash,
                                           String clientSession,
                                           String token) {

        Map<String, Object> body = new HashMap<>();
        body.put("img_front", imgCardHash); // Mặt trước CCCD
        body.put("img_face", selfieHash); // Ảnh selfie
        body.put("client_session", clientSession);
        body.put("token", token);

        return postJson("/ai/v1/face/compare", body);
    }

    // ================== Helper chung cho JSON POST ==================
    private Map<String, Object> postJson(String path, Map<String, Object> body) {
        try {
            String raw = webClient.post()
                    .uri(path)
                    .headers(h -> {
                        h.setBearerAuth(accessToken);
                        h.add("Token-id", tokenId);
                        h.add("Token-key", tokenKey);
                        h.add("mac-address", "TEST1");
                        // Debug log headers (không log full values)
                        log.debug("[VNPT] Request headers - Token-id length: {}, Token-key length: {}",
                                tokenId.length(), tokenKey.length());
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res -> res.bodyToMono(String.class).flatMap(b -> {
                        log.error("[VNPT] {} failed - Status: {}, Response: {}", path, res.statusCode(), b);
                        if (res.statusCode().value() == 401) {
                            log.error(
                                    "[VNPT] 401 Unauthorized - Token có thể đã hết hạn. Vui lòng refresh token từ VNPT eKYC dashboard.");
                        }
                        return Mono.error(new RuntimeException("VNPT error: " + b));
                    }))
                    .bodyToMono(String.class)
                    .block();

            return mapper.readValue(raw, new TypeReference<>() {
            });
        } catch (WebClientResponseException e) {
            log.error("VNPT {} 4xx/5xx: {}", path, e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            log.error("VNPT {} error", path, e);
            throw new RuntimeException("VNPT call error", e);
        }
    }
}
