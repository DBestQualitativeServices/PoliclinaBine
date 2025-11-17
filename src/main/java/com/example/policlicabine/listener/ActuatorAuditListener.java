package com.example.policlicabine.listener;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.service.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Listener that audits access to Spring Boot Actuator endpoints.
 * <p>
 * Logs all successful authentications that access actuator endpoints to the
 * SecurityAuditLog system for monitoring and compliance purposes.
 * <p>
 * Actuator endpoints contain sensitive system information:
 * - /actuator/health - Application health status, database connectivity
 * - /actuator/info - Application metadata
 * - /actuator/auditevents - Spring Security audit events
 * <p>
 * Access to these endpoints is restricted to ADMIN and MANAGER roles
 * via SecurityConfig, and all access is logged for security monitoring.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActuatorAuditListener {

    private final SecurityAuditService auditService;

    /**
     * Listens for successful authentication events and logs actuator endpoint access.
     * <p>
     * This method is triggered after a user successfully authenticates via JWT
     * and accesses any actuator endpoint. The event is logged with:
     * - User principal (username)
     * - Actuator endpoint path
     * - IP address
     * - User role (extracted from authentication)
     * <p>
     * Log severity is INFO for normal operations, as actuator access is expected
     * for administrators and monitoring tools.
     *
     * @param event Spring Security authentication success event
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            // Get current HTTP request from thread-local context
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();

            // Skip if no HTTP request context (e.g., background tasks)
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String requestUri = request.getRequestURI();

            // Check if user is accessing an actuator endpoint
            if (requestUri.startsWith("/actuator/")) {
                String principal = event.getAuthentication().getName();
                String ipAddress = getClientIpAddress(request);
                String userRole = event.getAuthentication().getAuthorities().stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse("UNKNOWN");

                // Log actuator access event
                auditService.logEvent(SecurityAuditLogDto.builder()
                        .eventType(AuditEventType.ACTUATOR_ACCESS)
                        .severity(AuditSeverity.INFO)
                        .principal(principal)
                        .userRole(userRole)
                        .pathname(requestUri)
                        .url(request.getRequestURL().toString())
                        .ipAddress(ipAddress)
                        .reason("Actuator endpoint accessed: " + requestUri)
                        .sessionId(request.getSession(false) != null ?
                                request.getSession(false).getId() : null)
                        .build());

                log.debug("Actuator access logged: user={}, endpoint={}, ip={}",
                        principal, requestUri, ipAddress);
            }

        } catch (Exception e) {
            // Don't fail authentication if audit logging fails
            log.error("Failed to log actuator access event", e);
        }
    }

    /**
     * Extracts the client's real IP address from the HTTP request.
     * <p>
     * Checks multiple headers in order of precedence:
     * 1. X-Forwarded-For (proxy/load balancer)
     * 2. X-Real-IP (nginx proxy)
     * 3. RemoteAddr (direct connection)
     * <p>
     * This is important for accurate security auditing when the application
     * runs behind Azure App Service or a reverse proxy.
     *
     * @param request HTTP servlet request
     * @return Client IP address or "UNKNOWN" if cannot be determined
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "UNKNOWN";
    }
}
