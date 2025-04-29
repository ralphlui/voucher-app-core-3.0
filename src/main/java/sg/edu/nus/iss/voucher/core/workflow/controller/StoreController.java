package sg.edu.nus.iss.voucher.core.workflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import sg.edu.nus.iss.voucher.core.workflow.dto.APIResponse;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreRequest;
import sg.edu.nus.iss.voucher.core.workflow.dto.ValidationResult;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.enums.HTTPVerb;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.exception.StoreNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.jwt.JWTService;
import sg.edu.nus.iss.voucher.core.workflow.search.StoreSearchRequest;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.StoreService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.strategy.impl.StoreValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;
import sg.edu.nus.iss.voucher.core.workflow.utility.HtmlSanitizerUtil;

import org.springframework.data.domain.*;

@RestController
@Validated
@RequestMapping("/api/core/stores")
public class StoreController {

	private static final Logger logger = LoggerFactory.getLogger(StoreController.class);
	private static final String INVALID_USER_ID = "Invalid UserID";
	private static final String API_CORE_STORES_ENDPOINT = "api/core/stores";

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreValidationStrategy storeValidationStrategy;

	@Autowired
	private UserValidatorService userValidatorService;

	@Autowired
	private AuditService auditService;

	@Autowired
	private JWTService jwtService;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<StoreDTO>>> getAllActiveStoreList(
			@RequestHeader("Authorization") String authorizationHeader, @RequestParam Map<String, String> allParams,
			@Valid StoreSearchRequest searchRequest,
			 HttpServletRequest request) {
		String activityType = "GetAllActiveStoreList";
		String endpoint = API_CORE_STORES_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";
		String sanitizedQuery="";
		String userId = INVALID_USER_ID;
		try {
			userId = jwtService.retrieveUserID(authorizationHeader);
			

	        if (request.getMethod().equals("GET") && request.getContentLength() > 0) {
	           
	            message = "GET request should not contain a body.";
	            
	            return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod,
						message, HttpStatus.BAD_REQUEST, "", authorizationHeader);
	        }
			
			
			Set<String> allowedParams = Set.of("query", "page", "size");

			for (String param : allParams.keySet()) {
				if (!allowedParams.contains(param)) {
					message = "Unknown parameter: " + param;

					return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod,
							message, HttpStatus.BAD_REQUEST, "", authorizationHeader);

				}
			}
			
			
			String query = searchRequest.getQuery();
			if (query == null) {
				query = "";
			}
			if (!query.equals("")) {
				sanitizedQuery = HtmlSanitizerUtil.sanitize(query);

				if (sanitizedQuery.isEmpty()) {

					message = "Invalid input detected in Query. Potential XSS attack.";

					return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod,
							message, HttpStatus.BAD_REQUEST, "", authorizationHeader);
				}
			}
			

			Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(),
					Sort.by("storeName").ascending());
			Map<Long, List<StoreDTO>> resultMap = storeService.getAllActiveStoreList(sanitizedQuery, pageable);
			logger.info("size" + resultMap.size());

			Map.Entry<Long, List<StoreDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<StoreDTO> storeDTOList = firstEntry.getValue();

			if (storeDTOList.size() > 0) {
				message = GeneralUtility.makeNotNull(searchRequest.getQuery()).isEmpty()
						? "Successfully retrieved all active stores."
						: "Successfully retrieved the stores matching the search criteria.";
				return handleResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord, authorizationHeader);

			} else {
				message = searchRequest.getQuery().isEmpty() ? "No Active Store List."
						: "No stores found matching the search criteria " + searchRequest.getQuery();
				return handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord, authorizationHeader);
			}

		} catch (Exception e) {
			message = "The attempt to retrieve active store list was unsuccessful.";
			return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, e.toString(), authorizationHeader);
		}

	}

	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> createStore(@RequestHeader("Authorization") String authorizationHeader,
			@RequestPart("store") Store store,
			@RequestPart(value = "image", required = false) MultipartFile uploadFile) {
		logger.info("Call store create API...");
		String message = "";
		String activityType = "CreatStore";
		String endpoint = API_CORE_STORES_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.POST;
		String userid = INVALID_USER_ID;

		try {
			userid = jwtService.retrieveUserID(authorizationHeader);
			ValidationResult validationResult = storeValidationStrategy.validateCreation(store, uploadFile,
					authorizationHeader);

			if (validationResult.isValid()) {

				StoreDTO storeDTO = storeService.createStore(store, uploadFile);
				message = storeDTO.getStoreName() + " is created successfully.";
				return handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint, httpMethod, message,
						storeDTO, authorizationHeader);

			} else {
				return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
						validationResult.getMessage(), validationResult.getStatus(), "", authorizationHeader);
			}

		} catch (Exception ex) {
			message = "The attempt to create store was unsuccessful.";
			return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
		}

	}

	@PostMapping(value = "/my-store", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> getStoreById(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody StoreRequest storeRequest) {
		logger.info("Call store getStoreById API...");

		String message = "";
		String activityType = "GetStoreById";
		String endpoint = String.format(API_CORE_STORES_ENDPOINT);
		HTTPVerb httpMethod = HTTPVerb.POST;
		String userId = INVALID_USER_ID;

		try {
			userId = jwtService.retrieveUserID(authorizationHeader);
			String storeId = GeneralUtility.makeNotNull(storeRequest.getStoreId()).trim();

			if (storeId.isEmpty()) {
				message = "Bad Request: Store Id could not be blank.";
				return handleResponseAndSendAudtiLogForFailureCase(userId, activityType, endpoint, httpMethod, message,
						HttpStatus.BAD_REQUEST, "", authorizationHeader);
			}

			StoreDTO storeDTO = storeService.findByStoreId(storeId);
			message = storeDTO.getStoreName() + " is found.";
			return handleResponseAndSendAudtiLogForSuccessCase(userId, activityType, endpoint, httpMethod, message,
					storeDTO, authorizationHeader);

		}

		catch (Exception e) {
			message = "The attempt to retrieve the store by the provided store ID was unsuccessful.";
			HttpStatusCode htpStatuscode = e instanceof StoreNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return handleResponseAndSendAudtiLogForFailureCase(userId, activityType, endpoint, httpMethod, message,
					htpStatuscode, e.toString(), authorizationHeader);
		}

	}

	@PostMapping(value = "/users", produces = "application/json")
	public ResponseEntity<APIResponse<List<StoreDTO>>> getAllStoreByUser(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody StoreRequest storeRequest,
			@Valid StoreSearchRequest searchRequest) {

		String message = "";
		String activityType = "GetAllStoreListByUserId";
		String endpoint = String.format("api/core/stores/users");
		HTTPVerb httpMethod = HTTPVerb.POST;
		String authorizationUserID = INVALID_USER_ID;

		try {

			authorizationUserID = jwtService.retrieveUserID(authorizationHeader);
			String userId = storeRequest.getCreatedBy();
			if (userId.isEmpty()) {
				message = "User id cannot be blank.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

			HashMap<Boolean, String> userMap = userValidatorService.validateActiveUser(userId,
					UserRoleType.MERCHANT.toString(), authorizationHeader);

			for (Map.Entry<Boolean, String> entry : userMap.entrySet()) {

				if (!entry.getKey()) {
					message = entry.getValue();
					return handleResponseListAndSendAuditLogForFailuresCase(authorizationUserID, activityType, endpoint,
							httpMethod, message, HttpStatus.BAD_REQUEST, "", authorizationHeader);
				}
			}

			Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(),
					Sort.by("storeName").ascending());
			Map<Long, List<StoreDTO>> resultMap = storeService.findActiveStoreListByUserId(userId, false, pageable);
			logger.info("size " + resultMap.size());

			Map.Entry<Long, List<StoreDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<StoreDTO> storeDTOList = firstEntry.getValue();

			if (storeDTOList.size() > 0) {
				message = "Successfully retrieved all active stores for the specified user.";
				return handleResponseListAndSendAuditLogForSuccessCase(authorizationUserID, activityType, endpoint,
						httpMethod, message, storeDTOList, totalRecord, authorizationHeader);
			} else {
				message = "No Active Store List.";
				return handleEmptyResponseListAndSendAuditLogForSuccessCase(authorizationUserID, activityType, endpoint,
						httpMethod, message, storeDTOList, totalRecord, authorizationHeader);
			}

		} catch (Exception e) {
			message = "The attempt to retrieve the list of all active stores for the specified user was unsuccessful.";
			return handleResponseListAndSendAuditLogForFailuresCase(authorizationUserID, activityType, endpoint,
					httpMethod, message, HttpStatus.INTERNAL_SERVER_ERROR, e.toString(), authorizationHeader);
		}
	}

	@PutMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> updateStore(@RequestHeader("Authorization") String authorizationHeader,
			@RequestPart("store") Store store,
			@RequestPart(value = "image", required = false) MultipartFile uploadFile) {

		logger.info("Call store updat API...");
		String message = "";
		String activityType = "UpdateStore";
		String endpoint = API_CORE_STORES_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.PUT;
		String userid = INVALID_USER_ID;

		try {
			userid = jwtService.retrieveUserID(authorizationHeader);
			ValidationResult validationResult = storeValidationStrategy.validateUpdating(store, uploadFile,
					authorizationHeader);
			if (validationResult == null || !validationResult.isValid()) {
				message = validationResult != null ? validationResult.getMessage()
						: "Validation failed: result is null";
				HttpStatus status = validationResult != null ? validationResult.getStatus()
						: HttpStatus.INTERNAL_SERVER_ERROR;
				return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
						status, "", authorizationHeader);
			}

			StoreDTO storeDTO = storeService.updateStore(store, uploadFile);
			message = storeDTO.getStoreName() + " is updated successfully.";
			return handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint, httpMethod, message,
					storeDTO, authorizationHeader);
		} catch (Exception e) {
			message = "The attempt to update store was unsuccessful.";
			logger.error(message);
			return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, e.toString(), authorizationHeader);
		}

	}

	private ResponseEntity<APIResponse<StoreDTO>> handleResponseAndSendAudtiLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, StoreDTO storeDTO,
			String authorizationHeader) {
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, HttpStatus.OK.value(), message, authorizationHeader);
		return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(storeDTO, message));
	}

	private ResponseEntity<APIResponse<StoreDTO>> handleResponseAndSendAudtiLogForFailureCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode,
			String remark, String authorizationHeader) {
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, htpStatuscode.value(), message, authorizationHeader);
		return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleResponseListAndSendAuditLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, List<StoreDTO> storeDTOList,
			long totalRecord, String authorizationHeader) {
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.success(storeDTOList, message, totalRecord));

	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleEmptyResponseListAndSendAuditLogForSuccessCase(
			String userId, String activityType, String endpoint, HTTPVerb httpVerb, String message,
			List<StoreDTO> storeDTOList, long totalRecord, String authorizationHeader) {
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.noList(storeDTOList, message));

	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleResponseListAndSendAuditLogForFailuresCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode,
			String remark, String authorizationHeader) {
		logger.info(message);
		int httpStatusCode = htpStatuscode.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.error(message));

	}

}
