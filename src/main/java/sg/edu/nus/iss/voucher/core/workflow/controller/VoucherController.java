package sg.edu.nus.iss.voucher.core.workflow.controller;

import java.util.HashMap;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.iss.voucher.core.workflow.dto.*;
import sg.edu.nus.iss.voucher.core.workflow.entity.*;
import sg.edu.nus.iss.voucher.core.workflow.enums.HTTPVerb;
import sg.edu.nus.iss.voucher.core.workflow.enums.UserRoleType;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;
import sg.edu.nus.iss.voucher.core.workflow.exception.CampaignNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.exception.VoucherNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.*;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;

@RestController
@Validated
@RequestMapping("/api/core/vouchers")
public class VoucherController {

	private static final Logger logger = LoggerFactory.getLogger(VoucherController.class);

	@Autowired
	private VoucherService voucherService;

	@Autowired
	private CampaignService campaignService;
	
	@Autowired
	private UserValidatorService userValidatorService;
	
	@Autowired
	private AuditService auditService;
	
	
	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	@GetMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<APIResponse<VoucherDTO>> getByVoucherId(@RequestHeader("X-User-Id") String userId,@PathVariable("id") String id) {
		String voucherId = id.trim();
		
		AuditDTO auditDTO = auditService.createAuditDTO(userId, "Find Voucher by Id.", activityTypePrefix,"/api/core/vouchers/"+id, HTTPVerb.GET);
        String message="";
		try {
			logger.info("Calling get Voucher API...");
			if (voucherId.isEmpty()) {
				message = "Bad Request:Voucher ID could not be blank.";
				logger.error(message);
				auditService.logAudit(auditDTO,400,message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(message));
			}

			VoucherDTO voucherDTO = voucherService.findByVoucherId(voucherId);
			
			if (voucherDTO.getVoucherId().equals(voucherId)) {

				message = "Successfully retrieved the voucher associated with the specified ID: " + voucherId;
				logger.info(message);
				auditService.logAudit(auditDTO,200,message);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(voucherDTO, message));
			}
			message = "The voucher could not be located using the specified voucher ID: " + voucherId;
			logger.error(message);
			auditService.logAudit(auditDTO,404,message);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));

		} catch (Exception ex) {
			message ="The attempt to retrieve the voucher ID " + voucherId +" was unsuccessful.";
			logger.error(message);
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO,404,message);
			
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(message));
		}

	}

	@PostMapping(value = "/claim", produces = "application/json")
	public ResponseEntity<APIResponse<VoucherDTO>> claimVoucher(@RequestHeader("X-User-Id") String userId,@RequestBody VoucherRequest voucherRequest) {
		AuditDTO auditDTO = auditService.createAuditDTO(userId, "Claim Voucher", activityTypePrefix,"/api/core/vouchers/claim", HTTPVerb.POST);
        String message="";
		try {
			logger.info("Calling Voucher claim API...");

			String campaignId = GeneralUtility.makeNotNull(voucherRequest.getCampaignId()).trim();
			String claimBy = voucherRequest.getClaimedBy();

			message = validateUser(claimBy);
			if (!message.isEmpty()) {
				auditService.logAudit(auditDTO,400,message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
			}
			
			//Validate Campaign
			Campaign campaign = validateCampaign(campaignId);

			//Check if Voucher Already Claimed
			if (isVoucherAlreadyClaimed(claimBy, campaign)) {
				message = "Voucher has already been claimed.";
				auditService.logAudit(auditDTO,400,message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(message));
			}

			//Check if Campaign is Fully Claimed
			if (isCampaignFullyClaimed(campaignId, campaign)) {
				message="Campaign has been fully claimed.";
				auditService.logAudit(auditDTO,401,message);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(message));
			}

			//Claim the Voucher
			VoucherDTO voucherDTO = voucherService.claimVoucher(voucherRequest);
			message = "Voucher has been successfully claimed.";
			auditService.logAudit(auditDTO,200,message);
			return ResponseEntity.status(HttpStatus.OK)
					.body(APIResponse.success(voucherDTO, message));

		} catch (CampaignNotFoundException ex) {
			message = "Campaign not Found.";
			logger.error(ex.getMessage());
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO,404,message);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));
		} catch (Exception ex) {
			logger.error("Calling Voucher claim API failed: " + ex.getMessage(), ex);
			message = "The attempt to claim the voucher has been unsuccessful.";
			
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO,404,message);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
		}
	}
	
	@GetMapping(value = "/users/{userId}", produces = "application/json")
	public ResponseEntity<APIResponse<List<VoucherDTO>>> findAllClaimedVouchersByUserId(@RequestHeader("X-User-Id") String XUserId,@PathVariable("userId") String userId,@RequestParam(defaultValue = "") String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {

		
		AuditDTO auditDTO = auditService.createAuditDTO(userId, "Claimed Voucher List by User", activityTypePrefix,"/api/core/vouchers/users/"+userId, HTTPVerb.GET);
        String message="";
        
		try {
			logger.info("Calling get Voucher by email API with status={}, page={}, size={}",status, page, size);

			if (!userId.equals("")) {

				Pageable pageable = PageRequest.of(page, size, Sort.by("claimTime").ascending());
				Map<Long, List<VoucherDTO>> resultMap = voucherService.findByClaimedByAndVoucherStatus(userId,status, pageable);
				
				
				Map.Entry<Long, List<VoucherDTO>> firstEntry = resultMap.entrySet().iterator().next();
				long totalRecord = firstEntry.getKey();
				List<VoucherDTO> voucherDTOList = firstEntry.getValue();

				if (voucherDTOList.size() > 0) {
					message = "Successfully retrieved all vouchers for the specified user.";
					auditService.logAudit(auditDTO,200,message);
					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(voucherDTOList,
							message, totalRecord));
				} else {
					message = "No Voucher list for the specified user: "+ userId;
					auditService.logAudit(auditDTO,200,message);
					return  ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(voucherDTOList, message));
				}

			} else {
				message="Bad Request: User id could not be blank.";
				logger.error(message);
				auditService.logAudit(auditDTO,400,message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(message));
			}
		} catch (Exception ex) {
			logger.error("Calling Voucher get Voucher by email API failed...");
			message =" Unable to retrieve the voucher for the specified user ID: " + userId;
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO,400,message);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(message));
		}

	}

	@GetMapping(value = "/campaigns/{campaignId}", produces = "application/json")
	public ResponseEntity<APIResponse<List<VoucherDTO>>> findAllClaimedVouchersBycampaignId(
			@RequestHeader("X-User-Id") String userId, @PathVariable("campaignId") String campaignId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {

		AuditDTO auditDTO = auditService.createAuditDTO(userId, "Claimed Voucher List by Campaign", activityTypePrefix,
				"/api/core/vouchers/campaigns/" + campaignId, HTTPVerb.GET);
		String message = "";

		try {
			logger.info("Calling get Voucher by campaignId API...");

			if (!campaignId.equals("")) {

				Pageable pageable = PageRequest.of(page, size, Sort.by("claimTime").ascending());

				Map<Long, List<VoucherDTO>> resultMap = voucherService.findAllClaimedVouchersByCampaignId(campaignId,
						pageable);

				Map.Entry<Long, List<VoucherDTO>> firstEntry = resultMap.entrySet().iterator().next();
				long totalRecord = firstEntry.getKey();
				List<VoucherDTO> voucherDTOList = firstEntry.getValue();

				if (voucherDTOList.size() > 0) {
					message = "Successfully retrieved claimed vouchers by campaignId: " + campaignId;
					;
					auditService.logAudit(auditDTO, 200, message);
					return ResponseEntity.status(HttpStatus.OK)
							.body(APIResponse.success(voucherDTOList, message, totalRecord));
				} else {
					message = "No Voucher List Available for Campaign ID: " + campaignId;
					auditService.logAudit(auditDTO, 200, message);
					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(voucherDTOList, message));
				}
			} else {
				message = "Bad Request:Campaign ID could not be blank.";
				logger.error(message);

				auditService.logAudit(auditDTO, 400, message);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error("Bad Request:CampaignId could not be blank."));
			}

		} catch (Exception ex) {
			logger.error("Calling Voucher get Voucher by campaignId API failed...");
			message = "Failed to get voucher for campaignId " + campaignId;
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO, 400, message);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(message));
		}

	}

	@PatchMapping(value = "{voucherID}/consume", produces = "application/json")
	public ResponseEntity<APIResponse<VoucherDTO>> consumeVoucher(@RequestHeader("X-User-Id") String userId,@PathVariable("voucherID") String voucherID) {
		String voucherId = GeneralUtility.makeNotNull(voucherID).trim();

		logger.info("Calling Voucher consume API...");
		
		AuditDTO auditDTO = auditService.createAuditDTO(userId, "Consume Voucher", activityTypePrefix,"/api/core/vouchers/"+voucherId+"/consume", HTTPVerb.PATCH);
        String message="";

		try {

			VoucherDTO voucherDTO = voucherService.findByVoucherId(voucherId);

			if (voucherDTO != null && !voucherDTO.getVoucherStatus().equals(VoucherStatus.CLAIMED)) {
				
				logger.error("Voucher already consumed or not in a claimable state. Id: {}", voucherId);
				message ="Voucher has already been consumed.";
				auditService.logAudit(auditDTO,401,message);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(message));
			}

			VoucherDTO updatedVoucherDTO = voucherService.consumeVoucher(voucherId);

			if (updatedVoucherDTO.getVoucherStatus().equals(VoucherStatus.CONSUMED)) {
				message ="Voucher has been successfully consumed.";
				auditService.logAudit(auditDTO,200,message);
				return ResponseEntity.status(HttpStatus.OK)
						.body(APIResponse.success(updatedVoucherDTO, message));
			} else {
				logger.error("Voucher consumption failed. Id: {}", voucherId);
				message ="The attempt to consume the voucher has been unsuccessful.";
				auditService.logAudit(auditDTO,500,message);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(message));
			}

		} catch (Exception ex) {
			logger.error("Exception during Voucher consume API call. Id: {}, Error: {}", voucherId, ex.getMessage());
			HttpStatusCode htpStatuscode = ex instanceof VoucherNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			message = "The attempt to consume the voucher has been unsuccessful.";
			auditDTO.setRemarks(ex.toString());
			auditService.logAudit(auditDTO,500,message);
			
			return ResponseEntity.status(htpStatuscode)
					.body(APIResponse.error(message));
		}
	}

	private String validateUser(String userId) {
		HashMap<Boolean, String> userMap = userValidatorService.validateActiveUser(userId, UserRoleType.CUSTOMER.toString());
		logger.info("user Id key map "+ userMap.keySet());
		
		for (Map.Entry<Boolean, String> entry : userMap.entrySet()) {
			logger.info("user role: " + entry.getValue());
			logger.info("user id: " + entry.getKey());
			
			if (!entry.getKey()) {
				String message = entry.getValue();
				logger.error(message);
				return message;
			}
		}
		return "";
	}

	// Validate Campaign
	private Campaign validateCampaign(String campaignId) {
		return campaignService.findById(campaignId)
				.orElseThrow(() -> new CampaignNotFoundException("Campaign not found by campaignId: " + campaignId));
	}

	// Check if the voucher has already been claimed by this user
	private boolean isVoucherAlreadyClaimed(String claimBy, Campaign campaign) {
		VoucherDTO voucherDTO = voucherService.findVoucherByCampaignIdAndUserId(campaign, claimBy);
		if (voucherDTO != null && voucherDTO.getVoucherId() != null && !voucherDTO.getVoucherId().isEmpty()) {
			logger.error("Voucher already claimed.");
			return true;
		}
		return false;
	}

	// Check if the campaign has already given out all its vouchers
	private boolean isCampaignFullyClaimed(String campaignId, Campaign campaign) {
		List<Voucher> claimedVoucherList = voucherService.findVoucherListByCampaignId(campaignId);
		logger.info("claimedVoucherList: " + claimedVoucherList);
		if (campaign.getNumberOfVouchers() <= claimedVoucherList.size()) {
			logger.error("Campaign is fully claimed.");
			return true;
		}
		return false;
	}
}
