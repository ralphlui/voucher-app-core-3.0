package sg.edu.nus.iss.voucher.core.workflow.controller;

import static org.hamcrest.CoreMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import sg.edu.nus.iss.voucher.core.workflow.dto.*;
import sg.edu.nus.iss.voucher.core.workflow.entity.*;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.pojo.User;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.*;
import sg.edu.nus.iss.voucher.core.workflow.strategy.impl.CampaignValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;
import sg.edu.nus.iss.voucher.core.workflow.utility.JSONReader;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class CampaignControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CampaignService campaignService;

	@MockBean
	private StoreService storeService;

	@MockBean
	private AuthAPICall authAPICall;

	@InjectMocks
	private CampaignController campaignController;

	@Mock
	private CampaignValidationStrategy campaignValidationStrategy;

	@MockBean
	private JWTService jwtService;

	@MockBean
	private JSONReader jsonReader;
	
	@MockBean
    private AuditService auditService;
	
	private static List<CampaignDTO> mockCampaigns = new ArrayList<>();

	static String userId = "user123";
	static String authorizationHeader = "Bearer mock.jwt.token";

	private static Store store = new Store("1", "MUJI",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "", "");

	private static Campaign campaign1 = new Campaign("1", "new campaign 1", store, CampaignStatus.CREATED, null, 10, 0,
			null, null, 10, LocalDateTime.now(), LocalDateTime.now(), userId, "", LocalDateTime.now(),
			LocalDateTime.now(), null, "Clothes", false);
	private static Campaign campaign2 = new Campaign("2", "new campaign 2", store, CampaignStatus.CREATED, null, 10, 0,
			null, null, 10, LocalDateTime.now(), LocalDateTime.now(), userId, "", LocalDateTime.now(),
			LocalDateTime.now(), null, "Clothes", false);
	
	private static CampaignRequest  messagePayload = new CampaignRequest( "1", "1", userId);

	@BeforeEach
	void setUp() throws Exception {
		mockCampaigns.add(DTOMapper.toCampaignDTO(campaign1));
		mockCampaigns.add(DTOMapper.toCampaignDTO(campaign2));

		User mockUser = new User();
		mockUser.setEmail("eleven.11@gmail.com");
		mockUser.setPassword("111111");
		mockUser.setUserId("12345");
		//when(jsonReader.getActiveUserDetails("12345", "mock.jwt.token")).thenReturn(mockUser);

		when(jwtService.extractUserID("mock.jwt.token")).thenReturn(userId);

		UserDetails mockUserDetails = mock(UserDetails.class);
		when(jwtService.getUserDetail(anyString(), anyString())).thenReturn(mockUserDetails);

		when(jwtService.validateToken(anyString(), eq(mockUserDetails))).thenReturn(true);

		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn(userId);
		
		ArgumentCaptor<AuditDTO> auditDTOCaptor = ArgumentCaptor.forClass(AuditDTO.class);
		   
	    doNothing().when(auditService).logAudit(auditDTOCaptor.capture(), eq(200), eq("message"), eq("authorizationHeader"));


	}
	

	@Test
	void testCreateCampaign() throws Exception {

	    String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
	            + userId
	            + "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";

	    Mockito.when(authAPICall.validateActiveUser(userId, authorizationHeader)).thenReturn(mockResponse);

	    JSONParser parser = new JSONParser();
	    JSONObject mockJsonResponse = (JSONObject) parser.parse(mockResponse);
	    when(jsonReader.parseJsonResponse(mockResponse)).thenReturn(mockJsonResponse);

	    JSONObject mockData = new JSONObject();
	    mockData.put("userID", "user123");
	    mockData.put("role", "MERCHANT");
	    Boolean mockSuccess = true;
	    String mockMessage = "eleven.11@gmail.com is Active";

	    when(jsonReader.getDataFromResponse(mockJsonResponse)).thenReturn(mockData);
	    when(jsonReader.getSuccessFromResponse(mockJsonResponse)).thenReturn(mockSuccess);
	    when(jsonReader.getMessageFromResponse(mockJsonResponse)).thenReturn(mockMessage);

	    Mockito.when(storeService.findByStoreId(store.getStoreId())).thenReturn(DTOMapper.toStoreDTO(store));

	    Mockito.when(campaignService.create(Mockito.any(Campaign.class)))
	            .thenReturn(DTOMapper.toCampaignDTO(campaign1));

	  	

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/campaigns")
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(campaign1)))
	            .andExpect(MockMvcResultMatchers.status().isOk())
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(true))
	            .andDo(print());

	}

	@Test
	void testUpdateCampaign() throws Exception {
		Mockito.when(campaignService.findById(campaign1.getCampaignId())).thenReturn(Optional.of(campaign1));

		campaign1.setDescription("new desc");
		campaign1.setUpdatedBy(userId);

		String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
				+ userId
				+ "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";
		
		Mockito.when(authAPICall.validateActiveUser(userId, authorizationHeader)).thenReturn(mockResponse);
		
		JSONParser parser = new JSONParser();
        JSONObject mockJsonResponse = (JSONObject) parser.parse(mockResponse);
        when(jsonReader.parseJsonResponse(mockResponse)).thenReturn(mockJsonResponse);

		JSONObject mockData = new JSONObject();
        mockData.put("userID", "user123");
        mockData.put("role","MERCHANT");
        Boolean mockSuccess = true;
        String mockMessage = "eleven.11@gmail.com is Active";
      
        when(jsonReader.getDataFromResponse(mockJsonResponse)).thenReturn(mockData);
        when(jsonReader.getSuccessFromResponse(mockJsonResponse)).thenReturn(mockSuccess);
        when(jsonReader.getMessageFromResponse(mockJsonResponse)).thenReturn(mockMessage);
		
		Mockito.when(campaignService.update(Mockito.any(Campaign.class)))
				.thenReturn(DTOMapper.toCampaignDTO(campaign1));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/campaigns/update")
				.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(campaign1))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());
	}

	@Test
	void testPromoteCampaign() throws Exception {

		campaign1.setStartDate(LocalDateTime.now().plusDays(10));
		campaign1.setEndDate(LocalDateTime.now().plusDays(20));
		campaign1.setCampaignStatus(CampaignStatus.CREATED);
		campaign1.setUpdatedBy(userId);
		String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
				+ userId
				+ "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";

		Mockito.when(authAPICall.validateActiveUser(userId, authorizationHeader)).thenReturn(mockResponse);
		
		JSONParser parser = new JSONParser();
        JSONObject mockJsonResponse = (JSONObject) parser.parse(mockResponse);
        when(jsonReader.parseJsonResponse(mockResponse)).thenReturn(mockJsonResponse);

		JSONObject mockData = new JSONObject();
        mockData.put("userID", "user123");
        mockData.put("role","MERCHANT");
        Boolean mockSuccess = true;
        String mockMessage = "eleven.11@gmail.com is Active";
      
        when(jsonReader.getDataFromResponse(mockJsonResponse)).thenReturn(mockData);
        when(jsonReader.getSuccessFromResponse(mockJsonResponse)).thenReturn(mockSuccess);
        when(jsonReader.getMessageFromResponse(mockJsonResponse)).thenReturn(mockMessage);
		
		Mockito.when(campaignService.findById(campaign1.getCampaignId())).thenReturn(Optional.of(campaign1));
		Mockito.when(campaignService.promote(campaign1.getCampaignId(), userId,authorizationHeader))
				.thenReturn(DTOMapper.toCampaignDTO(campaign1));

		mockMvc.perform(MockMvcRequestBuilders
				.post("/api/core/campaigns/promote")
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(messagePayload)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true)).andDo(print());
	}

	
	@Test
	void testGetAllActiveCampaigns() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").ascending());
		Map<Long, List<CampaignDTO>> mockCampaignMap = new HashMap<>();
		mockCampaignMap.put(0L, mockCampaigns);

		Mockito.when(campaignService.findAllActiveCampaigns("", pageable)).thenReturn(mockCampaignMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/campaigns").header("Authorization", authorizationHeader)
				.param("page", "0").param("size", "10").contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].campaignId").value(1))
				.andDo(print());
	}
	

	@Test
	void testGetAllActiveCampaigns_whenNoCampaignsFound() throws Exception {

		Map<Long, List<CampaignDTO>> mockCampaignMap = new HashMap<>();

		Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").ascending());

		Mockito.when(campaignService.findAllActiveCampaigns(campaign1.getDescription(), pageable))
				.thenReturn(mockCampaignMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/campaigns").header("Authorization", authorizationHeader)
				.param("page", "0").param("size", "10").contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("There are no available campaign list.")).andDo(print());
	}

	@Test
	void getAllCampaignsByStoreId() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate").ascending());
		Map<Long, List<CampaignDTO>> mockCampaignMap = new HashMap<>();
		mockCampaignMap.put(0L, mockCampaigns);

		Mockito.when(campaignService.findAllCampaignsByStoreId(campaign1.getStore().getStoreId(), "", pageable))
				.thenReturn(mockCampaignMap);
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/campaigns/stores")
				.header("Authorization", authorizationHeader).param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(messagePayload)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data[0].campaignId").value(1))
				.andDo(print());
	}

	@Test
	void testGetCampaignsByUserId() throws Exception {

		int page = 0;
		int size = 10;

		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());
		Map<Long, List<CampaignDTO>> mockCampaignMap = new HashMap<>();
		mockCampaignMap.put(0L, mockCampaigns);

		Mockito.when(campaignService.findAllCampaignsByUserId(userId, "", pageable)).thenReturn(mockCampaignMap);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/campaigns/users")
				.header("Authorization", authorizationHeader).param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(messagePayload)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data[0].campaignId").value(1L)).andDo(print());
	}

	@Test
	void testGetByCampaignId() throws Exception {

		Mockito.when(campaignService.findByCampaignId(campaign1.getCampaignId()))
				.thenReturn(DTOMapper.toCampaignDTO(campaign1));
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/campaigns/Id")
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(messagePayload)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());
	}

}
