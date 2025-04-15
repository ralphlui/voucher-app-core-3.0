package sg.edu.nus.iss.voucher.core.workflow.controller;

import static org.hamcrest.CoreMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.*; 
import sg.edu.nus.iss.voucher.core.workflow.entity.*; 
import sg.edu.nus.iss.voucher.core.workflow.enums.*;
import sg.edu.nus.iss.voucher.core.workflow.exception.CampaignNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.exception.VoucherNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.*;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;
import sg.edu.nus.iss.voucher.core.workflow.utility.JSONReader;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VoucherControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VoucherService voucherService;

	@MockitoBean
	private CampaignService campaignService;

	@MockitoBean
	private UserValidatorService userValidatorService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	AuthAPICall apiCall;

	@MockitoBean
	private JWTService jwtService;

	@MockitoBean
	private JSONReader jsonReader;

	@MockitoBean
	private AuditService auditService;

	private static List<VoucherDTO> mockVouchers = new ArrayList<>();
	private static Store store = new Store("1", "MUJI",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "M1", "");
	private static Campaign campaign = new Campaign("1", "new campaign 1", store, CampaignStatus.CREATED, null, 10, 0,
			null, null, 10, LocalDateTime.now(), LocalDateTime.now(), "U1", "", LocalDateTime.now(),
			LocalDateTime.now(), null, false);
	private static Voucher voucher1;
	private static Voucher voucher2 ;

	static String userId = "user123";
	static String authorizationHeader = "Bearer mock.jwt.token";

	@BeforeEach
	void setUp() throws Exception {
		voucher1 = new Voucher("1", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
				"U1");
		voucher2 = new Voucher("2", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
				"U1");

		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));
		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));

		JSONObject jsonObjet = new JSONObject();
		when(jsonReader.getActiveUser("12345", "mock.jwt.token")).thenReturn(jsonObjet);

		when(jwtService.extractUserID("mock.jwt.token")).thenReturn(userId);

		UserDetails mockUserDetails = mock(UserDetails.class);
		when(jwtService.getUserDetail(anyString(), anyString())).thenReturn(mockUserDetails);

		when(jwtService.validateToken(anyString(), eq(mockUserDetails))).thenReturn(true);

		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn(userId);
		
		AuditDTO mockAuditDTO = new AuditDTO();

		Mockito.when(auditService.createAuditDTO(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(), Mockito.any())).thenReturn(mockAuditDTO);

		Mockito.when(jwtService.retrieveUserID(Mockito.anyString())).thenReturn("testUser");

		Mockito.doNothing().when(auditService).logAudit(Mockito.any(), Mockito.anyInt(), Mockito.anyString(),
				Mockito.anyString());

	}

	@Test
	void testGetVoucherByVoucherId() throws Exception {
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setVoucherId(voucher2.getVoucherId());

		Mockito.when(voucherService.findByVoucherId(voucher2.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher2));
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers").header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message")
						.value("Successfully retrieved the voucher associated with the specified ID: "
								+ voucher2.getVoucherId()))
				.andDo(print());
	}

	@Test
	void testGetVoucherByVoucherId_EmptyVoucherId() throws Exception {
		// Setup
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setVoucherId("");

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers").header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expecting 400
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Bad Request:Voucher ID could not be blank.")).andDo(print());
	}

	@Test
	void testGetVoucherByVoucherId_NotFound() throws Exception {
		
		String voucherId = "111";
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setVoucherId(voucherId);
		VoucherDTO voucherDTO = new VoucherDTO();
		voucherDTO.setVoucherId("222");
		Mockito.when(voucherService.findByVoucherId(voucherId)).thenReturn(voucherDTO);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers").header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("The voucher could not be located using the specified voucher ID: " + voucherId))
				.andDo(print());
	}

	@Test
	void testGetVoucherByVoucherId_ExceptionHandling() throws Exception {

		String voucherId = "someVoucherId"; // Simulate a valid voucher ID
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setVoucherId(voucherId);
		Mockito.when(voucherService.findByVoucherId(voucherId)).thenThrow(new RuntimeException("Unexpected error"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers").header("Authorization", "Bearer some-token")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expecting 400
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message")
						.value("The attempt to retrieve the voucher ID someVoucherId was unsuccessful."))
				.andDo(print());
	}

	@Test
	void testClaimVoucher_Success() throws Exception {
		String campaignId = "campaign123";
		String claimedBy = "user123";
		String authorizationHeader = "Bearer test-token";

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(campaignId);
		voucherRequest.setClaimedBy(claimedBy);

		Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn("");
		Campaign campaign = new Campaign();
		campaign.setCampaignId(campaignId);
		Mockito.when(voucherService.validateCampaign(campaignId)).thenReturn(campaign);
		Mockito.when(voucherService.isVoucherAlreadyClaimed(claimedBy, campaign)).thenReturn(false);
		Mockito.when(voucherService.isCampaignFullyClaimed(campaignId, campaign)).thenReturn(false);
		VoucherDTO voucherDTO = new VoucherDTO();
		voucherDTO.setVoucherId("voucher123");
		Mockito.when(voucherService.claimVoucher(voucherRequest)).thenReturn(voucherDTO);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Voucher has been successfully claimed."));

		Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(200),
				eq("Voucher has been successfully claimed."), eq(authorizationHeader));
	}

	@Test
	void testClaimVoucher_VoucherAlreadyClaimed() throws Exception {
		String campaignId = "campaign123";
		String claimedBy = "user123";
		String authorizationHeader = "Bearer test-token";

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(campaignId);
		voucherRequest.setClaimedBy(claimedBy);

		Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn("");
		Campaign campaign = new Campaign();
		campaign.setCampaignId(campaignId);
		Mockito.when(voucherService.validateCampaign(campaignId)).thenReturn(campaign);
		Mockito.when(voucherService.isVoucherAlreadyClaimed(claimedBy, campaign)).thenReturn(true);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Voucher has already been claimed."));

		Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(400),
				eq("Voucher has already been claimed."), eq(authorizationHeader));
	}

	@Test
	void testClaimVoucher_CampaignFullyClaimed() throws Exception {
		String campaignId = "campaign123";
		String claimedBy = "user123";
		String authorizationHeader = "Bearer test-token";

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(campaignId);
		voucherRequest.setClaimedBy(claimedBy);

		Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn("");
		Campaign campaign = new Campaign();
		campaign.setCampaignId(campaignId);
		Mockito.when(voucherService.validateCampaign(campaignId)).thenReturn(campaign);
		Mockito.when(voucherService.isVoucherAlreadyClaimed(claimedBy, campaign)).thenReturn(false);
		Mockito.when(voucherService.isCampaignFullyClaimed(campaignId, campaign)).thenReturn(true);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Campaign has been fully claimed."));

		Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(401),
				eq("Campaign has been fully claimed."), eq(authorizationHeader));
	}

	@Test
	void testClaimVoucher_CampaignNotFound() throws Exception {
		String campaignId = "campaign123";
		String claimedBy = "user123";
		String authorizationHeader = "Bearer test-token";

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(campaignId);
		voucherRequest.setClaimedBy(claimedBy);

		Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn("");
		Mockito.when(voucherService.validateCampaign(campaignId))
				.thenThrow(new CampaignNotFoundException("Campaign not found"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Campaign not Found."));

		Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(404), eq("Campaign not Found."),
				eq(authorizationHeader));
	}
	
	@Test
	void testClaimVoucher_ExceptionHandling() throws Exception {
	    String campaignId = "campaign123";
	    String claimedBy = "user123";
	    String authorizationHeader = "Bearer test-token";
	    
	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId(campaignId);
	    voucherRequest.setClaimedBy(claimedBy);

	    Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn("");
	    Campaign campaign = new Campaign();
	    campaign.setCampaignId(campaignId);
	    Mockito.when(voucherService.validateCampaign(campaignId)).thenReturn(campaign);
	    Mockito.when(voucherService.isVoucherAlreadyClaimed(claimedBy, campaign)).thenReturn(false);
	    Mockito.when(voucherService.isCampaignFullyClaimed(campaignId, campaign)).thenReturn(false);

	    Mockito.when(voucherService.claimVoucher(Mockito.any())).thenThrow(new RuntimeException("Database error"));

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(new ObjectMapper().writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isBadRequest())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("The attempt to claim the voucher has been unsuccessful."));

	    Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(404), eq("The attempt to claim the voucher has been unsuccessful."), eq(authorizationHeader));
	}

	
	@Test
	void testClaimVoucher_UserValidationFailure() throws Exception {
	    String campaignId = "campaign123";
	    String claimedBy = "user123";
	    String authorizationHeader = "Bearer test-token";
	    
	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId(campaignId);
	    voucherRequest.setClaimedBy(claimedBy);

	    String validationMessage = "User validation failed";
	    Mockito.when(voucherService.validateUser(claimedBy, authorizationHeader)).thenReturn(validationMessage);

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(new ObjectMapper().writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isBadRequest())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(validationMessage));

	    Mockito.verify(auditService).logAudit(Mockito.any(AuditDTO.class), eq(400), eq(validationMessage), eq(""));
	}


	@Test
	void testGetAllVouchersClaimedBy() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
		Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
		mockVoucherMap.put(0L, mockVouchers);

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setClaimedBy(voucher2.getClaimedBy());

		Mockito.when(voucherService.findByClaimedByAndVoucherStatus(voucher2.getClaimedBy(),
				VoucherStatus.CLAIMED.toString(), pageable)).thenReturn(mockVoucherMap);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/users")
				.header("Authorization", authorizationHeader).param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].voucherId").value(1))
				.andDo(print());
	}

	@Test
	void testGetAllVouchersClaimedBy_BadRequest() throws Exception {

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setClaimedBy("");

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/users")
				.header("Authorization", authorizationHeader).param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false)) // Expect success = false
				.andExpect(
						MockMvcResultMatchers.jsonPath("$.message").value("Bad Request: User id could not be blank.")) // Check
																														// error
																														// message
				.andDo(MockMvcResultHandlers.print()); // Print response for debugging
	}

	@Test
	void testGetAllVouchersClaimedBy_NoVouchersForUser() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setClaimedBy(voucher2.getClaimedBy());

		Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
		mockVoucherMap.put(0L, Collections.emptyList());

		Mockito.when(voucherService.findByClaimedByAndVoucherStatus(voucher2.getClaimedBy(),
				VoucherStatus.CLAIMED.toString(), pageable)).thenReturn(mockVoucherMap);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/users")
				.header("Authorization", authorizationHeader).param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true)) // Expect success = true
				.andExpect(MockMvcResultMatchers.jsonPath("$.message")
						.value("No Voucher list for the specified user: " + voucher2.getClaimedBy())) // Check empty
																										// message
				.andDo(MockMvcResultHandlers.print()); // Print response for debugging
	}

	@Test
	void testGetAllVouchersClaimedBy_InternalServerError() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setClaimedBy(voucher2.getClaimedBy());

		Mockito.when(voucherService.findByClaimedByAndVoucherStatus(voucher2.getClaimedBy(),
				VoucherStatus.CLAIMED.toString(), pageable)).thenThrow(new RuntimeException("Internal server error"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/users")
				.header("Authorization", authorizationHeader).param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message")
						.value("Unable to retrieve the voucher for the specified user ID: " + voucher2.getClaimedBy()))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	void testGetAllVouchersByCampaignId() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
		Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
		mockVoucherMap.put(0L, mockVouchers);

		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(campaign.getCampaignId());

		Mockito.when(voucherService.findAllClaimedVouchersByCampaignId(campaign.getCampaignId(), pageable))
				.thenReturn(mockVoucherMap);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
				.header("Authorization", authorizationHeader).param("query", "").param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].voucherId").value(1))
				.andDo(print());
	}
	
	@Test
	void testFindAllClaimedVouchersByCampaignId_ValidRequest_WithVouchers() throws Exception {
	   
	    Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
	    Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
	    mockVoucherMap.put(10L, mockVouchers);

	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId("validCampaignId");

	    Mockito.when(voucherService.findAllClaimedVouchersByCampaignId("validCampaignId", pageable))
	            .thenReturn(mockVoucherMap);

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
	            .header("Authorization", authorizationHeader)
	            .param("page", "0")
	            .param("size", "10")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isOk())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].voucherId").value(1))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.totalRecord").value(10))
	            .andDo(print());
	}
	
	@Test
	void testFindAllClaimedVouchersByCampaignId_ValidRequest_NoVouchers() throws Exception {
	    
	    Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
	    Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
	    mockVoucherMap.put(0L, new ArrayList<>());

	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId("validCampaignId");
 
	    Mockito.when(voucherService.findAllClaimedVouchersByCampaignId("validCampaignId", pageable))
	            .thenReturn(mockVoucherMap);
 
	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
	            .header("Authorization", authorizationHeader)
	            .param("page", "0")
	            .param("size", "10")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isOk())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("No Voucher List Available for Campaign ID: validCampaignId"))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.data").isEmpty())
	            .andDo(print());
	}

	@Test
	void testFindAllClaimedVouchersByCampaignId_InvalidCampaignId() throws Exception {
	    
	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId("");
 
	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
	            .header("Authorization", authorizationHeader)
	            .param("page", "0")
	            .param("size", "10")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isBadRequest())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Bad Request:CampaignId could not be blank."))
	            .andDo(print());
	}

	
	@Test
	void testFindAllClaimedVouchersByCampaignId_ExceptionHandling() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
	    VoucherRequest voucherRequest = new VoucherRequest();
	    voucherRequest.setCampaignId("validCampaignId");

	    Mockito.when(voucherService.findAllClaimedVouchersByCampaignId("validCampaignId", pageable))
        .thenThrow(new RuntimeException("Service exception"));

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
	            .header("Authorization", authorizationHeader)
	            .param("page", "0")
	            .param("size", "10")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(voucherRequest)))
	            .andExpect(MockMvcResultMatchers.status().isBadRequest())
	            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
	            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Failed to get voucher for campaignId validCampaignId"))
	            .andDo(print());
	}



	@Test
	void testConsumeVoucher() throws Exception {

		Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher1));
		voucher1.setVoucherStatus(VoucherStatus.CONSUMED);
		Mockito.when(voucherService.consumeVoucher(voucher1.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher1));

		mockMvc.perform(
				MockMvcRequestBuilders.patch("/api/core/vouchers/consume").header("Authorization", authorizationHeader)
						.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(voucher1)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Voucher has been successfully consumed.")).andDo(print());
	}
	
	@Test
	void testConsumeVoucher_VoucherAlreadyConsumed() throws Exception {
	   
	    VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher1);
	    voucherDTO.setVoucherStatus(VoucherStatus.CONSUMED);
 
	    Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
	            .thenReturn(voucherDTO);
 
	    mockMvc.perform(
	            MockMvcRequestBuilders.patch("/api/core/vouchers/consume")
	                    .header("Authorization", authorizationHeader)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(voucher1)))
	            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("Voucher has already been consumed."))
	            .andDo(print());
	}
	
	@Test
	void testConsumeVoucher_VoucherNotFound() throws Exception {
	    
	    Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
	            .thenThrow(new VoucherNotFoundException("Voucher not found"));

	    mockMvc.perform(
	            MockMvcRequestBuilders.patch("/api/core/vouchers/consume")
	                    .header("Authorization", authorizationHeader)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(voucher1)))
	            .andExpect(MockMvcResultMatchers.status().isNotFound())
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("The attempt to consume the voucher has been unsuccessful."))
	            .andDo(print());
	}

	@Test
	void testConsumeVoucher_ConsumptionFailed() throws Exception {
	   
	    VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher1);
	    voucherDTO.setVoucherStatus(VoucherStatus.CLAIMED);
 
	    Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
	            .thenReturn(voucherDTO);
	     
	    Mockito.when(voucherService.consumeVoucher(voucher1.getVoucherId()))
	            .thenThrow(new RuntimeException("Consumption failed"));
 
	    mockMvc.perform(
	            MockMvcRequestBuilders.patch("/api/core/vouchers/consume")
	                    .header("Authorization", authorizationHeader)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(voucher1)))
	            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("The attempt to consume the voucher has been unsuccessful."))
	            .andDo(print());
	}

	@Test
	void testConsumeVoucher_FailureWithoutException() throws Exception {
	    
	    VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher1);
	    voucherDTO.setVoucherStatus(VoucherStatus.CLAIMED); 

	    
	    Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
	            .thenReturn(voucherDTO);
 
	    Mockito.when(voucherService.consumeVoucher(voucher1.getVoucherId()))
	            .thenReturn(voucherDTO);  
 
	    mockMvc.perform(
	            MockMvcRequestBuilders.patch("/api/core/vouchers/consume")
	                    .header("Authorization", authorizationHeader)
	                    .contentType(MediaType.APPLICATION_JSON)
	                    .content(objectMapper.writeValueAsString(voucher1)))
	            .andExpect(MockMvcResultMatchers.status().isInternalServerError()) // Expect 500 Internal Server Error
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(false))  // Success should be false
	            .andExpect(jsonPath("$.message").value("The attempt to consume the voucher has been unsuccessful."))  // Correct failure message
	            .andDo(print());
	}

	

}
