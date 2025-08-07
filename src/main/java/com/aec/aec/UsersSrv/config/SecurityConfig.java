package com.aec.aec.UsersSrv.config;

import com.aec.aec.UsersSrv.security.JwtAuthenticationFilter;
import com.aec.aec.UsersSrv.service.CustomUserDetailsService;
import com.aec.aec.UsersSrv.util.JwtUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer; // <<-- AÑADE ESTA LÍNEA

import java.util.List;

// SecurityConfig.java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtUtil jwtUtil;
  private final CustomUserDetailsService userDetailsService;

  public SecurityConfig(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
    this.jwtUtil = jwtUtil;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);

    http
        .cors(Customizer.withDefaults()) // <<-- CAMBIO AQUÍ. Usará tu Bean corsConfigurationSource() si está presente.
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // Rutas públicas
            .requestMatchers(HttpMethod.GET, "/api/users/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users/by-username/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users/{username}").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users/request-password-reset").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users/solicitud-creador").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/users/contact").permitAll()


            // -->> AÑADE ESTAS LÍNEAS <<--
            .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/users/me/change-password").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated() // <-- ¡AÑADE ESTA LÍNEA! Para actualizar
                                                                              // el propio perfil
            .requestMatchers(HttpMethod.DELETE, "/api/users/me").authenticated() // <-- ¡AÑADE ESTA LÍNEA! Para eliminar
                                                                                 // el propio perfil

            // -->> FIN DE LÍNEAS AÑADIDAS <<--

            // Reglas para ADMIN
            .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/users/{id:\\d+}").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/users/{id:\\d+}/reset-password").hasAuthority("ROL_ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/users/{id:\\d+}").hasAuthority("ROL_ADMIN") // <-- ¡AÑADE ESTA LÍNEA!
                                                                                               // Para que el admin
                                                                                               // actualice otros
                                                                                               // usuarios
            .requestMatchers(HttpMethod.DELETE, "/api/users/{id:\\d+}").hasAuthority("ROL_ADMIN") // <-- ¡AÑADE ESTA
                                                                                                  // LÍNEA! Para que el
                                                                                                  // admin elimine otros
                                                                                                  // usuarios

            .anyRequest().authenticated())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configura CORS para permitir llamadas desde tu frontend
   */
  @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of("https://gateway-production-129e.up.railway.app","https://aecblock.com"));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}