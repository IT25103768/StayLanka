package com.staylanka.security;

import com.staylanka.user.AppUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AppUserRepository users
    ) throws Exception {

        /*
         * IMPORTANT:
         *
         * Spring Security normally defers loading/creating the CSRF token
         * until it is actually needed.
         *
         * Thymeleaf needs the token when rendering a POST form.
         *
         * For this application, the HTML response can already be committed
         * by the time Thymeleaf reaches the login form. Since the default
         * CSRF repository stores the token in the HttpSession, Spring then
         * cannot create a session.
         *
         * Setting the attribute name to null causes Spring Security to load
         * the CSRF token eagerly during request processing.
         */
        XorCsrfTokenRequestAttributeHandler csrfRequestHandler =
                new XorCsrfTokenRequestAttributeHandler();

        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http

                /*
                 * Keep CSRF protection ENABLED.
                 *
                 * We only change deferred loading behavior.
                 */
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(csrfRequestHandler)
                )

                .authorizeHttpRequests(authorize -> authorize

                        /*
                         * Public password-recovery pages
                         */
                        .requestMatchers(
                                "/forgot-password",
                                "/reset-password"
                        )
                        .permitAll()

                        /*
                         * Public pages and static resources
                         */
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/rooms",
                                "/rooms/**",
                                "/promotions",
                                "/reviews",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error",
                                "/error/**"
                        )
                        .permitAll()

                        /*
                         * Customer area
                         */
                        .requestMatchers("/customer/**")
                        .hasRole("CUSTOMER")

                        /*
                         * =================================================
                         * ROOM MANAGEMENT
                         * =================================================
                         */

                        .requestMatchers(
                                "/staff/rooms/new",
                                "/staff/rooms/*/edit"
                        )
                        .hasAnyRole(
                                "ROOM_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/staff/rooms/**"
                        )
                        .hasAnyRole(
                                "ROOM_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/staff/rooms/**"
                        )
                        .hasAnyRole(
                                "ROOM_MANAGER",
                                "RESERVATION_MANAGER",
                                "STAY_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers("/admin/room-types/**")
                        .hasAnyRole(
                                "ROOM_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * RESERVATION MANAGEMENT
                         * =================================================
                         */

                        .requestMatchers(
                                "/staff/reservations/*/edit"
                        )
                        .hasAnyRole(
                                "RESERVATION_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/staff/reservations/**"
                        )
                        .hasAnyRole(
                                "RESERVATION_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/staff/reservations/**"
                        )
                        .hasAnyRole(
                                "RESERVATION_MANAGER",
                                "STAY_MANAGER",
                                "INQUIRY_REQUEST_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * CUSTOMER PROFILE MANAGEMENT
                         * =================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/staff/customers/**"
                        )
                        .hasAnyRole(
                                "PROFILE_MANAGER",
                                "RESERVATION_MANAGER",
                                "STAY_MANAGER",
                                "INQUIRY_REQUEST_MANAGER",
                                "ADMIN"
                        )

                        .requestMatchers("/admin/customers/**")
                        .hasAnyRole(
                                "PROFILE_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * CHECK-IN / CHECK-OUT / STAYS
                         * =================================================
                         */

                        .requestMatchers(
                                "/staff/check-ins/**",
                                "/staff/stays/**"
                        )
                        .hasAnyRole(
                                "STAY_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * INQUIRIES / SPECIAL REQUESTS
                         * =================================================
                         */

                        .requestMatchers("/staff/requests/**")
                        .hasAnyRole(
                                "INQUIRY_REQUEST_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * PROMOTIONS / REVIEWS
                         * =================================================
                         */

                        .requestMatchers(
                                "/admin/promotions/**",
                                "/admin/reviews/**"
                        )
                        .hasAnyRole(
                                "PROMOTION_REVIEW_MANAGER",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * STAFF DASHBOARD
                         * =================================================
                         */

                        .requestMatchers("/staff/dashboard")
                        .hasAnyRole(
                                "ROOM_MANAGER",
                                "RESERVATION_MANAGER",
                                "PROFILE_MANAGER",
                                "STAY_MANAGER",
                                "PROMOTION_REVIEW_MANAGER",
                                "INQUIRY_REQUEST_MANAGER",
                                "STAFF",
                                "ADMIN"
                        )

                        /*
                         * =================================================
                         * ADMIN
                         * =================================================
                         */

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        /*
                         * Any remaining staff URL that was not explicitly
                         * handled above requires ADMIN.
                         */
                        .requestMatchers("/staff/**")
                        .hasRole("ADMIN")

                        /*
                         * All other requests require authentication.
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * =====================================================
                 * LOGIN
                 * =====================================================
                 */
                .formLogin(form -> form

                        /*
                         * Thymeleaf login page.
                         */
                        .loginPage("/login")

                        /*
                         * Your HTML input is:
                         *
                         * <input name="email">
                         */
                        .usernameParameter("email")

                        /*
                         * Password input uses the default name:
                         *
                         * <input name="password">
                         */

                        .successHandler(
                                (request, response, authentication) ->
                                        response.sendRedirect("/dashboard")
                        )

                        .failureUrl("/login?error")

                        .permitAll()
                )

                /*
                 * =====================================================
                 * LOGOUT
                 * =====================================================
                 */
                .logout(logout -> logout

                        .logoutSuccessUrl("/?logout")

                        .invalidateHttpSession(true)

                        .clearAuthentication(true)

                        .deleteCookies("JSESSIONID")
                )

                /*
                 * =====================================================
                 * ACCESS DENIED
                 * =====================================================
                 */
                .exceptionHandling(exceptions -> exceptions

                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                );

        /*
         * Validate the authenticated user's account on every request.
         */
        http.addFilterAfter(
                new AccountStatusFilter(users),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}