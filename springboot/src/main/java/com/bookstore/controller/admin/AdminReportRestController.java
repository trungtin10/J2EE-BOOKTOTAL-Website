package com.bookstore.controller.admin;

import com.bookstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportRestController {

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/revenue")
    public ResponseEntity<?> revenueReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "day") String groupBy
    ) {
        if (from == null || to == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu tham số from/to"));
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Khoảng thời gian không hợp lệ"));
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        List<String> labels = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        List<Long> orders = new ArrayList<>();

        if ("month".equalsIgnoreCase(groupBy)) {
            List<Object[]> rows = orderRepository.getRevenueByMonthRange(start, endExclusive);
            DateTimeFormatter ym = DateTimeFormatter.ofPattern("MM/yyyy");
            for (Object[] r : rows) {
                String key = String.valueOf(r[0]); // yyyy-MM
                LocalDate parsed = LocalDate.parse(key + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                labels.add(parsed.format(ym));
                revenues.add(toDouble(r[1]));
                orders.add(toLong(r[2]));
            }
        } else {
            List<Object[]> rows = orderRepository.getRevenueByDayRange(start, endExclusive);
            DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM");
            for (Object[] r : rows) {
                LocalDate d = toLocalDate(r[0]);
                labels.add(d.format(dmy));
                revenues.add(toDouble(r[1]));
                orders.add(toLong(r[2]));
            }
        }

        Double totalRevenue = orderRepository.getTotalRevenueRange(start, endExclusive);
        Long totalOrders = orderRepository.countOrdersRange(start, endExclusive);

        Map<String, Object> out = new HashMap<>();
        out.put("success", true);
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("groupBy", groupBy);
        out.put("labels", labels);
        out.put("revenues", revenues);
        out.put("ordersSeries", orders);
        out.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        out.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        return ResponseEntity.ok(out);
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof java.math.BigDecimal bd) return bd.doubleValue();
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof java.math.BigInteger bi) return bi.longValue();
        if (o instanceof java.math.BigDecimal bd) return bd.longValue();
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.time.LocalDate d) return d;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        // Fallback: try parse ISO
        try { return LocalDate.parse(String.valueOf(o)); } catch (Exception e) { }
        // Last resort
        return LocalDateTime.of(1970, 1, 1, 0, 0).toLocalDate();
    }
}

