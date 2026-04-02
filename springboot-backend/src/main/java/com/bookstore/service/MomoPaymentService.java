package com.bookstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MomoPaymentService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.base-url:http://localhost:8081}")
    private String appBaseUrl;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.request-type:captureWallet}")
    private String requestType;

    @Value("${momo.lang:vi}")
    private String lang;

    public MomoPaymentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public MomoCreateResponse createPayment(long orderId, long amount, String orderInfo) {
        return createPayment(orderId, amount, orderInfo, null);
    }

    public MomoCreateResponse createPayment(long orderId, long amount, String orderInfo, String baseUrlOverride) {
        if (!StringUtils.hasText(partnerCode) || partnerCode.startsWith("disabled-")
                || !StringUtils.hasText(accessKey) || accessKey.startsWith("disabled-")
                || !StringUtils.hasText(secretKey) || secretKey.startsWith("disabled-")) {
            throw new IllegalStateException("MoMo keys are not configured (MOMO_PARTNER_CODE/MOMO_ACCESS_KEY/MOMO_SECRET_KEY).");
        }

        String base = (StringUtils.hasText(baseUrlOverride) ? baseUrlOverride : appBaseUrl);
        String requestId = "REQ-" + orderId + "-" + Instant.now().toEpochMilli();
        String momoOrderId = String.valueOf(orderId);
        String redirectUrl = base.replaceAll("/+$", "") + "/payment/momo/return";
        String ipnUrl = base.replaceAll("/+$", "") + "/payment/momo/ipn";
        String extraData = "";

        // MoMo signature raw string (v2 create)
        String rawHash = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + momoOrderId
                + "&orderInfo=" + (orderInfo != null ? orderInfo : "")
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        String signature = hmacSha256(secretKey, rawHash);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", momoOrderId);
        body.put("orderInfo", orderInfo != null ? orderInfo : "pay with MoMo");
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("lang", lang);
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", signature);

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("MoMo create failed: HTTP " + resp.statusCode() + " - " + resp.body());
            }
            JsonNode node = objectMapper.readTree(resp.body());
            String payUrl = text(node, "payUrl");
            String deeplink = text(node, "deeplink");
            String qrCodeUrl = text(node, "qrCodeUrl");
            Integer resultCode = node.hasNonNull("resultCode") ? node.get("resultCode").asInt() : null;
            String message = text(node, "message");

            return new MomoCreateResponse(payUrl, deeplink, qrCodeUrl, resultCode, message, requestId);
        } catch (Exception e) {
            throw new RuntimeException("MoMo create error: " + e.getMessage(), e);
        }
    }

    public boolean verifyReturnOrIpnSignature(Map<String, String> params) {
        // For return/ipn: MoMo sends signature for these fields (we verify best-effort).
        // Common fields: partnerCode, accessKey, requestId, amount, orderId, orderInfo, orderType,
        // transId, resultCode, message, payType, responseTime, extraData
        String signature = params.get("signature");
        if (!StringUtils.hasText(signature)) return false;

        // Build raw hash in a stable order using the fields we actually have.
        // NOTE: This must match MoMo's spec for your chosen API version; adjust if needed.
        String rawHash = "accessKey=" + nullToEmpty(params.get("accessKey"))
                + "&amount=" + nullToEmpty(params.get("amount"))
                + "&extraData=" + nullToEmpty(params.get("extraData"))
                + "&message=" + nullToEmpty(params.get("message"))
                + "&orderId=" + nullToEmpty(params.get("orderId"))
                + "&orderInfo=" + nullToEmpty(params.get("orderInfo"))
                + "&orderType=" + nullToEmpty(params.get("orderType"))
                + "&partnerCode=" + nullToEmpty(params.get("partnerCode"))
                + "&payType=" + nullToEmpty(params.get("payType"))
                + "&requestId=" + nullToEmpty(params.get("requestId"))
                + "&responseTime=" + nullToEmpty(params.get("responseTime"))
                + "&resultCode=" + nullToEmpty(params.get("resultCode"))
                + "&transId=" + nullToEmpty(params.get("transId"));

        String expected = hmacSha256(secretKey, rawHash);
        return expected.equals(signature);
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // MoMo expects hex string, not base64
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error: " + e.getMessage(), e);
        }
    }

    public record MomoCreateResponse(
            String payUrl,
            String deeplink,
            String qrCodeUrl,
            Integer resultCode,
            String message,
            String requestId
    ) {}
}

