package sg.edu.nus.iss.voucher.core.workflow.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherRequest;
import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.entity.Voucher;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.CampaignService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.VoucherService;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;

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

	@BeforeAll
	static void setUp() {
		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));
		mockVouchers.add(DTOMapper.toVoucherDTO(voucher1));
	}


	@Test
	void testGetVoucherByVoucherId() throws Exception {
		Mockito.when(voucherService.findByVoucherId(voucher2.getVoucherId()))
				.thenReturn(DTOMapper.toVoucherDTO(voucher2));
		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/vouchers/{id}", voucher2.getVoucherId()).header("X-User-Id", userId).contentType(MediaType.APPLICATION_JSON))
		         .andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))	
				.andExpect(jsonPath("$.message").value("Successfully retrieved the voucher associated with the specified ID: " + voucher2.getVoucherId())).andDo(print());
	}
	
	@Test
	void testClaimVoucher() throws Exception {
		
		Mockito.when(userValidatorService.validateActiveUser(voucher1.getClaimedBy(),  UserRoleType.CUSTOMER.toString())).thenReturn(new HashMap<>());

		Mockito.when(campaignService.findById(campaign.getCampaignId())).thenReturn(Optional.of(campaign));

		Mockito.when(voucherService.claimVoucher(Mockito.any(VoucherRequest.class)))
				.thenReturn(DTOMapper.toVoucherDTO(voucher1));
		
		VoucherRequest voucherRequest = new VoucherRequest();
		voucherRequest.setCampaignId(voucher1.getCampaign().getCampaignId());
		voucherRequest.setClaimedBy(voucher1.getClaimedBy());

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/vouchers/claim").header("X-User-Id", userId).contentType(MediaType.APPLICATION_JSON)
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

		Mockito.when(voucherService.findByClaimedByAndVoucherStatus("U1",VoucherStatus.CLAIMED.toString(), pageable))
				.thenReturn(mockVoucherMap);
		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/vouchers/users/{userId}","U1").header("X-User-Id", "U1").param("status", "CLAIMED").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
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

		Mockito.when(voucherService.findAllClaimedVouchersByCampaignId(campaign.getCampaignId(), pageable))
				.thenReturn(mockVoucherMap);
		mockMvc.perform(
				MockMvcRequestBuilders.get("/api/core/vouchers/campaigns/{campaignId}",campaign.getCampaignId()).header("X-User-Id", userId).param("query", "").param("page", "0").param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
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

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/core/vouchers/{id}/consume", voucher1.getVoucherId()).header("X-User-Id", userId).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(voucher1))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))	
				.andExpect(jsonPath("$.message").value("Voucher has been successfully consumed.")).andDo(print());
	}
}
