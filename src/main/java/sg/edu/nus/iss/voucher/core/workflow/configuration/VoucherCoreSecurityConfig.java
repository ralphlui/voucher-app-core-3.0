package sg.edu.nus.iss.voucher.core.workflow.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;

import sg.edu.nus.iss.voucher.core.workflow.jwt.JwtFilter;

@Configuration
@EnableWebSecurity
public class VoucherCoreSecurityConfig {

	private static final String[] SECURED_URLS = { "/api/core/**" };
	
	@Value("${frontend.url}")
	private String frontEndUrl;

	@Bean
	public String getFrontEndUrl() {
		return frontEndUrl;
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
	    return http
	        .cors(cors -> {
	            cors.configurationSource(request -> {
	                CorsConfiguration config = new CorsConfiguration();
	                config.setAllowedOrigins(List.of(frontEndUrl));
	                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
	                config.setAllowedHeaders(List.of("*"));
	                config.setAllowCredentials(true);
	                config.applyPermitDefaultValues();
	                return config;
	            });
	        })
	        .headers(headers -> headers
	            .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Origin", "*"))
	            .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, OPTIONS"))
	            .addHeaderWriter(new StaticHeadersWriter("Access-Control-Allow-Headers", "*"))
	            .addHeaderWriter(new HstsHeaderWriter(31536000, false, true))
	            .addHeaderWriter((request, response) -> 
	                response.addHeader("Cache-Control", "max-age=60, must-revalidate")
	            )
	            .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
	                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-src 'none'; object-src 'none'; base-uri 'self'; form-action 'self';"))
	        )
	        // CSRF protection is disabled because JWT Bearer tokens are used for stateless authentication.
			.csrf(csrf -> csrf.disable()) // NOSONAR - CSRF is not required for JWT-based stateless authentication
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers(SECURED_URLS).permitAll()
	            .anyRequest().authenticated()
	        )
	        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
	        .build();
	}
	

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

}
