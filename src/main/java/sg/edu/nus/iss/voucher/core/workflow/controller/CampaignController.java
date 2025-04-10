package sg.edu.nus.iss.voucher.core.workflow.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.iss.voucher.core.workflow.dto.*;
import sg.edu.nus.iss.voucher.core.workflow.entity.*;
import sg.edu.nus.iss.voucher.core.workflow.enums.*;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.*;
import sg.edu.nus.iss.voucher.core.workflow.strategy.impl.CampaignValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.*;

@RestController
@Validated
@RequestMapping("/api/core/campaigns")
public class CampaignController {

	private static final Logger logger = LoggerFactory.getLogger(CampaignController.class);
	private static final String SORT_FIELD_START_DATE = "startDate";


	@Autowired
	private CampaignService campaignService;

	@Autowired
	private CampaignValidationStrategy campaignValidationStrategy;

	@Autowired
	private AuditService auditService;


	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<CampaignDTO>>> getAllActiveCampaigns(
			@RequestParam(defaultValue = "") String description, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		logger.info("Calling Campaign getAllActiveCampaigns API with description={}, page={}, size={}",
				description, page, size);

		String activityType = "Active Campaign List";
		String endpoint = "/api/core/campaigns";
		HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";
		String userId = "Invalid UserID";

		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		try {	

			Pageable pageable = PageRequest.of(page, size, Sort.by(SORT_FIELD_START_DATE).ascending());
			Map<Long, List<CampaignDTO>> resultMap = campaignService.findAllActiveCampaigns(description, pageable);
			List<CampaignDTO> campaignDTOList = new ArrayList<>();

			long totalRecord = resultMap.keySet().stream().findFirst().orElse(0L);

			campaignDTOList = resultMap.getOrDefault(totalRecord, new ArrayList<>());

			logger.info("Total record: {}", totalRecord);
			logger.info("CampaignDTO List: {}", campaignDTOList);

			if (campaignDTOList.size() > 0) {
				message = "Successfully retrieve all active campaigns.";

				
				auditService.logAudit(auditDTO, 200, message,"");
				return ResponseEntity.status(HttpStatus.OK)
						.body(APIResponse.success(campaignDTOList, message, totalRecord));

			} else {
				message = "There are no available campaign list.";
				logger.error(message);
				
				auditService.logAudit(auditDTO, 200, message,"");
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(campaignDTOList, message));
			}

		} catch (Exception ex) {
			logger.error("An error occurred while processing getAllActiveCampaigns API.", ex);
			message = "The attempt to retrieve all active campaigns was unsuccessful.";
			auditDTO.setRemarks(ex.toString());
			
			auditService.logAudit(auditDTO, 500, message,"");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@PostMapping(value = "/stores", produces = "application/json")
	public ResponseEntity<APIResponse<List<CampaignDTO>>> getAllCampaignsByStoreId(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody CampaignRequest messagePayload,
			@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String description,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		logger.info(
				"Calling Campaign getAllCampaignsByStoreId API with userId={}, status={}, description={}, page={}, size={}",
				authorizationHeader, status, description, page, size);

		String activityType = "Campaign List by Store";
		String endpoint = "/api/core/campaigns/stores" ;
		HTTPVerb httpMethod = HTTPVerb.POST;
		String message = "";
		String userId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
			 
			String storeId = GeneralUtility.makeNotNull(messagePayload.getStoreId()).trim();
			if (storeId.isEmpty()) {
				message = "Bad Request: Store ID could not be blank.";
				logger.error(message);
			
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by(SORT_FIELD_START_DATE).ascending());
			Map<Long, List<CampaignDTO>> resultMap;

			if (status.isEmpty()) {
				resultMap = campaignService.findAllCampaignsByStoreId(storeId, description, pageable);
			} else {
				try {
					CampaignStatus campaignStatus = CampaignStatus.valueOf(status);
					resultMap = campaignService.findByStoreIdAndStatus(storeId, campaignStatus, pageable);
				} catch (IllegalArgumentException ex) {

					message = "Unable to retrieve all campaigns for the specified store ID. The campaign status provided is invalid.";
					logger.error(message, ex);
					auditDTO.setRemarks(ex.toString());
					
					auditService.logAudit(auditDTO, 404, message, authorizationHeader);

					throw ex;
				}
			}

			List<CampaignDTO> campaignDTOList = new ArrayList<CampaignDTO>();

			long totalRecord = resultMap.keySet().stream().findFirst().orElse(0L);

			campaignDTOList = resultMap.getOrDefault(totalRecord, new ArrayList<>());

			logger.info("Total record: {}", totalRecord);
			logger.info("CampaignDTO List: {}", campaignDTOList);
			if (campaignDTOList.size() > 0) {
				message = "Successfully retrieve all active campaigns.";
				
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(
						APIResponse.success(campaignDTOList, "Successfully get all active campaigns", totalRecord));

			} else {
				message = "No campaigns found for the specified store ID: " + storeId;
				logger.error(message);
				
				auditService.logAudit(auditDTO, 200, message,   authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(campaignDTOList, message));
			}

		} catch (Exception ex) {
			message = "The attempt to retrieve campaigns for the specified store ID was unsuccessful.";
			logger.error(message, ex);
			auditDTO.setRemarks(ex.toString());
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@PostMapping(value = "/users", produces = "application/json")
	public ResponseEntity<APIResponse<List<CampaignDTO>>> getCampaignsByUserId(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody CampaignRequest messagePayload,
			@RequestParam(defaultValue = "") String description, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		long totalRecord = 0;
		logger.info("Calling Campaign getAllCampaignsByEmail API with  description={}, page={}, size={}",
				 description, page, size);

		String activityType = "Campaign List by User";
		String endpoint = "/api/core/campaigns/users";
		HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";
		String xUserId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(xUserId, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {

			String userId = GeneralUtility.makeNotNull(messagePayload.getUserId()).trim();

			if (!userId.equals("")) {
				Pageable pageable = PageRequest.of(page, size, Sort.by(SORT_FIELD_START_DATE).ascending());

				Map<Long, List<CampaignDTO>> resultMap = campaignService.findAllCampaignsByUserId(userId, description,
						pageable);

				List<CampaignDTO> campaignDTOList = new ArrayList<CampaignDTO>();

				for (Map.Entry<Long, List<CampaignDTO>> entry : resultMap.entrySet()) {
					totalRecord = entry.getKey();
					campaignDTOList = entry.getValue();

					logger.info("totalRecord: " + totalRecord);
					logger.info("CampaignDTO List: " + campaignDTOList);

				}

				if (campaignDTOList.size() > 0) {
					message = "Successfully retrieved all campaigns for the specified user.";
					
					auditService.logAudit(auditDTO, 200, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.OK)
							.body(APIResponse.success(campaignDTOList, message, totalRecord));

				} else {
					message = "No campaigns were found for the specified user ID: " + userId;
					
					auditService.logAudit(auditDTO, 200, message,authorizationHeader);
					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(campaignDTOList, message));
				}
			} else {
				message = "Bad Request:Email could not be blank.";
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);

				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error("Bad Request:UserId could not be blank."));
			}

		} catch (Exception ex) {
			message = "The attempt to retrieve campaigns for the specified user was unsuccessful.";
			logger.error(message);
			auditDTO.setRemarks(ex.toString());
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@PostMapping(value = "/Id", produces = "application/json")
	public ResponseEntity<APIResponse<CampaignDTO>> getByCampaignId(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody CampaignRequest messagePayload) {

		logger.info("Calling get Campaign API...");
		String activityType = "Search Campaign by Id";
		String endpoint = "/api/core/campaigns" ;
		HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";
		String userId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
			
			String campaignId = GeneralUtility.makeNotNull(messagePayload.getCampaignId()).trim();

			if (!campaignId.equals("")) {

				CampaignDTO campaignDTO = campaignService.findByCampaignId(campaignId);

				if (campaignDTO.getCampaignId().equals(campaignId)) {
					message = "Successfully retrieved campaign with ID: " + campaignId;

					
					auditService.logAudit(auditDTO, 200, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(campaignDTO, message));
				} else {
					message = "Campaign not found for the specified campaign ID: " + campaignId;

					
					auditService.logAudit(auditDTO, 404, message,authorizationHeader);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));

				}

			} else {
				message = "Bad Request:CampaignId could not be blank.";
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

		} catch (Exception ex) {
			message = "The attempt retrieve campaing for specified campaign was unsuccessful.";
			logger.error(message);
			auditDTO.setRemarks(ex.toString());
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}

	}


	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<CampaignDTO>> createCampaign(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody Campaign campaign) {

		logger.info("Calling Campaign create API...");

		String activityType = "Create Campaign";
		String endpoint = "/api/core/campaigns";
		HTTPVerb httpMethod = HTTPVerb.POST;
		String message = "";
		String userId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
		
			ValidationResult validationResult = campaignValidationStrategy.validateCreation(campaign, null, authorizationHeader);
			if (validationResult.isValid()) {

				CampaignDTO campaignDTO = campaignService.create(campaign);
				if (campaignDTO != null && !campaignDTO.getCampaignId().isEmpty()) {
					message = "Campaign has been created successfully.";
					
					auditService.logAudit(auditDTO, 200, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(campaignDTO, message));
				} else {
					message = "Campaign creation process was unsuccessful.";
					
					auditService.logAudit(auditDTO, 500, message, authorizationHeader);
					logger.error(message);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
				}

			} else {
				message = validationResult.getMessage();
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

		} catch (Exception ex) {
			message = "An error has occurred while processing the Create Campaign API request.";
			auditDTO.setRemarks(ex.toString());

			logger.error(message, ex);
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@PutMapping(value = "/update", produces = "application/json")
	public ResponseEntity<APIResponse<CampaignDTO>> updateCampaign(
			@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody Campaign campaign) {

		logger.info("Calling Campaign update API...");

		
		String activityType = "Update Campaign";
		String endpoint = "/api/core/campaigns/update" ;
		HTTPVerb httpMethod = HTTPVerb.PUT;
		String message = "";
		String userId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
			
			
			String campaignId = GeneralUtility.makeNotNull(campaign.getCampaignId()).trim();

			if (!campaignId.equals("")) {
				campaign.setCampaignId(campaignId);
				
				ValidationResult validationResult = campaignValidationStrategy.validateUpdating(campaign, null,authorizationHeader);
				if (validationResult.isValid()) {
					CampaignDTO campaignDTO = campaignService.update(campaign);
					if (campaignDTO != null && !campaignDTO.getCampaignId().isEmpty()) {
						message = "Campaign has been updated successfully.";
						
						auditService.logAudit(auditDTO, 200, message, authorizationHeader);
						return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(campaignDTO, message));
					} else {
						logger.error("Calling Campaign create API failed...");
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.body(APIResponse.error(
										"The update for the campaign has failed. Please check the provided campaign ID:"
												+ campaign.getCampaignId()));
					}

				} else {
					message = validationResult.getMessage();
					logger.error(message);
					int status =404;
					if(validationResult.getStatus().equals(HttpStatus.BAD_REQUEST)) {
						status = 400;
					}
					auditService.logAudit(auditDTO, status, message, authorizationHeader);
					return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
				}
			} else {
				message = "Bad Request:Campaign ID could not be blank.";
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}

		} catch (Exception ex) {
			message = "An error has occurred while processing the Update Campaign API request.";
			auditDTO.setRemarks(ex.toString());

			logger.info(message);
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@PatchMapping(value = "/promote", produces = "application/json")
	public ResponseEntity<APIResponse<CampaignDTO>> promoteCampaign(
			@RequestHeader("Authorization") String authorizationHeader,  @RequestBody CampaignRequest messagePayload) {
		
		logger.info("Calling Campaign Promote API...");
		String activityType = "Promote Campaign";
		String endpoint = "/api/core/campaigns/promote" ;
		HTTPVerb httpMethod = HTTPVerb.PATCH;
		String message = "";
		String userId = "Invalid UserID";
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpMethod);

		
		
		try {
			userId=messagePayload.getUserId();
			ValidationResult validationResult = campaignValidationStrategy.validateObject(messagePayload.getCampaignId());
			if (!validationResult.isValid()) {
				message = validationResult.getMessage();
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}

			
			validationResult = campaignValidationStrategy.validateUser(userId, authorizationHeader);
			if (!validationResult.isValid()) {
				message = validationResult.getMessage();
				logger.error(message);
				
				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
				return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(message));
			}

			CampaignDTO campaignDTO = campaignService.promote(messagePayload.getCampaignId(), userId,authorizationHeader);
			if (campaignDTO != null && campaignDTO.getCampaignId() != null) {
				message = "Campaign has been promoted successfully.";
				
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(campaignDTO, message));
			} else {
				message = "Campaign promotion process has encountered an error and was unsuccessful.";
				logger.error(message);
				
				auditService.logAudit(auditDTO, 500, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
			}

		} catch (Exception ex) {
			message = "An error has occurred while processing the Promote Campaign API request..";
			logger.error(message);
			auditDTO.setRemarks(ex.toString());
			
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			logger.error("", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

}
