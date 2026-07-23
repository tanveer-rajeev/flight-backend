package com.aerionsoft.application.service.audit;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ActivityAuditInvocationCapture {

    private static final Pattern BOOKING_ID_PATH = Pattern.compile("/api/bookings/(\\d+)");
    private static final Pattern TICKET_ACTION_ID_PATH = Pattern.compile("/api/admin/ticket-actions/(\\d+)");
    private static final Pattern ADMIN_USER_ID_PATH = Pattern.compile("/api/admin/users/(\\d+)");
    private static final Pattern BUSINESS_ID_PATH = Pattern.compile("/api/admin/businesses/(\\d+)");

    public void enrichMutationMetadata(ProceedingJoinPoint joinPoint, HttpServletRequest request, Map<String, Object> metadata) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        metadata.put("controller", signature.getDeclaringType().getSimpleName());
        metadata.put("method", signature.getName());

        if (request != null) {
            metadata.put("httpMethod", request.getMethod());
            metadata.put("path", request.getRequestURI());
            if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
                metadata.put("query", request.getQueryString());
            }
        }

        captureSimpleParams(joinPoint, metadata);
        inferResourceFromPath(stringVal(metadata.get("path")), metadata);
        metadata.put("actionLabel", buildActionLabel(metadata));
    }

    private static void captureSimpleParams(ProceedingJoinPoint joinPoint, Map<String, Object> metadata) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (names == null || args == null) {
            return;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < names.length && i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || shouldSkipParam(arg)) {
                continue;
            }
            if (isSimpleValue(arg)) {
                params.put(names[i], arg);
            }
        }
        if (!params.isEmpty()) {
            metadata.put("params", params);
        }
    }

    private static void inferResourceFromPath(String path, Map<String, Object> metadata) {
        if (path == null || path.isBlank()) {
            return;
        }
        matchResource(path, BOOKING_ID_PATH, "BOOKING", metadata);
        matchResource(path, TICKET_ACTION_ID_PATH, "TICKET_ACTION_REQUEST", metadata);
        matchResource(path, ADMIN_USER_ID_PATH, "ADMIN_USER", metadata);
        matchResource(path, BUSINESS_ID_PATH, "BUSINESS", metadata);
    }

    private static void matchResource(String path, Pattern pattern, String resourceType, Map<String, Object> metadata) {
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            metadata.putIfAbsent("inferredResourceType", resourceType);
            metadata.putIfAbsent("inferredResourceId", matcher.group(1));
        }
    }

    static String buildActionLabel(Map<String, Object> metadata) {
        String controller = stringVal(metadata.get("controller"));
        String method = stringVal(metadata.get("method"));
        String httpMethod = stringVal(metadata.get("httpMethod"));
        String path = stringVal(metadata.get("path"));

        if (controller != null && method != null) {
            String base = controller + "." + method;
            if (httpMethod != null && path != null) {
                return httpMethod + " " + path + " (" + base + ")";
            }
            return base;
        }
        if (httpMethod != null && path != null) {
            return httpMethod + " " + path;
        }
        return "Admin action";
    }

    private static boolean shouldSkipParam(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof Authentication
                || arg instanceof BindingResult
                || arg instanceof MultipartFile
                || arg.getClass().isArray() && MultipartFile.class.isAssignableFrom(arg.getClass().getComponentType());
    }

    private static boolean isSimpleValue(Object arg) {
        return arg instanceof String
                || arg instanceof Number
                || arg instanceof Boolean
                || arg instanceof Enum<?>;
    }

    private static String stringVal(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
