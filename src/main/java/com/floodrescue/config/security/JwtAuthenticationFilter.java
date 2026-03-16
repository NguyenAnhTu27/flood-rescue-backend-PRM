package com.floodrescue.config.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, java.io.IOException {
    
        String auth = request.getHeader("Authorization");
    
        if (auth != null && auth.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = auth.substring(7);
    
            try {
                Claims claims = jwtTokenProvider.parseToken(token).getBody();
                Long userId = Long.parseLong(claims.getSubject());
    
                UserDetails userDetails = userDetailsService.loadUserById(userId);
    
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
    
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                ex.printStackTrace();   // ✅ thêm dòng này
                SecurityContextHolder.clearContext();
            }
        }
    
        chain.doFilter(request, response);
    }}