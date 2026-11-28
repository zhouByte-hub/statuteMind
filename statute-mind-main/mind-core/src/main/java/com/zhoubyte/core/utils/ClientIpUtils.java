package com.zhoubyte.core.utils;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtils {

    public static String getClientIp(HttpServletRequest request) {
        String ip = null;
        // 优先标准Forwarded头
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isBlank()) {
            ip = extractIpFromForwarded(forwarded);
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (!isUnknown(ip)) {
            // XFF逗号分割，取第一个
            ip = ip.split(",")[0].trim();
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        // 兜底：直连IP
        if (isUnknown(ip)) {
            ip = request.getRemoteAddr();
        }
        // 兼容ipv6本地回环
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private static boolean isUnknown(String ip) {
        return ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip);
    }

    private static String extractIpFromForwarded(String forwarded) {
        // 简单提取 for=xxx
        String[] arr = forwarded.split(";");
        for (String part : arr) {
            if (part.trim().startsWith("for=")) {
                return part.replace("for=", "").replace("\"", "").trim();
            }
        }
        return null;
    }
}
