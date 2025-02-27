package sg.edu.nus.iss.voucher.core.workflow.jwt;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
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

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

	@Autowired
	AuthAPICall authApiCall;

	@Autowired
	private JSONReader jsonReader;

	@Autowired
	private AuditService auditLogService;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	private String userID;
	private String apiEndpoint;
	private HTTPVerb httpMethod;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorizationHeader = request.getHeader("Authorization");
		userID = Optional.ofNullable(request.getHeader("X-User-Id")).orElse("Invalid UserID");
		apiEndpoint = request.getRequestURI();
		httpMethod = HTTPVerb.fromString(request.getMethod());

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			String token = authorizationHeader.substring(7);
			String responseStr = authApiCall.validateToken(token, userID);

			try {
				JSONObject jsonResponse = jsonReader.parseJsonResponse(responseStr);
				Boolean isValid = jsonReader.getSuccessFromResponse(jsonResponse);
				String message = jsonReader.getMessageFromResponse(jsonResponse);
				int status = jsonReader.getStatusFromResponse(jsonResponse);

				if (!isValid) {
					handleException(response, message, status);
					return;
				}

			} catch (ParseException e) {
				handleException(response, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				e.printStackTrace();
			}
		}
		filterChain.doFilter(request, response);
	}

	private void handleException(HttpServletResponse response, String message, int status) throws IOException {
		TokenErrorResponse.sendErrorResponse(response, message, status, "UnAuthorized");
		AuditDTO auditDTO = auditLogService.createAuditDTO(userID, "", activityTypePrefix, apiEndpoint, httpMethod);
		auditLogService.logAudit(auditDTO, status, message);
	}
}
