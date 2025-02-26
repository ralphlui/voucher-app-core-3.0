package sg.edu.nus.iss.voucher.core.workflow.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.StoreService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;

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
	
	@MockBean
	AuthAPICall apiCall;

	@MockBean
	private StoreService storeService;
	
	@MockBean
	private UserValidatorService userValidatorService;
	

	private static List<StoreDTO> mockStores = new ArrayList<>();

	private static Store store1 = new Store("1", "MUJI",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "423edfbf-ec17-471f-b45a-892a75fa9008", "");
	private static Store store2 = new Store("2", "SK",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "", "");

	@BeforeAll
	static void setUp() {

		mockStores.add(DTOMapper.toStoreDTO(store1));
		mockStores.add(DTOMapper.toStoreDTO(store2));
	}
	
	@Test
	void testGetAllActiveStore() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());
		Map<Long, List<StoreDTO>> mockStoreMap = new HashMap<>();
		mockStoreMap.put(0L, mockStores);

		Mockito.when(storeService.getAllActiveStoreList("", pageable)).thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores").param("query", "").param("page", "0")
				.param("size", "10")
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully retrieved all active stores.")).andDo(print());

		Mockito.when(storeService.getAllActiveStoreList("SK", pageable)).thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores").param("query", "SK").param("page", "0")
				.param("size", "10")
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully retrieved the stores matching the search criteria.")).andDo(print());
		
		
		List<StoreDTO> emptyMockStores = new ArrayList<>();
		Map<Long, List<StoreDTO>> emptyMockStoreMap = new HashMap<>();
		emptyMockStoreMap.put(0L, emptyMockStores);
		Mockito.when(storeService.getAllActiveStoreList("",pageable)).thenReturn(emptyMockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores").param("query", "").param("page", "0").param("size", "10")
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.APPLICATION_JSON))
		        .andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("No Active Store List.")).andDo(print());

	}
	

	@Test
	void testCreateStore() throws Exception {

		MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());

		MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store1));
		
		StoreDTO storeDTO = new StoreDTO();
		
		HashMap<Boolean, String> map = new HashMap<Boolean, String>();
		map.put(true, "User Account is active.");

		
		Mockito.when(userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString())).thenReturn(map);
		
		
		Mockito.when(storeService.findByStoreName(store1.getStoreName())).thenReturn(storeDTO);
		Mockito.when(
				storeService.createStore(Mockito.any(Store.class), (MultipartFile) Mockito.any(MultipartFile.class)))
				.thenReturn(DTOMapper.toStoreDTO(store1));
		
		
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/core/stores").file(store).file(uploadFile)
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.MULTIPART_FORM_DATA))
		        .andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andDo(print());
		
		MockMultipartFile storeFile = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store2));

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/core/stores").file(storeFile).file(uploadFile)
				.header("X-User-Id", store2.getCreatedBy())
				.contentType(MediaType.MULTIPART_FORM_DATA))
		        .andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Bad Request: User id field could not be blank.")).andDo(print());

	}
	
	@Test
	void testGetStoreById() throws Exception {
		Mockito.when(storeService.findByStoreId(store1.getStoreId())).thenReturn(DTOMapper.toStoreDTO(store1));
		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores/{id}", store1.getStoreId())
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(store1))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true)).andDo(print());
	}
	

	@Test
	void testGetAllStoreByUser() throws Exception {

		Pageable pageable = PageRequest.of(0, 10, Sort.by("storeName").ascending());
		Map<Long, List<StoreDTO>> mockStoreMap = new HashMap<>();
		mockStoreMap.put(0L, mockStores);
		
		Mockito.when(userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString())).thenReturn(new HashMap<>());

		Mockito.when(storeService.findActiveStoreListByUserId(store1.getCreatedBy(), false, pageable))
				.thenReturn(mockStoreMap);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/core/stores/users/{userId}", store1.getCreatedBy())
				.param("page", "0").param("size", "10")
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("Successfully retrieved all active stores for the specified user.")).andDo(print());

	}
	
	@Test
	void testUpdateStore() throws Exception {

		store1.setUpdatedBy(store1.getCreatedBy());
		MockMultipartFile uploadFile = new MockMultipartFile("image", "store.jpg", "image/jpg", "store".getBytes());

		MockMultipartFile store = new MockMultipartFile("store", "store", MediaType.APPLICATION_JSON_VALUE,
				objectMapper.writeValueAsBytes(store1));
		
		
		HashMap<Boolean, String> map = new HashMap<Boolean, String>();
		map.put(true, "User Account is active.");

		
		Mockito.when(userValidatorService.validateActiveUser(store1.getCreatedBy(), UserRoleType.MERCHANT.toString())).thenReturn(map);
		
		
		Mockito.when(storeService.findByStoreId(store1.getStoreId())).thenReturn(DTOMapper.toStoreDTO(store1));
	 
		Mockito.when(
				storeService.updateStore(Mockito.any(Store.class), (MultipartFile) Mockito.any(MultipartFile.class)))
				.thenReturn(DTOMapper.toStoreDTO(store1));

		mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT,"/api/core/stores").file(store).file(uploadFile)
				.header("X-User-Id", store1.getCreatedBy())
				.contentType(MediaType.MULTIPART_FORM_DATA))
		         .andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value(store1.getStoreName()+" is updated successfully.")).andDo(print());

	}

}
