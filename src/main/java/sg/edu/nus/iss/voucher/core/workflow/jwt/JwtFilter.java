package sg.edu.nus.iss.voucher.core.workflow.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.enums.HTTPVerb;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;

import java.io.IOException;

import io.jsonwebtoken.*;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JWTService jwtService;
	
	private final AuditService auditLogService;

	public JwtFilter(JWTService jwtService,AuditService auditLogService) {
		this.jwtService = jwtService;
		this.auditLogService= auditLogService;
	}

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	private String userID;
	private String apiEndpoint;
	private HTTPVerb httpMethod;
	private String authorizationHeader;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		authorizationHeader = request.getHeader("Authorization");
		userID = "Invalid UserID";
		apiEndpoint = request.getRequestURI();
		httpMethod = HTTPVerb.fromString(request.getMethod());

		httpMethod = HTTPVerb.fromString(request.getMethod());
		String requestURI = request.getRequestURI();

		if (requestURI.equalsIgnoreCase("/api/core/campaigns") && httpMethod.equals(HTTPVerb.GET)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			handleException(response, "Authorization header is missing or invalid",
					HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String jwtToken = authorizationHeader.substring(7);

		try {
			userID = jwtService.retrieveUserID(authorizationHeader);
			
			UserDetails userDetails = jwtService.getUserDetail(authorizationHeader, jwtToken);
			if (jwtService.validateToken(jwtToken, userDetails)) {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} else {
				handleException(response, "Invalid or expired JWT token", HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		} catch (ExpiredJwtException e) {
			handleException(response, "JWT token is expired", HttpServletResponse.SC_UNAUTHORIZED);
			return;
		} catch (MalformedJwtException | SecurityException e) {
			handleException(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
			return;
		} catch (Exception e) {
			handleException(response, e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void handleException(HttpServletResponse response, String message, int status) throws IOException {
		TokenErrorResponse.sendErrorResponse(response, message, status, "UnAuthorized");
		AuditDTO auditDTO = auditLogService.createAuditDTO(userID, "", activityTypePrefix, apiEndpoint, httpMethod);
		auditLogService.logAudit(auditDTO, status, message, authorizationHeader);
	}

}
