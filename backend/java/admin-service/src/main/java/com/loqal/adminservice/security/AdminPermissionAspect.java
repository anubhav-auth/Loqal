package com.loqal.adminservice.security;

import com.loqal.adminservice.entity.AdminUser;
import com.loqal.adminservice.entity.dto.AdminPermission;
import com.loqal.adminservice.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminPermissionAspect {

    private final AdminUserRepository adminUserRepository;

    @Around("@annotation(com.loqal.adminservice.security.HasAdminPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        HasAdminPermission annotation = method.getAnnotation(HasAdminPermission.class);
        AdminPermission requiredPermission = annotation.value();

        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Admin user not registered"));

        if (!user.getPermissions().contains(requiredPermission)) {
            throw new RuntimeException("Forbidden – Missing required admin permission: " + requiredPermission);
        }

        return joinPoint.proceed();
    }
}

