package com.aerionsoft.application.config;

import com.aerionsoft.application.filters.ApiKeyFilter;
import com.aerionsoft.application.filters.ClientBasicAuthFilter;
import com.aerionsoft.application.filters.CustomAuthenticationEntryPoint;
import com.aerionsoft.application.filters.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    @Autowired
    private ClientBasicAuthFilter clientBasicAuthFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .anonymous(Customizer.withDefaults()) // Enable anonymous authentication for public endpoints
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/oauth/**").permitAll()
                        .requestMatchers("/api/admin/auth/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/admin/airport-airline/**").permitAll()
                        .requestMatchers("/api/flight/common/**").permitAll()
                        .requestMatchers("/api/flights/**").permitAll()
                        .requestMatchers("/api/core/callback/**").permitAll()
                        .requestMatchers("/api/webhooks/stripe").permitAll()
                        .requestMatchers("/api/v1/webhooks/tabby").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/businesses").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/files/upload/image").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/ws/**", "/api/ws/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(clientBasicAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


}
