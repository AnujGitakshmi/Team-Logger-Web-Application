
package com.example.TeamLoggerC.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClientIpAddressService {

    private static final Logger log =
            LoggerFactory.getLogger(ClientIpAddressService.class);

    public String getClientIp(HttpServletRequest request) {

        if (request == null) {
            return "UNKNOWN";
        }

        // Only read forwarding headers from a trusted proxy.
        if (isBehindTrustedProxy(request)) {

            String forwardedFor =
                    request.getHeader("X-Forwarded-For");

            String forwardedIp = extractFirstIp(forwardedFor);

            if (isValidIp(forwardedIp)) {
                String normalizedIp = normalizeIp(forwardedIp);

                log.debug("Client IP obtained from trusted proxy: {}",
                        normalizedIp);

                return normalizedIp;
            }
        }

        String remoteAddress = request.getRemoteAddr();

        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "UNKNOWN";
        }

        remoteAddress = normalizeIp(remoteAddress);

        log.info(
                "CLIENT IP | RemoteAddr={} | ResolvedIP={} | RequestURI={}",
                request.getRemoteAddr(),
                remoteAddress,
                request.getRequestURI()
        );

        if ("::1".equals(remoteAddress)
                || "127.0.0.1".equals(remoteAddress)) {
            log.warn(
                    "Local loopback connection detected. "
                            + "The actual public client IP is not available "
                            + "from this localhost request."
            );
        }

        return remoteAddress;
    }

    private String extractFirstIp(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.split(",")[0].trim();
    }

    private boolean isValidIp(String value) {

        return value != null
                && !value.isBlank()
                && !"unknown".equalsIgnoreCase(value.trim())
                && !"null".equalsIgnoreCase(value.trim());
    }

    private String normalizeIp(String ip) {

        if (ip == null) {
            return null;
        }

        ip = ip.trim();

        if ("0:0:0:0:0:0:0:1".equalsIgnoreCase(ip)) {
            return "::1";
        }

        if ("::ffff:127.0.0.1".equalsIgnoreCase(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }

    private boolean isBehindTrustedProxy(
            HttpServletRequest request) {

        // Keep false until a real, trusted reverse proxy
        // is configured and its source address is verified.
        return false;
    }
}
