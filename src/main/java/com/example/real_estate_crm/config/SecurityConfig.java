package com.example.real_estate_crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .cors().and()
	        .csrf().disable()
	        .authorizeHttpRequests()
	            .requestMatchers("/api/leads/**").permitAll()
	            .requestMatchers("/api/users/**").permitAll()
	            .requestMatchers("/api/properties/**").permitAll()
	            .requestMatchers("/api/notes/**").permitAll()
	            .requestMatchers("/api/notifications/**").permitAll()
	            .anyRequest().authenticated()
	        .and()
	        .formLogin().disable()    // disable form login FIRST
	        .httpBasic().disable()    // disable basic auth
	        .anonymous();             // THEN enable anonymous access at the END

	    return http.build();
	}



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://127.0.0.1:5500","http://127.0.0.1:5501","http://localhost:5173")); // Allow frontend to access API
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Specify allowed methods
        config.setAllowedHeaders(List.of("*"));  // Allow all headers
        config.setAllowCredentials(true); // Allow credentials (cookies, authorization headers, etc.)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // Apply CORS settings to all endpoints
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();  // Create the authentication manager
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Use BCrypt for password encoding
    }
}
