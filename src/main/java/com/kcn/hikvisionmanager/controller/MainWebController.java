package com.kcn.hikvisionmanager.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
public class MainWebController {

    @GetMapping("/")
    public String index(HttpServletResponse response, Model model) {

        // sessionId
        String sessionId = UUID.randomUUID().toString();

        Cookie cookie = new Cookie("sessionId", sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 h

        response.addCookie(cookie);

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("currentPath", "/index");

        return "index";
    }

    @GetMapping("/recordings")
    public String recordings(Model model) {
        model.addAttribute("currentPath", "/recordings");
        return "recordings";
    }

    @GetMapping("/backups")
    public String backups(Model model) {
        model.addAttribute("currentPath", "/backups");
        return "backups";
    }

    @GetMapping("/info")
    public String info(Model model) {
        model.addAttribute("currentPath", "/info");
        return "info";
    }

    @GetMapping("/backup-history")
    public String backupHistory(Model model) {
        model.addAttribute("currentPath", "/backup-history");
        return "backup-history";
    }
}
