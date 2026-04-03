package com.bookstore.service;



import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;



import jakarta.annotation.PostConstruct;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;

import java.util.*;



/**

 * Tích hợp VNPAY theo tài liệu

 * <a href="https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html">PAY API</a>:

 * HMAC-SHA512, sort tham số, chuỗi hash giống mẫu PHP (urlencode cả key và value).

 */

@Service

public class VnpayPaymentService {



    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);



    @Value("${vnpay.tmn-code:disabled-vnpay-tmn}")

    private String tmnCodeRaw;



    @Value("${vnpay.hash-secret:disabled-vnpay-secret}")

    private String hashSecretRaw;



    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")

    private String payUrl;



    private String tmnCode;

    private String hashSecret;



    @PostConstruct

    void initVnpaySecrets() {

        this.tmnCode = tmnCodeRaw != null ? tmnCodeRaw.trim() : "";

        this.hashSecret = hashSecretRaw != null ? hashSecretRaw.trim() : "";

    }



    public boolean isConfigured() {

        return StringUtils.hasText(tmnCode)

                && StringUtils.hasText(hashSecret)

                && !tmnCode.startsWith("disabled-")

                && !hashSecret.startsWith("disabled-");

    }



    public String createPaymentUrl(long orderId, long amountVnd, String baseUrl, HttpServletRequest req) {

        if (!isConfigured()) {

            throw new IllegalStateException("VNPAY keys are not configured (VNPAY_TMN_CODE/VNPAY_HASH_SECRET).");

        }

        long amount = Math.max(0, amountVnd) * 100L;

        String txnRef = orderId + "-" + randomDigits(6);

        String ipAddr = normalizeIpForVnpay(resolveClientIp(req));



        Map<String, String> params = new LinkedHashMap<>();

        params.put("vnp_Version", "2.1.0");

        params.put("vnp_Command", "pay");

        params.put("vnp_TmnCode", tmnCode);

        params.put("vnp_Amount", String.valueOf(amount));

        params.put("vnp_CurrCode", "VND");

        params.put("vnp_TxnRef", txnRef);

        // Tiếng Việt không dấu, tránh ký tự đặc biệt (theo bảng tham số tài liệu)

        params.put("vnp_OrderInfo", "Thanh toan don hang ma " + orderId);

        params.put("vnp_OrderType", "other");

        params.put("vnp_Locale", "vn");

        params.put("vnp_ReturnUrl", baseUrl.replaceAll("/+$", "") + "/payment/vnpay/return");

        params.put("vnp_IpAddr", ipAddr);



        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");

        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        String createDate = fmt.format(cal.getTime());

        params.put("vnp_CreateDate", createDate);

        cal.add(Calendar.MINUTE, 15);

        params.put("vnp_ExpireDate", fmt.format(cal.getTime()));



        String hashData = buildHashDataOfficial(params);

        String secureHash = hmacSHA512(hashSecret, hashData);

        String query = buildQueryString(params) + "&vnp_SecureHash=" + secureHash;



        log.info("[VNPAY] create pay url tmnCode={}, returnUrl={}, txnRef={}, amountMinor={}, hashPrefix={}",

                tmnCode,

                params.get("vnp_ReturnUrl"),

                params.get("vnp_TxnRef"),

                params.get("vnp_Amount"),

                secureHash != null && secureHash.length() >= 12 ? secureHash.substring(0, 12) : secureHash);

        if (log.isDebugEnabled()) {

            log.debug("[VNPAY] hashData={}", hashData);

        }



        return payUrl + "?" + query;

    }



    /**

     * Xác thực query Return / IPN: chỉ loại {@code vnp_SecureHash} khỏi tập tham số khi tạo chuỗi ký

     * (đúng mẫu PHP IPN trong tài liệu; giữ {@code vnp_SecureHashType} nếu VNPAY gửi kèm).

     */

    public boolean verifyReturn(Map<String, String> params) {

        if (!isConfigured()) return false;

        if (params == null || params.isEmpty()) return false;



        String secureHash = params.get("vnp_SecureHash");

        if (!StringUtils.hasText(secureHash)) return false;



        Map<String, String> toSign = new HashMap<>(params);

        toSign.remove("vnp_SecureHash");



        String hashData = buildHashDataOfficial(toSign);

        String expected = hmacSHA512(hashSecret, hashData);

        boolean ok = expected != null && expected.equalsIgnoreCase(secureHash.trim());

        if (!ok) {

            log.warn("[VNPAY] verify checksum failed. hashData(first220)={}, expectedPrefix={}, gotPrefix={}",

                    hashData != null && hashData.length() > 220 ? hashData.substring(0, 220) + "..." : hashData,

                    expected != null && expected.length() >= 16 ? expected.substring(0, 16) : expected,

                    secureHash.length() >= 16 ? secureHash.substring(0, 16) : secureHash);

        }

        return ok;

    }



    /** Số tiền VNPAY gửi lại (đã nhân 100). */

    public static long parseAmountMinorUnits(Map<String, String> params) {

        if (params == null) return -1;

        String a = params.get("vnp_Amount");

        if (!StringUtils.hasText(a)) return -1;

        try {

            return Long.parseLong(a.trim());

        } catch (NumberFormatException e) {

            return -1;

        }

    }



    public static Long extractOrderId(Map<String, String> params) {

        if (params == null) return null;



        String txnRef = params.get("vnp_TxnRef");

        if (StringUtils.hasText(txnRef)) {

            String s = txnRef.trim();

            int dash = s.indexOf('-');

            if (dash > 0) s = s.substring(0, dash);

            try {

                return Long.parseLong(s);

            } catch (Exception ignored) {

                /* fall through */

            }

        }



        String info = params.get("vnp_OrderInfo");

        if (StringUtils.hasText(info)) {

            String digits = info.replaceAll(".*?(\\d+)\\s*$", "$1");

            try {

                return Long.parseLong(digits);

            } catch (Exception ignored) {

                /* fall through */

            }

        }

        return null;

    }



    /**

     * Chuỗi HMAC: sort key tăng dần, bỏ value rỗng, mỗi cặp {@code urlencode(key)=urlencode(value)},

     * nối {@code &} — theo đoạn PHP trong tài liệu VNPAY.

     */

    private static String buildHashDataOfficial(Map<String, String> params) {

        List<String> keys = new ArrayList<>(params.keySet());

        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();

        for (String k : keys) {

            if (k == null || !k.startsWith("vnp_")) continue;

            if ("vnp_SecureHash".equals(k)) continue;

            String v = params.get(k);

            if (!StringUtils.hasText(v)) continue;

            if (sb.length() > 0) sb.append('&');

            sb.append(urlEncode(k)).append('=').append(urlEncode(v));

        }

        return sb.toString();

    }



    private static String buildQueryString(Map<String, String> params) {

        List<String> keys = new ArrayList<>(params.keySet());

        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();

        for (String k : keys) {

            String v = params.get(k);

            if (!StringUtils.hasText(v)) continue;

            if (sb.length() > 0) sb.append('&');

            sb.append(urlEncode(k)).append('=').append(urlEncode(v));

        }

        return sb.toString();

    }



    private static String urlEncode(String s) {

        if (s == null) return "";

        try {

            return URLEncoder.encode(s, StandardCharsets.UTF_8);

        } catch (Exception e) {

            return "";

        }

    }



    private static String hmacSHA512(String key, String data) {

        if (key == null || data == null) return null;

        try {

            Mac hmac = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");

            hmac.init(secretKey);

            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(bytes.length * 2);

            for (byte b : bytes) sb.append(String.format("%02x", b));

            return sb.toString();

        } catch (Exception e) {

            return null;

        }

    }



    private static String randomDigits(int n) {

        Random r = new Random();

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) sb.append(r.nextInt(10));

        return sb.toString();

    }



    private static String resolveClientIp(HttpServletRequest req) {

        String xff = req.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(xff)) {

            String first = xff.split(",")[0].trim();

            if (StringUtils.hasText(first)) return first;

        }

        String realIp = req.getHeader("X-Real-IP");

        if (StringUtils.hasText(realIp)) return realIp.trim();

        return req.getRemoteAddr();

    }



    /**

     * vnp_IpAddr: [7,45] ký tự (tài liệu). Chuẩn hóa IPv6 localhost → IPv4; cắt nếu quá dài.

     */

    static String normalizeIpForVnpay(String ip) {

        if (!StringUtils.hasText(ip)) return "127.0.0.1";

        String s = ip.trim();

        if ("0:0:0:0:0:0:0:1".equals(s) || "::1".equals(s)) return "127.0.0.1";

        if (s.length() > 45) return s.substring(0, 45);

        return s;

    }

}

