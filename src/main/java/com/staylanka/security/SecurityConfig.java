package com.staylanka.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/register", "/rooms", "/rooms/**",
                                "/promotions", "/reviews", "/css/**", "/js/**", "/images/**",
                                "/uploads/**", "/error", "/error/**").permitAll()
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")

                        // Room & availability owner. Reservation and stay managers need room read access only.
                        .requestMatchers(HttpMethod.POST, "/staff/rooms/**").hasAnyRole("ROOM_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/staff/rooms/**").hasAnyRole(
                                "ROOM_MANAGER", "RESERVATION_MANAGER", "STAY_MANAGER", "ADMIN")
                        .requestMatchers("/admin/room-types/**").hasAnyRole("ROOM_MANAGER", "ADMIN")

                        // Reservation owner. Stay and inquiry managers may resolve references without modifying them.
                        .requestMatchers(HttpMethod.POST, "/staff/reservations/**").hasAnyRole("RESERVATION_MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/staff/reservations/**").hasAnyRole(
                                "RESERVATION_MANAGER", "STAY_MANAGER", "INQUIRY_REQUEST_MANAGER", "ADMIN")

                        // Customer profile owner plus minimum cross-module read access.
                        .requestMatchers(HttpMethod.GET, "/staff/customers/**").hasAnyRole(
                                "PROFILE_MANAGER", "RESERVATION_MANAGER", "STAY_MANAGER", "INQUIRY_REQUEST_MANAGER", "ADMIN")
                        .requestMatchers("/admin/customers/**").hasAnyRole("PROFILE_MANAGER", "ADMIN")

                        // Check-in/out and stay owner.
                        .requestMatchers("/staff/check-ins/**", "/staff/stays/**").hasAnyRole("STAY_MANAGER", "ADMIN")

                        // Inquiry / special-request owner.
                        .requestMatchers("/staff/requests/**").hasAnyRole("INQUIRY_REQUEST_MANAGER", "ADMIN")

                        // Promotion and review owner.
                        .requestMatchers("/admin/promotions/**", "/admin/reviews/**")
                                .hasAnyRole("PROMOTION_REVIEW_MANAGER", "ADMIN")

                        .requestMatchers("/staff/dashboard").hasAnyRole(
                                "ROOM_MANAGER", "RESERVATION_MANAGER", "PROFILE_MANAGER", "STAY_MANAGER",
                                "PROMOTION_REVIEW_MANAGER", "INQUIRY_REQUEST_MANAGER", "STAFF", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler((request, response, authentication) -> response.sendRedirect("/dashboard"))
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)));
        return http.build();
    }
}
