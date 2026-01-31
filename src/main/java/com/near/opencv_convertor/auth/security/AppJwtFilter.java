package com.near.opencv_convertor.auth.security;

import com.near.opencv_convertor.firebase.dto.FirebaseUserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppJwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.validateToken(token);
            String uid = claims.getSubject();
            String email = claims.get("email", String.class);

            FirebaseUserPrincipal principal = new FirebaseUserPrincipal(uid, email);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of()
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}
