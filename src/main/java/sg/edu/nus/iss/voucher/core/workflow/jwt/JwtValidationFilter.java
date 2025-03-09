package sg.edu.nus.iss.voucher.core.workflow.jwt;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.enums.HTTPVerb;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;
import sg.edu.nus.iss.voucher.core.workflow.utility.JSONReader;

import java.io.IOException;
import java.util.Optional;
import io.jsonwebtoken.*;

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

	@Autowired
	AuthAPICall authApiCall;

	@Autowired
	private JSONReader jsonReader;

	@Autowired
	private AuditService auditLogService;
	
	@Autowired
	private JWTUtility jwtUtility;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	private String userID;
	private String apiEndpoint;
	private HTTPVerb httpMethod;
	private String authorizationHeader = "";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		authorizationHeader = request.getHeader("Authorization");
		apiEndpoint = request.getRequestURI();
		httpMethod = HTTPVerb.fromString(request.getMethod());
		userID = "Invalid UserID";

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			try {
			userID = Optional.ofNullable(jwtUtility.retrieveUserID(authorizationHeader)).orElse("Invalid UserID");
			
			String responseStr = authApiCall.validateToken(authorizationHeader);

				JSONObject jsonResponse = jsonReader.parseJsonResponse(responseStr);
				Boolean isValid = jsonReader.getSuccessFromResponse(jsonResponse);

				if (!isValid) {
					String message = jsonReader.getMessageFromResponse(jsonResponse);
					int status = jsonReader.getStatusFromResponse(jsonResponse);
					handleException(response, message, status);
					return;
				}

			} catch (ExpiredJwtException e) {
				handleException(response, "JWT token is expired", HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} catch (MalformedJwtException e) {
				handleException(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} catch (SecurityException e) {
				handleException(response, "JWT signature is invalid", HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} catch (Exception e) {
				handleException(response, e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private void handleException(HttpServletResponse response, String message, int status) throws IOException {
		TokenErrorResponse.sendErrorResponse(response, message, status, "UnAuthorized");
		AuditDTO auditDTO = auditLogService.createAuditDTO(userID, "", activityTypePrefix, apiEndpoint, httpMethod);
		auditLogService.logAudit(auditDTO, status, message, authorizationHeader);
	}
}
