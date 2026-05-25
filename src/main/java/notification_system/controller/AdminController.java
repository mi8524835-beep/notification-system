package notification_system.controller;

import notification_system.domain.Notification;
import notification_system.domain.NotificationStatus;
import notification_system.repository.NotificationRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final NotificationRepository repository;

    public AdminController(
            NotificationRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Model model
    ) {

        List<Notification> failed =
                repository.findByStatus(
                        NotificationStatus.FAILED
                );

        model.addAttribute(
                "notifications",
                failed
        );

        model.addAttribute(
                "failedCount",
                repository.countByStatus(NotificationStatus.FAILED)
        );

        model.addAttribute(
                "processingCount",
                repository.countByStatus(NotificationStatus.PROCESSING)
        );

        model.addAttribute(
                "requestedCount",
                repository.countByStatus(NotificationStatus.REQUESTED)
        );

        model.addAttribute(
                "failedCount",
                repository.countByStatus(
                        NotificationStatus.FAILED
                )
        );

        model.addAttribute(
                "processingCount",
                repository.countByStatus(
                        NotificationStatus.PROCESSING
                )
        );

        model.addAttribute(
                "requestedCount",
                repository.countByStatus(
                        NotificationStatus.REQUESTED
                )
        );

        return "admin";
    }

}