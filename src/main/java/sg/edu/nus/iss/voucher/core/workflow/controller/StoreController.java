package sg.edu.nus.iss.voucher.core.workflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import sg.edu.nus.iss.voucher.core.workflow.dto.APIResponse;
import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.ValidationResult;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.enums.HTTPVerb;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.exception.StoreNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.AuditService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.StoreService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.strategy.impl.StoreValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;

import org.springframework.data.domain.*;

@RestController
@Validated
@RequestMapping("/api/core/stores")
public class StoreController {

	private static final Logger logger = LoggerFactory.getLogger(StoreController.class);

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreValidationStrategy storeValidationStrategy;

	@Autowired
	private UserValidatorService userValidatorService;

	@Autowired
	private AuditService auditService;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;
	
	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<StoreDTO>>> getAllActiveStoreList(@RequestHeader("X-User-Id") String userId,
			@RequestParam(defaultValue = "") String query, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {
		logger.info("Call store getAll API with page={}, size={}", page, size);
		String activityType = "GetAllActiveStoreList";
		String endpoint = "/api/core/stores";
		HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";

		try {

			Pageable pageable = PageRequest.of(page, size, Sort.by("storeName").ascending());
			Map<Long, List<StoreDTO>> resultMap = storeService.getAllActiveStoreList(query, pageable);
			logger.info("size" + resultMap.size());

			Map.Entry<Long, List<StoreDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<StoreDTO> storeDTOList = firstEntry.getValue();

			if (storeDTOList.size() > 0) {
				message = query.isEmpty() ? "Successfully retrieved all active stores."
						: "Successfully retrieved the stores matching the search criteria.";
				return handleResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord);

			} else {
				message = query.isEmpty() ? "No Active Store List." : "No stores found matching the search criteria " + query;
				;
				return handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord);
			}

		} catch (Exception e) {
			message = "The attempt to retrieve active store list was unsuccessful.";
			return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod,
					message, HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
		}

	}

	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> createStore(@RequestHeader("X-User-Id") String userId,
			@RequestPart("store") Store store,
			@RequestPart(value = "image", required = false) MultipartFile uploadFile) {
		logger.info("Call store create API...");
		String message = "";
		String activityType = "CreatStore";
		String endpoint = "api/core/stores";
		HTTPVerb httpMethod = HTTPVerb.POST;
		String userid = store.getCreatedBy().isEmpty() ? userId : store.getCreatedBy();

		try {
			ValidationResult validationResult = storeValidationStrategy.validateCreation(store, uploadFile);

			if (validationResult.isValid()) {

				StoreDTO storeDTO = storeService.createStore(store, uploadFile);
				message = storeDTO.getStoreName() + " is created successfully.";
				return handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint, httpMethod, message,
						storeDTO);

			} else {
				return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
						validationResult.getMessage(), validationResult.getStatus(), "");
			}

		} catch (Exception ex) {
			message = "The attempt to create store was unsuccessful.";
			return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, ex.toString());
		}

	}

	@GetMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> getStoreById(@RequestHeader("X-User-Id") String userId,
			@PathVariable("id") String id) {
		logger.info("Call store getStoreById API...");

		String message = "";
		String activityType = "GetStoreById";
		String endpoint = String.format("api/core/stores/%s", id);
		HTTPVerb httpMethod = HTTPVerb.GET;

		try {
			String storeId = GeneralUtility.makeNotNull(id).trim();
			logger.info("storeId: " + storeId);

			if (storeId.isEmpty()) {
				message = "Bad Request: Store Id could not be blank.";
				return handleResponseAndSendAudtiLogForFailureCase(userId, activityType, endpoint, httpMethod, message,
						HttpStatus.BAD_REQUEST, "");
			}

			StoreDTO storeDTO = storeService.findByStoreId(storeId);
			message = storeDTO.getStoreName() + " is found.";
			return handleResponseAndSendAudtiLogForSuccessCase(userId, activityType, endpoint, httpMethod, message,
					storeDTO);

		}

		catch (Exception e) {
			message = "The attempt to retrieve the store by the provided store ID was unsuccessful.";
			HttpStatusCode htpStatuscode = e instanceof StoreNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return handleResponseAndSendAudtiLogForFailureCase(userId, activityType, endpoint, httpMethod, message,
					htpStatuscode, e.toString());
		}

	}

	@GetMapping(value = "/users/{userId}", produces = "application/json")
	public ResponseEntity<APIResponse<List<StoreDTO>>> getAllStoreByUser(@RequestHeader("X-User-Id") String xUserId,
			@PathVariable("userId") String userId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "500") int size) {

		logger.info("Call store getAllByUser API with page={}, size={}", page, size);
		String message = "";
		String activityType = "GetAllStoreListByUserId";
		String endpoint = String.format("api/core/stores/users/%s", userId);
		HTTPVerb httpMethod = HTTPVerb.GET;
		String userid = !userId.isEmpty() ? userId : xUserId;

		try {

			if (userid.isEmpty()) {
				message = "User id cannot be blank.";
				logger.error(message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

			logger.info("UserId: " + userid);
			HashMap<Boolean, String> userMap = userValidatorService.validateActiveUser(userid,
					UserRoleType.MERCHANT.toString());
			logger.info("user Id key map " + userMap.keySet());

			for (Map.Entry<Boolean, String> entry : userMap.entrySet()) {
				logger.info("user role: " + entry.getValue());
				logger.info("user id: " + entry.getKey());

				if (!entry.getKey()) {
					message = entry.getValue();
					return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod,
							message, HttpStatus.BAD_REQUEST, "");
				}
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by("storeName").ascending());
			Map<Long, List<StoreDTO>> resultMap = storeService.findActiveStoreListByUserId(userid, false, pageable);
			logger.info("size " + resultMap.size());

			Map.Entry<Long, List<StoreDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<StoreDTO> storeDTOList = firstEntry.getValue();

			if (storeDTOList.size() > 0) {
				message = "Successfully retrieved all active stores for the specified user.";
				return handleResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord);
			} else {
				message = "No Active Store List.";
				return handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType, endpoint, httpMethod,
						message, storeDTOList, totalRecord);
			}

		} catch (Exception e) {
			message = "The attempt to retrieve the list of all active stores for the specified user was unsuccessful.";
			return handleResponseListAndSendAuditLogForFailuresCase(userId, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
		}
	}

	@PutMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<StoreDTO>> updateStore(@RequestHeader("X-User-Id") String userId,
			@RequestPart("store") Store store,
			@RequestPart(value = "image", required = false) MultipartFile uploadFile) {

		logger.info("Call store updat API...");
		String message = "";
		String activityType = "UpdateStore";
		String endpoint = "api/core/stores";
		HTTPVerb httpMethod = HTTPVerb.PUT;
		String userid = store.getUpdatedBy().isEmpty() ? userId : store.getUpdatedBy();

		try {

			ValidationResult validationResult = storeValidationStrategy.validateUpdating(store, uploadFile);
			if (!validationResult.isValid()) {
				message = validationResult.getMessage();
				return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
						validationResult.getStatus(), "");
			}

			StoreDTO storeDTO = storeService.updateStore(store, uploadFile);
			message = storeDTO.getStoreName() + " is updated successfully.";
			return handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint, httpMethod, message,
					storeDTO);
		} catch (Exception e) {
			message = "The attempt to update store was unsuccessful.";
				logger.error(message);
			return handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
					HttpStatus.INTERNAL_SERVER_ERROR, e.toString());
		}

	}

	private ResponseEntity<APIResponse<StoreDTO>> handleResponseAndSendAudtiLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, StoreDTO storeDTO) {
		logger.info(message);
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, HttpStatus.OK.value(), message);
		return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(storeDTO, message));
	}

	private ResponseEntity<APIResponse<StoreDTO>> handleResponseAndSendAudtiLogForFailureCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode, String remark) {
		logger.error(message);
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, htpStatuscode.value(), message);
		return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleResponseListAndSendAuditLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, List<StoreDTO> storeDTOList,
			long totalRecord) {
		logger.info(message);
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.success(storeDTOList, message, totalRecord));

	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleEmptyResponseListAndSendAuditLogForSuccessCase(
			String userId, String activityType, String endpoint, HTTPVerb httpVerb, String message,
			List<StoreDTO> storeDTOList, long totalRecord) {
		logger.info(message);
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.noList(storeDTOList, message));

	}

	private ResponseEntity<APIResponse<List<StoreDTO>>> handleResponseListAndSendAuditLogForFailuresCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode, String remark) {
		logger.info(message);
		int httpStatusCode = htpStatuscode.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, httpStatusCode, message);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.error(message));

	}

}
