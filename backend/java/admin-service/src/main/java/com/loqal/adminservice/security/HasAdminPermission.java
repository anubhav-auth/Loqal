package com.loqal.adminservice.security;

import com.loqal.adminservice.entity.dto.AdminPermission;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasAdminPermission {
    AdminPermission value();
}
