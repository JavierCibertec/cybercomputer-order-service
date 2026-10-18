package pe.edu.cibertec.cybercomputer.order.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/mis").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/*").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pedidos/*/estado").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> SecurityErrors.unauthorized(res))
                        .accessDeniedHandler((req, res, ex) -> SecurityErrors.forbidden(res)))
                .addFilterBefore(new FiltroJwtAuth(jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
