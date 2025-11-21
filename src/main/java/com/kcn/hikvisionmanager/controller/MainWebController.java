package com.kcn.hikvisionmanager.controller;

import com.kcn.hikvisionmanager.config.CameraConfig;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;


@Controller
@Slf4j
public class MainWebController {

    private final int mainTrackId;
    private final int subTrackId;

    public MainWebController(CameraConfig config) {
        this.mainTrackId = config.getTrackMain();
        this.subTrackId = config.getTrackSub();
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index(HttpSession httpSession, Principal principal, Model model) {
        String sessionId = httpSession.getId();
        String username = (principal != null) ? principal.getName() : "Guest";

        log.info("\uD83D\uDE46\uD83C\uDFFB\u200D♂\uFE0F User {} accessed index page, sessionId: {}", username, sessionId);

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("username", username);
        model.addAttribute("mainTrack", mainTrackId);
        model.addAttribute("subTrack", subTrackId);
        model.addAttribute("currentPath", "/index");

        return "index";
    }

    @GetMapping("/recordings")
    public String recordings(Principal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getName() : "Guest");
        model.addAttribute("currentPath", "/recordings");
        return "recordings";
    }

    @GetMapping("/backups")
    public String backups(Principal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getName() : "Guest");
        model.addAttribute("currentPath", "/backups");
        return "backups";
    }

    @GetMapping("/info")
    public String info(Principal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getName() : "Guest");
        model.addAttribute("currentPath", "/info");
        return "info";
    }

    @GetMapping("/backup-history")
    public String backupHistory(Principal principal, Model model) {
        model.addAttribute("username", principal != null ? principal.getName() : "Guest");
        model.addAttribute("currentPath", "/backup-history");
        return "backup-history";
    }
}
