package com.truckassist.backend.config;

import com.truckassist.backend.service.JwtService;
import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(
            JwtService jwtService) {

        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri =
                request.getRequestURI();

        String method =
                request.getMethod();

        System.out.println(
                "[JWT] ========================================"
        );

        System.out.println(
                "[JWT] Request: " +
                method +
                " " +
                uri
        );

        String authorization =
                request.getHeader("Authorization");

        // =====================================================
        // NO AUTHORIZATION HEADER
        // =====================================================

        if (authorization == null ||
                authorization.isBlank()) {

            System.out.println(
                    "[JWT] Authorization header: MISSING"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        System.out.println(
                "[JWT] Authorization header: PRESENT"
        );

        // =====================================================
        // INVALID HEADER
        // =====================================================

        if (!authorization.startsWith(
                "Bearer ")) {

            System.out.println(
                    "[JWT] Invalid Bearer header"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authorization
                        .substring(7)
                        .trim();

        if (token.isBlank()) {

            System.out.println(
                    "[JWT] Token is empty"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        // =====================================================
        // PARSE TOKEN
        // =====================================================

        try {

            Claims claims =
                    jwtService.parseToken(token);

            // =================================================
            // USER ID
            // =================================================

            String userId =
                    claims.getSubject();

            System.out.println(
                    "[JWT] Subject/User ID: " +
                    userId
            );

            if (userId == null ||
                    userId.isBlank()) {

                System.out.println(
                        "[JWT] ERROR: User ID missing from token"
                );

                SecurityContextHolder
                        .clearContext();

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }

            // =================================================
            // VALIDATE UUID
            // =================================================

            try {

                UUID.fromString(userId);

            } catch (IllegalArgumentException ex) {

                System.out.println(
                        "[JWT] ERROR: Invalid UUID in token: " +
                        userId
                );

                SecurityContextHolder
                        .clearContext();

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }

            // =================================================
            // ROLE
            // =================================================

            String role =
                    claims.get(
                            "role",
                            String.class
                    );

            System.out.println(
                    "[JWT] Role from token: " +
                    role
            );

            if (role == null ||
                    role.isBlank()) {

                System.out.println(
                        "[JWT] WARNING: Role missing"
                );
            }

            String normalizedRole =
                    role == null
                            ? "USER"
                            : role
                                    .trim()
                                    .toUpperCase();

            // Prevent ROLE_ROLE_DRIVER
            if (normalizedRole.startsWith(
                    "ROLE_")) {

                normalizedRole =
                        normalizedRole.substring(5);
            }

            String authority =
                    "ROLE_" +
                    normalizedRole;

            System.out.println(
                    "[JWT] Granted authority: " +
                    authority
            );

            // =================================================
            // CREATE AUTHENTICATION
            // =================================================

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority(
                                            authority
                                    )
                            )
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(
                            authentication
                    );

            System.out.println(
                    "[JWT] Authentication: SUCCESS"
            );

            System.out.println(
                    "[JWT] Authentication name: " +
                    authentication.getName()
            );

            System.out.println(
                    "[JWT] Is authenticated: " +
                    authentication.isAuthenticated()
            );

        } catch (Exception ex) {

            SecurityContextHolder
                    .clearContext();

            System.out.println(
                    "[JWT] ========================================"
            );

            System.out.println(
                    "[JWT] Authentication: FAILED"
            );

            System.out.println(
                    "[JWT] Error: " +
                    ex.getClass().getName()
            );

            System.out.println(
                    "[JWT] Message: " +
                    ex.getMessage()
            );

            System.out.println(
                    "[JWT] ========================================"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        System.out.println(
                "[JWT] Continuing filter chain..."
        );

        filterChain.doFilter(
                request,
                response
        );

        System.out.println(
                "[JWT] Response status: " +
                response.getStatus()
        );

        System.out.println(
                "[JWT] ========================================"
        );
    }
}