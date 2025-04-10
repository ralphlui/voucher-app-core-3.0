package sg.edu.nus.iss.voucher.core.workflow.exception;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.iss.voucher.core.workflow.dto.APIResponse;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.pojo.User;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private JWTService jwtService;

	@MockBean
	private StoreNotFoundException storeNotFoundException;

	@MockBean
	private AuditService auditService;

	@Test
	void testGenericExceptionHandler() throws Exception {
		String userId = "user123";
		String authorizationHeader = "Bearer mock.jwt.token";

		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn(userId);

		ArgumentCaptor<AuditDTO> auditDTOCaptor = ArgumentCaptor.forClass(AuditDTO.class);

		doNothing().when(auditService).logAudit(auditDTOCaptor.capture(), eq(200), eq("message"),
				eq("authorizationHeader"));

		mockMvc.perform(get("/throw-exception")).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Authorization header is missing or invalid"));
	}


	@Test
	void testHandleObjectNotFoundException() {

		Exception exception = new Exception("An unexpected error occurred");

		GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

		ResponseEntity<APIResponse> response = exceptionHandler.handleObjectNotFoundException(exception);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

		assertEquals("Failed to get data. An unexpected error occurred", response.getBody().getMessage());
	}

	@Test
	void testHandleIllegalArgumentException() {

		IllegalArgumentException exception = new IllegalArgumentException("Invalid input data");

		GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

		ResponseEntity<APIResponse> response = exceptionHandler.illegalArgumentException(exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		assertEquals("Invalid data: Invalid input data", response.getBody().getMessage());
	}
}
