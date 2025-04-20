package sg.edu.nus.iss.voucher.core.workflow.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreRequest;
import sg.edu.nus.iss.voucher.core.workflow.dto.ValidationResult;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.StoreService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.strategy.impl.StoreValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;
import sg.edu.nus.iss.voucher.core.workflow.utility.JSONReader;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class StoreControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	AuthAPICall apiCall;

	@InjectMocks
	private StoreController storeController;

	@Mock
	private StoreValidationStrategy storeValidationStrategy;

	@MockitoBean
	private StoreService storeService;

	@MockitoBean
	private UserValidatorService userValidatorService;

	@MockitoBean
	private JWTService jwtService;

	@MockitoBean
	private JSONReader jsonReader;

	private static List<StoreDTO> mockStores = new ArrayList<>();

	static String authorizationHeader = "Bearer mock.jwt.token";
	static String userId = "user123";

	private static Store store1;
	private static Store store2;

	@BeforeEach
	void setUp() throws Exception {
		store1 = new Store("1", "MUJI",
				"MUJI offers a wide variety of good quality items from stationery to household items and apparel.",
				"Test", "#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore",
				"Singapore", null, null, null, null, false, "423edfbf-ec17-471f-b45a-892a75fa9008", "");
		store2 = new Store("2", "SK",
				"MUJI offers a wide variety of good quality items from stationery to household items and apparel.",
				"Test", "#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore",
				"Singapore", null, null, null, null, false, "", "");

		mockStores.add(DTOMapper.toStoreDTO(store1));
		mockStores.add(DTOMapper.toStoreDTO(store2));

		JSONObject jsonObjet = new JSONObject();
		when(jsonReader.getActiveUser("12345", "mock.jwt.token")).thenReturn(jsonObjet);

		when(jwtService.extractUserID("mock.jwt.token")).thenReturn(userId);

		UserDetails mockUserDetails = mock(UserDetails.class);
		when(jwtService.getUserDetail(anyString(), anyString())).thenReturn(mockUserDetails);

		when(jwtService.validateToken(anyString(), eq(mockUserDetails))).thenReturn(true);

		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn(userId);

	}

	@Test
	void testGetAllActiveStore() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());
		Map<Long, List<StoreDTO>> mockStoreMap = new HashMap<>();
		mockStoreMap.put(0L, mockStores);

		Mockito.when(storeService.getAllActiveStoreList("", pageable)).thenReturn(mockStoreMap);

		mockMvc.perform(
				MockMvcRequestBuilders.get("/api/core/stores").param("query", "").param("page", "0").param("size", "10")
						.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully retrieved all active stores.")).andDo(print());

		Mockito.when(storeService.getAllActiveStoreList("SK", pageable)).thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores").param("query", "SK").param("page", "0")
				.param("size", "10").header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(
						jsonPath("$.message").value("Successfully retrieved the stores matching the search criteria."))
				.andDo(print());

		List<StoreDTO> emptyMockStores = new ArrayList<>();
		Map<Long, List<StoreDTO>> emptyMockStoreMap = new HashMap<>();
		emptyMockStoreMap.put(0L, emptyMockStores);
		Mockito.when(storeService.getAllActiveStoreList("", pageable)).thenReturn(emptyMockStoreMap);

		mockMvc.perform(
				MockMvcRequestBuilders.get("/api/core/stores").param("query", "").param("page", "0").param("size", "10")
						.header("Authorization", authorizationHeader).contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("No Active Store List.")).andDo(print());

	}
	
	@Test
	void testGetAllActiveStore_ShouldReturnInternalServerError_WhenExceptionOccurs() throws Exception {
	    Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());

	    Mockito.when(storeService.getAllActiveStoreList("", pageable))
	           .thenThrow(new RuntimeException("Simulated failure"));

	    mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores")
	            .param("query", "")
	            .param("page", "0")
	            .param("size", "10")
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.APPLICATION_JSON))
	    		.andExpect(MockMvcResultMatchers.status().isInternalServerError())
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("The attempt to retrieve active store list was unsuccessful."))
	            .andDo(print());
	}


	@Test
	void testCreateStore() throws Exception {

		MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());

		MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store1));

		StoreDTO storeDTO = new StoreDTO();

		HashMap<Boolean, String> map = new HashMap<Boolean, String>();
		map.put(true, "User Account is active.");

		Mockito.when(userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString(),
				authorizationHeader)).thenReturn(map);
		String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
				+ userId
				+ "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";

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

		Mockito.when(storeService.findByStoreName(store1.getStoreName())).thenReturn(storeDTO);
		Mockito.when(
				storeService.createStore(Mockito.any(Store.class), (MultipartFile) Mockito.any(MultipartFile.class)))
				.thenReturn(DTOMapper.toStoreDTO(store1));

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/core/stores").file(store).file(uploadFile)
				.header("Authorization", authorizationHeader).contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());

		MockMultipartFile storeFile = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store2));

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/core/stores").file(storeFile).file(uploadFile)
				.header("Authorization", authorizationHeader).contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Bad Request: User id field could not be blank."))
				.andDo(print());

	}
	
	@Test
	void testCreateStore_ReturnInternalServerError_WhenExceptionOccurs() throws Exception {
	    MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());
	    MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
	            objectMapper.writeValueAsBytes(store1));

	    HashMap<Boolean, String> map = new HashMap<>();
	    map.put(true, "User Account is active.");
	    Mockito.when(userValidatorService.validateActiveUser(
	            store1.getCreatedBy(), UserRoleType.MERCHANT.toString(), authorizationHeader)).thenReturn(map);

	    String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
	            + userId
	            + "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";

	    JSONObject mockJsonResponse = (JSONObject) new JSONParser().parse(mockResponse);
	    JSONObject mockData = new JSONObject();
	    mockData.put("userID", "user123");
	    mockData.put("role", "MERCHANT");

	    Mockito.when(jsonReader.parseJsonResponse(mockResponse)).thenReturn(mockJsonResponse);
	    Mockito.when(jsonReader.getDataFromResponse(mockJsonResponse)).thenReturn(mockData);
	    Mockito.when(jsonReader.getSuccessFromResponse(mockJsonResponse)).thenReturn(true);
	    Mockito.when(jsonReader.getMessageFromResponse(mockJsonResponse)).thenReturn("eleven.11@gmail.com is Active");

	    Mockito.when(storeService.findByStoreName(store1.getStoreName())).thenReturn(null);
	    Mockito.when(storeService.createStore(Mockito.any(Store.class), Mockito.any(MultipartFile.class)))
	           .thenThrow(new RuntimeException("Simulated DB failure"));

	    mockMvc.perform(MockMvcRequestBuilders.multipart("/api/core/stores")
	            .file(store)
	            .file(uploadFile)
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.MULTIPART_FORM_DATA))
	            .andExpect(MockMvcResultMatchers.status().isInternalServerError())
	            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("The attempt to create store was unsuccessful."))
	            .andDo(print());
	}

	@Test
	void testGetStoreById() throws Exception {
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setStoreId(store1.getStoreId());

		Mockito.when(storeService.findByStoreId(store1.getStoreId())).thenReturn(DTOMapper.toStoreDTO(store1));
		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/my-store").contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", authorizationHeader).content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());
	}

	@Test
	void testGetAllStoreByUser() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());
		Map<Long, List<StoreDTO>> mockStoreMap = new HashMap<>();
		mockStoreMap.put(0L, mockStores);
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setCreatedBy(store1.getCreatedBy());

		Mockito.when(
				userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString(), ""))
				.thenReturn(new HashMap<>());

		Mockito.when(storeService.findActiveStoreListByUserId(store1.getCreatedBy(), false, pageable))
				.thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/users").param("page", "0").param("size", "10")
				.contentType(MediaType.APPLICATION_JSON).header("Authorization", authorizationHeader)
				.content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(
						jsonPath("$.message").value("Successfully retrieved all active stores for the specified user."))
				.andDo(print());

	}
	
	@Test
	void testGetAllStoreByUser_BlankUserId() throws Exception {
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setCreatedBy(""); 

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/users")
				.param("page", "0")
				.param("size", "10")
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("User id cannot be blank."));
	}
	
	@Test
	void testGetAllStoreByUser_NoStoresFound() throws Exception {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setCreatedBy("user123");

		List<StoreDTO> emptyList = new ArrayList<>();
		Map<Long, List<StoreDTO>> mockStoreMap = new HashMap<>();
		mockStoreMap.put(0L, emptyList);


		Mockito.when(storeService.findActiveStoreListByUserId("user123", false, pageable))
				.thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/users")
				.param("page", "0")
				.param("size", "10")
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("No Active Store List."));
	}


	@Test
	void testGetAllStoreByUser_ExceptionThrown() throws Exception {
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setCreatedBy("user123");

		Mockito.when(storeService.findActiveStoreListByUserId(Mockito.anyString(), Mockito.anyBoolean(), Mockito.any()))
				.thenThrow(new RuntimeException("Database down"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/users")
				.param("page", "0")
				.param("size", "10")
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(storeRequest)))
			    .andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("The attempt to retrieve the list of all active stores for the specified user was unsuccessful."));
	}
	
	@Test
	void testGetAllStoreByUser_InactiveUser() throws Exception {
	    StoreRequest storeRequest = new StoreRequest();
	    storeRequest.setCreatedBy("user123");

	    HashMap<Boolean, String> userMap = new HashMap<>();
	    userMap.put(false, "User is not active.");
	    Mockito.when(userValidatorService.validateActiveUser("user123", UserRoleType.MERCHANT.toString(), authorizationHeader))
	           .thenReturn(userMap);
	    Mockito.when(jwtService.retrieveUserID(authorizationHeader)).thenReturn("authUser123");

	    mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/users")
	            .param("page", "0")
	            .param("size", "10")
	            .header("Authorization", authorizationHeader)
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(storeRequest)))
	            .andExpect(MockMvcResultMatchers.status().isBadRequest())
	            .andExpect(jsonPath("$.success").value(false))
	            .andExpect(jsonPath("$.message").value("User is not active."));
	}


	
	@Test
	void testGetStoreById_ReturnBadRequest_WhenStoreIdIsEmpty() throws Exception {
		StoreRequest storeRequest = new StoreRequest();

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/my-store")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", authorizationHeader)
				.content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Bad Request: Store Id could not be blank."))
				.andDo(print());
	}

	@Test
	void testGetStoreById_ReturnInternalServerError_WhenUnexpectedExceptionThrown() throws Exception {
		StoreRequest storeRequest = new StoreRequest();
		storeRequest.setStoreId("store123");

		Mockito.when(storeService.findByStoreId("store123"))
		       .thenThrow(new RuntimeException("Unexpected error"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/core/stores/my-store")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", authorizationHeader)
				.content(objectMapper.writeValueAsString(storeRequest)))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("The attempt to retrieve the store by the provided store ID was unsuccessful."))
				.andDo(print());
	}


	@Test
	void testUpdateStore() throws Exception {

		store1.setUpdatedBy(store1.getCreatedBy());
		MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());

		MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store1));

		HashMap<Boolean, String> map = new HashMap<Boolean, String>();
		map.put(true, "User Account is active.");

		Mockito.when(userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString(),
				authorizationHeader)).thenReturn(map);

		String mockResponse = "{\"success\":true,\"message\":\"eleven.11@gmail.com is Active\",\"totalRecord\":1,\"data\":{\"userID\":\""
				+ userId
				+ "\",\"email\":\"eleven.11@gmail.com\",\"username\":\"Eleven11\",\"role\":\"MERCHANT\",\"active\":true,\"verified\":true}}";

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

		Mockito.when(storeService.findByStoreId(store1.getStoreId())).thenReturn(DTOMapper.toStoreDTO(store1));

		Mockito.when(
				storeService.updateStore(Mockito.any(Store.class), (MultipartFile) Mockito.any(MultipartFile.class)))
				.thenReturn(DTOMapper.toStoreDTO(store1));

		mockMvc.perform(
				MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/core/stores").file(store).file(uploadFile)
						.header("Authorization", authorizationHeader).contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value(store1.getStoreName() + " is updated successfully."))
				.andDo(print());

	}
	
	@Test
	void testUpdateStoreBadRequest_ValidationFails() throws Exception {
		MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());

		MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store1));

		ValidationResult invalidResult = new ValidationResult();
		invalidResult.setStatus(HttpStatus.BAD_REQUEST);
		invalidResult.setMessage( "Invalid store data");
		invalidResult.setValid(false);
		when(storeValidationStrategy.validateUpdating(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(invalidResult);

		mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/core/stores")
				.file(store).file(uploadFile)
				.header("Authorization", authorizationHeader)
				.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Invalid store Id: "+store1.getStoreId()))
				.andDo(print());
	}
	
	

}
