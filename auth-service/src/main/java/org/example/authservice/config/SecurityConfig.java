package org.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.authservice.repository.UserRepository;
import org.example.authservice.entity.User;

import java.io.IOException;


@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.security.oauth2.enabled:true}")
    private boolean oauth2Enabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RestTemplate restTemplate) throws Exception {

        // Create an inline OAuth2 success handler
        AuthenticationSuccessHandler oauth2SuccessHandler = new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                try {
                    String email = authentication.getName();
                    logger.info("OAuth2 authentication successful for: {}", email);

                    // Check if user exists, create if not
                    boolean isNewUser = !userRepository.existsByEmail(email);
                    User user = userRepository.findByEmail(email)
                            .orElseGet(() -> {
                                User newUser = new User();
                                newUser.setEmail(email);
                                newUser.setRole("ROLE_CUSTOMER");
                                newUser.setProvider("GITHUB");
                                return userRepository.save(newUser);
                            });

                    // Create wallet for new GitHub OAuth users
                    if (isNewUser) {
                        try {
                            String walletServiceUrl = "http://wallet-service:8085/wallet/create";
                            restTemplate.postForObject(walletServiceUrl, null, Object.class);
                            logger.info("Wallet created for new OAuth user: {}", user.getUserId());
                        } catch (Exception e) {
                            logger.warn("Failed to create wallet: {}", e.getMessage());
                        }
                    }

                    // Generate JWT token
                    String token = jwtUtil.generateToken(email);
                    logger.info("JWT token generated for: {}", email);

                    // Redirect to frontend with token
                    String redirectUrl = "http://localhost:5173/auth/callback?token=" + token;
                    logger.info("Redirecting to: {}", redirectUrl);
                    response.sendRedirect(redirectUrl);

                } catch (Exception e) {
                    logger.error("OAuth2 success handler error", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth failed");
                }
            }
        };

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/users",
                                "/oauth-success",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauth2Enabled) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(oauth2SuccessHandler)
            );
        } else {
            logger.info("OAuth2 login is disabled for this environment.");
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
                // Disable service-level CORS handling so API Gateway handles CORS centrally.
                // Returning an empty UrlBasedCorsConfigurationSource avoids adding Access-Control-Allow-* headers here.
                return new UrlBasedCorsConfigurationSource();
    }
}
