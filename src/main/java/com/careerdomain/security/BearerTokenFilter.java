package com.careerdomain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenFilter extends org.springframework.web.filter.OncePerRequestFilter {
    private final TokenService tokens; private final CustomUserDetailsService users;
    public BearerTokenFilter(TokenService tokens, CustomUserDetailsService users) { this.tokens = tokens; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String email = tokens.subject(header.substring(7));
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    var details = users.loadUserByUsername(email);
                    var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ignored) {
                    // The account was removed after token issuance. Treat the token as unauthenticated.
                }
            }
        }
        chain.doFilter(request, response);
    }
}
