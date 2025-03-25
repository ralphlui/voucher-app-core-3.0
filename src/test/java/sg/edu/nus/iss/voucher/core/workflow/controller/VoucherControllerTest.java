package sg.edu.nus.iss.voucher.core.workflow.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherRequest;
import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.entity.Voucher;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.CampaignService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.VoucherService;
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


	@MockBean
	private VoucherService voucherService;
	
	@MockBean
	private CampaignService campaignService;
	
	@MockBean
	private UserValidatorService userValidatorService;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@MockBean
	AuthAPICall apiCall;

	@MockBean
	private JWTService jwtService;

	@MockBean
	private JSONReader jsonReader;
	
	@MockBean
    private AuditService auditService;

	private static List<VoucherDTO> mockVouchers = new ArrayList<>();
	private static Store store = new Store("1", "MUJI",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "M1", "");
	private static Campaign campaign = new Campaign("1", "new campaign 1", store, CampaignStatus.CREATED, null, 10, 0,
			null, null, 10, LocalDateTime.now(), LocalDateTime.now(), "U1", "", LocalDateTime.now(),
			LocalDateTime.now(), null,"Clothes", false);
	private static Voucher voucher1 = new Voucher("1", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
			"U1");
	private static Voucher voucher2 = new Voucher("2", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
			"U1");
	
	static String userId ="user123";
	static String authorizationHeader = "Bearer mock.jwt.token";
	
	@BeforeEach
	void setUp() throws Exception {
		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));
		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));

		JSONObject jsonObjet = new JSONObject();
		when(jsonReader.getActiveUser("12345", "mock.jwt.token")).thenReturn(jsonObjet);

		when(jwtService.extractUserID("mock.jwt.token")).thenReturn(userId);

		UserDetails mockUserDetails = mock(UserDetails.class);
		when(jwtService.getUserDetail(anyString(), anyString())).thenReturn(mockUserDetails);

		when(jwtService.validateToken(anyString(), eq(mockUserDetails))).thenReturn(true);

		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn(userId);
		ArgumentCaptor<AuditDTO> auditDTOCaptor = ArgumentCaptor.forClass(AuditDTO.class);
		   
	    doNothing().when(auditService).logAudit(auditDTOCaptor.capture(), eq(200), eq("message"), eq("authorizationHeader"));


	}


	@Test
	void testGetVoucherByVoucherId() throws Exception {
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setVoucherId(voucher2.getVoucherId());
		
		Mockito.when(voucherService.findByVoucherId(voucher2.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher2));
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voucherRequest)))
		        .andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))	
				.andExpect(jsonPath("$.message").value("Successfully retrieved the voucher associated with the specified ID: " + voucher2.getVoucherId())).andDo(print());
	}
	
	@Test
	void testClaimVoucher() throws Exception {
		
		Mockito.when(userValidatorService.validateActiveUser(voucher1.getClaimedBy(),  UserRoleType.CUSTOMER.toString(), "")).thenReturn(new HashMap<>());

		Mockito.when(campaignService.findById(campaign.getCampaignId())).thenReturn(Optional.of(campaign));

		Mockito.when(voucherService.claimVoucher(Mockito.any(VoucherRequest.class)))
				.thenReturn(DTOMapper.toVoucherDTO(voucher1));
		
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(voucher1.getCampaign().getCampaignId());
		voucherRequest.setClaimedBy(voucher1.getClaimedBy());

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim")
				.header("Authorization",authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voucherRequest))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))	
				.andExpect(jsonPath("$.message").value("Voucher has been successfully claimed.")).andDo(print());
	}
	
	@Test
	void testGetAllVouchersClaimedBy() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("claimTime").ascending());
		Map<Long, List<VoucherDTO>> mockVoucherMap = new HashMap<>();
		mockVoucherMap.put(0L, mockVouchers);
		
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setClaimedBy(voucher2.getClaimedBy());

		Mockito.when(voucherService.findByClaimedByAndVoucherStatus(voucher2.getClaimedBy(),VoucherStatus.CLAIMED.toString(), pageable))
				.thenReturn(mockVoucherMap);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/users")
				.header("Authorization", authorizationHeader).param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].voucherId").value(1))
				.andDo(print());
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
		mockMvc.perform(
				MockMvcRequestBuilders.post("/api/core/vouchers/campaigns")
				.header("Authorization", authorizationHeader).param("query", "").param("page", "0").param("size", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(voucherRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].voucherId").value(1))
				.andDo(print());
	}
	

	@Test
	void testConsumeVoucher() throws Exception {

		Mockito.when(voucherService.findByVoucherId(voucher1.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher1));
		voucher1.setVoucherStatus(VoucherStatus.CONSUMED);
		Mockito.when(voucherService.consumeVoucher(voucher1.getVoucherId())).thenReturn(DTOMapper.toVoucherDTO(voucher1));

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/core/vouchers/consume")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voucher1))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))	
				.andExpect(jsonPath("$.message").value("Voucher has been successfully consumed.")).andDo(print());
	}
}
