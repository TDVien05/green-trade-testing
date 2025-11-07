package Green_trade.green_trade_platform.filter;

import Green_trade.green_trade_platform.exception.AuthException;
import Green_trade.green_trade_platform.service.implement.UserDetailsServiceCustomer;
import Green_trade.green_trade_platform.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceCustomer userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        if (path.startsWith("/api/v1/auth") || path.startsWith("/ws") || path.startsWith("/queue")
                || path.startsWith("/api/v1/vnpay/return") || path.startsWith("/api/v1/vnpay/ipn")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/verify-otp") || path.startsWith("/api/test/redis")
                || path.startsWith("/forgot-password") || path.startsWith("/verify-otp-forgot-password")
                || path.startsWith("/verify-username-forgot-password") || path.startsWith("/signin-google")
                || path.startsWith("/api/v1/post-product")) {
            filterChain.doFilter(request, response);
            return;
        }


        log.info(">>> [Auth filter] Authentication in request : {}", request.getRequestURI());

        try {
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtils.verifyToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.info(">>> [Auth filter] User detail loading: {}", userDetails.getUsername());

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                log.info(">>> [Auth filter] Role from JWT: {}", userDetails.getAuthorities());

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            // Ném ra AuthenticationException để EntryPoint xử lý (trả về 401 JSON)
            throw new AuthException(">>> [Auth filter] Authentication failed: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String jwt = jwtUtils.getTokenFromRequest(request);
        log.info("AuthTokenFilter.java: {}", jwt);
        return jwt;
    }
}
