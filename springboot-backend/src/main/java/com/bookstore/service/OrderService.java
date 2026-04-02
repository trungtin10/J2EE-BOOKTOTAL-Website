package com.bookstore.service;

import com.bookstore.model.Order;
import com.bookstore.model.OrderDetail;
import com.bookstore.model.Product;
import com.bookstore.repository.OrderDetailRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private com.bookstore.repository.CouponRepository couponRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public List<Order> searchOrders(String keyword, String status, String paymentStatus) {
        if ("all".equals(status)) status = null;
        if ("all".equals(paymentStatus)) paymentStatus = null;
        return orderRepository.searchOrders(keyword, status, paymentStatus);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrder(Order order, List<OrderDetail> details) {
        order.setOrderDate(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        for (OrderDetail detail : details) {
            detail.setOrder(savedOrder);
            orderDetailRepository.save(detail);

            Product product = detail.getProduct();
            if (product != null) {
                if (product.getQuantity() < detail.getQuantity()) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho.");
                }
                // Tồn kho chỉ trừ khi admin duyệt đơn (CONFIRMED), không trừ lúc đặt hàng
            }
        }

        // Cập nhật số lượng sử dụng cho mã giảm giá
        if (order.getCouponCode() != null) {
            final String code = order.getCouponCode();
            couponRepository.findByCodeAndIsActiveTrue(code).ifPresent(c -> {
                c.setUsedCount(c.getUsedCount() + 1);
                couponRepository.save(c);
            });
        }
        if (order.getShippingCouponCode() != null) {
            final String code = order.getShippingCouponCode();
            couponRepository.findByCodeAndIsActiveTrue(code).ifPresent(c -> {
                c.setUsedCount(c.getUsedCount() + 1);
                couponRepository.save(c);
            });
        }

        // Trigger notification
        if (savedOrder.getUser() != null) {
            notificationService.createNotification(
                savedOrder.getUser().getId(),
                "Đặt hàng thành công",
                "Đơn hàng #" + savedOrder.getId() + " của bạn đã được tiếp nhận thành công."
            );
        }

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.isCustomerOrderCancelled() && (status == null || !"CANCELLED".equalsIgnoreCase(status.trim()))) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể cập nhật trạng thái.");
        }
        String previous = order.getStatus();
        if (status != null && status.equals(previous)) {
            return;
        }

        boolean wasDeducted = Boolean.TRUE.equals(order.getStockDeducted());
        if (wasDeducted && ("CANCELLED".equals(status) || "PENDING".equals(status))) {
            restoreOrderInventory(order);
            order.setStockDeducted(false);
        }

        order.setStatus(status);

        if (("DELIVERING".equals(status) || "SHIPPED".equals(status) || "SHIPPING".equals(status))
                && (order.getTrackingCode() == null || order.getTrackingCode().isEmpty())) {
            long randomNum = (long) (Math.random() * 90000000L + 10000000L);
            order.setTrackingCode("GHN-" + randomNum);
            order.setExpectedDeliveryDate(LocalDateTime.now().plusDays(3));
        }

        if ("CONFIRMED".equals(status) && !Boolean.TRUE.equals(order.getStockDeducted())) {
            deductOrderInventory(order);
            order.setStockDeducted(true);
        }

        orderRepository.save(order);

        String title = "Cập nhật đơn hàng";
        String message = "Đơn hàng #" + order.getId() + " đã chuyển sang: " + Order.labelVietnamese(status) + ".";
        String type = "info";

        switch (status) {
            case "PENDING":
                title = "Đơn hàng chờ duyệt";
                message = "Đơn hàng #" + order.getId() + " đang ở trạng thái chờ duyệt.";
                type = "warning";
                break;
            case "CONFIRMED":
                title = "Đơn hàng đã được duyệt";
                message = "Đơn hàng #" + order.getId() + " đã được duyệt và sẽ được chuẩn bị giao.";
                break;
            case "PROCESSING":
                title = "Đơn hàng đang được chuẩn bị";
                message = "Đơn hàng #" + order.getId() + " đang được chuẩn bị.";
                break;
            case "SHIPPED":
                title = "Đơn hàng đang giao";
                message = "Đơn hàng #" + order.getId() + " đã có mã vận đơn: " + order.getTrackingCode() + ".";
                type = "warning";
                break;
            case "DELIVERING":
            case "SHIPPING":
                title = "Đơn hàng đang được giao";
                message = "Đơn hàng #" + order.getId() + " đang trên đường giao đến bạn."
                        + (order.getTrackingCode() != null && !order.getTrackingCode().isEmpty()
                        ? " Mã vận đơn: " + order.getTrackingCode() + "." : "");
                type = "warning";
                break;
            case "COMPLETED":
                title = "Đơn hàng đã giao";
                message = "Đơn hàng #" + order.getId() + " đã giao thành công. Cảm ơn bạn đã mua sắm!";
                type = "success";
                break;
            case "CANCELLED":
                title = "Đơn hàng đã hủy";
                message = "Đơn hàng #" + order.getId() + " đã bị hủy.";
                type = "danger";
                break;
            default:
                break;
        }

        if (order.getUser() != null) {
            notificationService.createNotification(order.getUser().getId(), title, message, type);
            String email = order.getUser().getEmail();
            if (email != null && !email.isBlank()) {
                String recipientName = resolveRecipientName(order);
                String detailHtml = buildOrderStatusEmailDetail(order, status);
                emailService.sendOrderStatusEmail(email, recipientName, order.getId(), Order.labelVietnamese(status), detailHtml);
            }
        }
    }

    private static String resolveRecipientName(Order order) {
        String n = order.getShippingName();
        if (n != null && !n.trim().isEmpty()) return n.trim();
        if (order.getUser() != null) {
            if (order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank()) {
                return order.getUser().getFullName().trim();
            }
            return order.getUser().getUsername();
        }
        return "Quý khách";
    }

    private static String buildOrderStatusEmailDetail(Order order, String status) {
        if (("DELIVERING".equals(status) || "SHIPPED".equals(status) || "SHIPPING".equals(status))
                && order.getTrackingCode() != null && !order.getTrackingCode().isBlank()) {
            return "<p><strong>Mã vận đơn:</strong> " + order.getTrackingCode() + "</p>";
        }
        if ("COMPLETED".equals(status)) {
            return "<p>Bạn có thể xem lại chi tiết đơn hàng trong mục <strong>Lịch sử đơn hàng</strong> trên website.</p>";
        }
        if ("CANCELLED".equals(status)) {
            return "<p>Nếu cần hỗ trợ, vui lòng liên hệ bộ phận chăm sóc khách hàng.</p>";
        }
        return "";
    }

    private void deductOrderInventory(Order order) {
        if (order.getOrderDetails() == null) {
            return;
        }
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getProduct() != null && d.getQuantity() != null && d.getQuantity() > 0) {
                productService.deductStockForOrderConfirmation(d.getProduct().getId(), d.getQuantity(), order.getId());
            }
        }
    }

    private void restoreOrderInventory(Order order) {
        if (order.getOrderDetails() == null) {
            return;
        }
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getProduct() != null && d.getQuantity() != null && d.getQuantity() > 0) {
                productService.restoreStockAfterOrderRelease(d.getProduct().getId(), d.getQuantity(), order.getId());
            }
        }
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.isCustomerOrderCancelled()) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể cập nhật thanh toán.");
        }
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void updatePaymentMethod(Long orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.isCustomerOrderCancelled()) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể cập nhật phương thức thanh toán.");
        }
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);
    }

    @Transactional
    public void updateShippingInfo(Long orderId, String shippingName, String shippingPhone, String shippingAddress, String orderNote) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.isCustomerOrderCancelled()) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể cập nhật thông tin giao hàng.");
        }
        order.setShippingName(shippingName);
        order.setShippingPhone(shippingPhone);
        order.setShippingAddress(shippingAddress);
        order.setOrderNote(orderNote);
        orderRepository.save(order);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public void updateOrderItemQuantity(Long orderId, Long itemId, Integer quantity) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderDetail item = orderDetailRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (!item.getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Mục này không thuộc về đơn hàng đã chọn");
        }

        Product product = item.getProduct();
        int diff = quantity - item.getQuantity();
        if (product != null && Boolean.TRUE.equals(order.getStockDeducted())) {
            if (product.getQuantity() < diff) {
                throw new RuntimeException("Số lượng tồn kho không đủ");
            }
            product.setQuantity(product.getQuantity() - diff);
            product.setSoldCount(product.getSoldCount() + diff);
            productRepository.save(product);
        }

        item.setQuantity(quantity);
        orderDetailRepository.save(item);
        
        recalculateOrderTotals(order);
    }

    @Transactional
    public void deleteOrderItem(Long orderId, Long itemId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        OrderDetail item = orderDetailRepository.findById(itemId).orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (!item.getOrder().getId().equals(orderId)) {
            throw new RuntimeException("Mục này không thuộc về đơn hàng đã chọn");
        }

        Product product = item.getProduct();
        if (product != null && Boolean.TRUE.equals(order.getStockDeducted())) {
            product.setQuantity(product.getQuantity() + item.getQuantity());
            product.setSoldCount(product.getSoldCount() - item.getQuantity());
            productRepository.save(product);
        }

        orderDetailRepository.delete(item);
        
        // Refresh items in memory for recalculation
        order.getOrderDetails().remove(item);
        recalculateOrderTotals(order);
    }

    private void recalculateOrderTotals(Order order) {
        double subtotal = 0;
        // Fetch fresh details if necessary or use the ones in memory
        for (OrderDetail detail : order.getOrderDetails()) {
            subtotal += detail.getPriceAtPurchase() * detail.getQuantity();
        }
        order.setTotalMoney(subtotal);
        
        // Final total = subtotal + shippingFee - discountAmount
        double finalTotal = subtotal + (order.getShippingFee() != null ? order.getShippingFee() : 0) 
                                     - (order.getDiscountAmount() != null ? order.getDiscountAmount() : 0);
        order.setFinalTotal(Math.max(0, finalTotal));
        
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.findById(id).ifPresent(order -> {
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreOrderInventory(order);
            }
            orderRepository.deleteById(id);
        });
    }
}
