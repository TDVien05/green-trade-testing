package Green_trade.green_trade_platform.config;

import Green_trade.green_trade_platform.filter.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/verify-otp",
            "/api/test/redis",
            "/ws/**",
            "/queue/**"
    );

    @Autowired
    private AuthTokenFilter filter;

    @Autowired
    private AuthEntryPoint authEntryPoint;

    @Bean
    public DelegatingPasswordEncoder passwordEncoder() {
        return (DelegatingPasswordEncoder) (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())       // turn off CORS
                .csrf(AbstractHttpConfigurer::disable)       // turn off CSRF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST.toArray(new String[0])).permitAll() // Permit url in WHITE_LIST that do not need to authenticated
                        .requestMatchers("/error",
                                "/api/v1/vnpay/return",
                                "/api/v1/vnpay/ipn",
                                "/api/v1/post-product/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))//  Add AuthTokenFilter to SecurityFilterChain
                .formLogin(AbstractHttpConfigurer::disable) // Turn off basic authentication form from Spring Security
                .httpBasic(AbstractHttpConfigurer::disable); // Turn off Basic Auth

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Nếu dùng cookie/token
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
