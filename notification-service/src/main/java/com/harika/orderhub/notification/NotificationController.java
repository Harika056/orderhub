package com.harika.orderhub.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    public record NotificationView(String orderId, String message) {
    }

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/{orderId}")
    public List<NotificationView> forOrder(@PathVariable String orderId) {
        return notifications.findByOrderId(orderId).stream()
                .map(n -> new NotificationView(n.getOrderId(), n.getMessage()))
                .toList();
    }
}
