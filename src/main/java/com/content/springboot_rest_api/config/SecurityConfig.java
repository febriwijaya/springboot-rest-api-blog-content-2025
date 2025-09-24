package com.content.springboot_rest_api.config;

import com.content.springboot_rest_api.security.JwtAuthenticationFilter;
import com.content.springboot_rest_api.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint unauthorizedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // encoder untuk hash password user
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .authorizeHttpRequests(auth -> auth
                        //  endpoint auth public
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        //  Articles public hanya GET
                        .requestMatchers(HttpMethod.GET, "/api/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/slug/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/approved-articles").permitAll()

                        //  Categories public hanya GET
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/{slug}/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/approved").permitAll()

                        // Tags public hanya GET
                        .requestMatchers(HttpMethod.GET, "/api/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/{slug}/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/slug/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tags/approved").permitAll()

                        // Comments public hanya GET
                        .requestMatchers(HttpMethod.GET, "/api/comment/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comment/article/{articleId}").permitAll()

                        //  lainnya butuh authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
