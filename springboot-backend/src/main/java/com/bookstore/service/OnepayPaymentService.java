package com.bookstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * OnePAY (vpcpay.op) - tạo link thanh toán + verify chữ ký return.
 *
 * Theo mẫu phổ biến:
 * - Chuỗi hash: ksort(params) và ghép "key=value" với '&' giữa
 * - Secure hash: HMAC-SHA256 với key là HashCode (chuỗi hex) và lấy hex (Uppercase)
 */
@Service
public class OnepayPaymentService {

    @Value("${onepay.endpoint:https://mtf.onepay.vn/paygate/vpcpay.op}")
    private String endpoint;

    @Value("${onepay.merchant-id:disabled-onepay-merchant}")
    private String merchantId;

    @Value("${onepay.access-code:disabled-onepay-access}")
    private String accessCode;

    @Value("${onepay.hash-code:disabled-onepay-hash}")
    private String hashCodeHex;

    @Value("${onepay.locale:vn}")
    private String locale;

    @Value("${onepay.version:1}")
    private String version;

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public OnepayPaymentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(merchantId)
                && StringUtils.hasText(accessCode)
                && StringUtils.hasText(hashCodeHex)
                && !merchantId.startsWith("disabled-")
                && !accessCode.startsWith("disabled-")
                && !hashCodeHex.startsWith("disabled-");
    }

    /**
     * Tạo redirect URL tới OnePAY.
     */
    public String createPaymentRedirectUrl(long orderId, double amountVnd, String orderInfo, String baseUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("OnePAY keys are not configured (ONEPAY_MERCHANT_ID/ONEPAY_ACCESS_CODE/ONEPAY_HASH_CODE).");
        }

        String returnUrl = trimTrailingSlash(baseUrl) + "/payment/onepay/return";

        // OnePAY sample thường nhân 100 cho vpc_Amount.
        long amountMinor = Math.round(Math.max(0d, amountVnd) * 100d);

        // vpc_MerchTxnRef phải là duy nhất (nếu dùng lại dễ bị OnePAY từ chối / báo lỗi xử lý).
        // Giữ orderId ở prefix để khi return có thể parse ra.
        String merchTxnRef = orderId + "-" + System.currentTimeMillis();

        // Giữ orderInfo ngắn gọn (1 vài gateway giới hạn độ dài).
        String info = StringUtils.hasText(orderInfo) ? orderInfo : ("Order #" + orderId);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("vpc_AccessCode", accessCode);
        data.put("vpc_Amount", String.valueOf(amountMinor));
        data.put("vpc_Command", "pay");
        data.put("vpc_Currency", "VND");
        data.put("vpc_Locale", locale);
        data.put("vpc_MerchTxnRef", merchTxnRef);
        data.put("vpc_Merchant", merchantId);
        data.put("vpc_OrderInfo", info);
        data.put("vpc_ReturnURL", returnUrl);
        data.put("vpc_Version", version);

        // OnePAY thường sort alphabet theo key (ksort) trước khi ghép chuỗi hash.
        TreeMap<String, String> sorted = new TreeMap<>(data);

        String hashData = buildHashQuery(sorted); // raw (không urlencode) cho secure hash
        String secureHash = signHmacSha256Upper(hashData, hashCodeHex);

        String query = buildUrlEncodedQuery(sorted) + "&vpc_SecureHash=" + urlEncode(secureHash);

        // endpoint có thể có sẵn query (hiếm). Nếu có, sẽ thêm '&' thay vì '?'.
        String join = endpoint.contains("?") ? "&" : "?";
        return endpoint + join + query;
    }

    /**
     * Verify chữ ký return (best-effort) bằng vpc_SecureHash.
     */
    public boolean verifySecureHash(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) return false;

        String secureHash = queryParams.get("vpc_SecureHash");
        if (!StringUtils.hasText(secureHash)) return false;

        // Bỏ vpc_SecureHash ra khỏi tập tham số để hash giống cách tạo ở phía client.
        TreeMap<String, String> filtered = new TreeMap<>();
        for (Map.Entry<String, String> e : queryParams.entrySet()) {
            String k = e.getKey();
            if (k == null || "vpc_SecureHash".equals(k)) continue;
            String v = e.getValue();
            if (!StringUtils.hasText(v)) continue;
            filtered.put(k, v);
        }

        String md5HashData = buildHashQuery(filtered);
        String expected = signHmacSha256Upper(md5HashData, hashCodeHex);
        return expected.equalsIgnoreCase(secureHash.trim());
    }

    public static String extractOrderIdFromReturn(Map<String, String> queryParams) {
        if (queryParams == null) return null;
        String merchTxnRef = queryParams.get("vpc_MerchTxnRef");
        if (!StringUtils.hasText(merchTxnRef)) return null;
        try {
            String ref = merchTxnRef.trim();
            int dash = ref.indexOf('-');
            return dash > 0 ? ref.substring(0, dash) : ref;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.replaceAll("/+$", "");
    }

    private static String buildHashQuery(TreeMap<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (!StringUtils.hasText(v)) continue;
            if (!first) sb.append("&");
            sb.append(k).append("=").append(v);
            first = false;
        }
        return sb.toString();
    }

    private static String buildUrlEncodedQuery(TreeMap<String, String> sortedParams) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : sortedParams.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (!StringUtils.hasText(v)) continue;
            if (!first) sb.append("&");
            sb.append(urlEncode(k)).append("=").append(urlEncode(v));
            first = false;
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return s;
        }
    }

    private static String signHmacSha256Upper(String data, String hexKey) {
        try {
            byte[] keyBytes = hexToBytes(hexKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHexUpper(raw);
        } catch (Exception e) {
            throw new RuntimeException("OnePAY secure hash error: " + e.getMessage(), e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (!StringUtils.hasText(hex)) return new byte[0];
        String cleaned = hex.trim();
        if (cleaned.length() % 2 != 0) cleaned = "0" + cleaned;
        int len = cleaned.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(cleaned.substring(i, i + 2), 16);
        }
        return out;
    }

    private static String bytesToHexUpper(byte[] raw) {
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}

