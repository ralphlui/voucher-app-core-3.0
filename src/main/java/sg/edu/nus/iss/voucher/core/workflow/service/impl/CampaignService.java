package sg.edu.nus.iss.voucher.core.workflow.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.voucher.core.workflow.aws.service.SNSPublishingService;
import sg.edu.nus.iss.voucher.core.workflow.dto.*;
import sg.edu.nus.iss.voucher.core.workflow.entity.*;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;
import sg.edu.nus.iss.voucher.core.workflow.repository.*;
import sg.edu.nus.iss.voucher.core.workflow.service.ICampaignService;
import sg.edu.nus.iss.voucher.core.workflow.utility.*;


@Service
public class CampaignService implements ICampaignService {

	private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);
	
	@Autowired
	private CampaignRepository campaignRepository;

	@Autowired
	private StoreRepository storeRepository;
	
	@Autowired
	private VoucherRepository voucherRepository;

	@Autowired
	private SNSPublishingService messagePublishService;

	@Override
	public Map<Long, List<CampaignDTO>> findAllActiveCampaigns(String description,Pageable pageable) {
		logger.info("Getting all active campaigns...");
		Map<Long, List<CampaignDTO>> result = new HashMap<>();
		List<CampaignDTO> campaignDTOList = new ArrayList<>();
		Page<Campaign> campaignPages = null ; 
		
		if (description.isEmpty()) {
			campaignPages = campaignRepository.findByCampaignStatusIn(Arrays.asList(CampaignStatus.PROMOTED),
					pageable);
		}else {
			campaignPages = campaignRepository.findByCampaignStatusInAndDescriptionLike(Arrays.asList(CampaignStatus.PROMOTED),description,
					pageable);
		}
		
		long totalRecord = campaignPages.getTotalElements();

		if (totalRecord > 0) {
			for (Campaign campaign : campaignPages.getContent()) {
				
				CampaignDTO campaignDTO = DTOMapper.toCampaignDTO(campaign);
				campaignDTOList.add(campaignDTO);
			}

		} else {
			logger.info("Campaign not found...");
		}

		result.put(totalRecord, campaignDTOList);

		return result;
	}

	@Override
	public Map<Long, List<CampaignDTO>> findAllCampaignsByStoreId(String storeId,String description, Pageable pageable) {
		logger.info("Getting all campaigns by Store Id...");
		Map<Long, List<CampaignDTO>> result = new HashMap<>();
		Page<Campaign> campaignPages = null ;
		
		if (description.isEmpty()) {
			 campaignPages = campaignRepository.findByStoreStoreId(storeId, pageable);
		}else {
			 campaignPages = campaignRepository.findByStoreStoreIdAndDescriptionLike(storeId,description, pageable);
		}
	
		long totalRecord = campaignPages.getTotalElements();
		List<CampaignDTO> campaignDTOList = new ArrayList<>();

		if (totalRecord > 0) {
			for (Campaign campaign : campaignPages) {
				campaign.setVoucher(voucherRepository.findByCampaignCampaignId(campaign.getCampaignId()));
				campaignDTOList.add(DTOMapper.toCampaignDTO(campaign));
			}
		} else {
			logger.info("Campaign not found...");
		}

		result.put(totalRecord, campaignDTOList);
		return result;
	}

	@Override
	public Map<Long, List<CampaignDTO>> findByStoreIdAndStatus(String storeId, CampaignStatus campaignStatus,
			Pageable pageable) {
		logger.info("Getting all campaigns by Store Id and Status...");

		Map<Long, List<CampaignDTO>> result = new HashMap<>();

		Page<Campaign> campaignPages = campaignRepository.findByStoreStoreIdAndCampaignStatus(storeId, campaignStatus,
				pageable);

		long totalRecord = campaignPages.getTotalElements();
		List<CampaignDTO> campaignDTOList = new ArrayList<>();
		if (totalRecord > 0) {
			for (Campaign campaign : campaignPages) {
				campaign.setVoucher(voucherRepository.findByCampaignCampaignId(campaign.getCampaignId()));
				campaignDTOList.add(DTOMapper.toCampaignDTO(campaign));
			}
		} else {
			logger.info("Campaign not found...");
		}
		result.put(totalRecord, campaignDTOList);
		return result;
	}

	@Override
	public Map<Long, List<CampaignDTO>> findAllCampaignsByUserId(String userId,String description, Pageable pageable) {
		logger.info("Getting all campaigns by email...");
		Map<Long, List<CampaignDTO>> result = new HashMap<>();
		Page<Campaign> campaignPages = null;
		
		if (description.isEmpty()) {
			campaignPages = campaignRepository.findByCreatedBy(userId, pageable);
		}else {
			campaignPages = campaignRepository.findByCreatedByAndDescriptionLike(userId,description, pageable);
		}

		long totalRecord = campaignPages.getTotalElements();
		List<CampaignDTO> campaignDTOList = new ArrayList<>();
		if (totalRecord > 0) {
			for (Campaign campaign : campaignPages) {
				campaign.setVoucher(voucherRepository.findByCampaignCampaignId(campaign.getCampaignId()));
				campaignDTOList.add(DTOMapper.toCampaignDTO(campaign));
			}
		}
		result.put(totalRecord, campaignDTOList);
		return result;
	}

	@Override
	public CampaignDTO findByCampaignId(String campaignId) {
		logger.info("Getting campaign for campaignId {} ...", campaignId);
		Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
		if (campaign != null) {
			logger.info("Campaign found...");
			campaign.setVoucher(voucherRepository.findByCampaignCampaignId(campaignId));
			return DTOMapper.toCampaignDTO(campaign);
		}
		logger.warn("Didn't find any campaign for campaignId {}...", campaignId);
		return null;
	}

	@Override
	public CampaignDTO create(Campaign campaign) {
		CampaignDTO campaignDTO = new CampaignDTO();
		try {

			Store store = storeRepository.findById(campaign.getStore().getStoreId()).orElseThrow();
			campaign.setPin(String.valueOf(new Random().nextInt(9000) + 1000));
			campaign.setCreatedBy(campaign.getCreatedBy());
			campaign.setCreatedDate(LocalDateTime.now());
			campaign.setStore(store);
			logger.info("Saving campaign...");
			Campaign savedCampaign = campaignRepository.save(campaign);
			logger.info("Saved successfully...");
			campaignDTO = DTOMapper.toCampaignDTO(savedCampaign);

		} catch (Exception ex) {
			logger.error("Campaign saving exception... {}", ex.toString());

		}
		return campaignDTO;
	}

	@Override
	public CampaignDTO update(Campaign campaign) {
		CampaignDTO campaignDTO = new CampaignDTO();
		try {
			Optional<Campaign> dbCampaign = campaignRepository.findById(campaign.getCampaignId());
			
			dbCampaign.get().setDescription(GeneralUtility.makeNotNull(campaign.getDescription()));
			dbCampaign.get().setAmount(campaign.getAmount());
			dbCampaign.get().setStartDate(campaign.getStartDate());
			dbCampaign.get().setEndDate(campaign.getEndDate());
			dbCampaign.get().setNumberOfLikes(campaign.getNumberOfLikes());
			dbCampaign.get().setNumberOfVouchers(campaign.getNumberOfVouchers());
			dbCampaign.get().setTagsJson(GeneralUtility.makeNotNull(campaign.getTagsJson()));
			dbCampaign.get().setTandc(GeneralUtility.makeNotNull(campaign.getTandc()));
			dbCampaign.get().setUpdatedBy(campaign.getUpdatedBy());
			dbCampaign.get().setUpdatedDate(LocalDateTime.now());
			logger.info("Update campaign...");
			Campaign savedCampaign = campaignRepository.save(dbCampaign.get());
			logger.info("Updated successfully...");
			campaignDTO = DTOMapper.toCampaignDTO(savedCampaign);

		} catch (Exception ex) {
			logger.error("Campaign updating exception... {}", ex.toString());

		}

		return campaignDTO;

	}

	@Override
	public CampaignDTO promote(String campaignId,String userId) {
		CampaignDTO campaignDTO = new CampaignDTO();
		try {

			Optional<Campaign> dbCampaign = campaignRepository.findById(campaignId);
			if (dbCampaign.isPresent()) {
				logger.info("Promoting campaign: status {}..", dbCampaign.get().getCampaignStatus());
				if (dbCampaign.get().getCampaignStatus().equals(CampaignStatus.CREATED)) {

					LocalDateTime startDate = dbCampaign.get().getStartDate();
					LocalDateTime endDate = dbCampaign.get().getEndDate();

					logger.info("Promoting campaign:startDate{} ,endDate{}...", startDate, endDate);

					if ((startDate.isAfter(LocalDateTime.now()) || startDate.equals(LocalDateTime.now()))
							&& endDate.isAfter(LocalDateTime.now())) {
						dbCampaign.get().setCampaignStatus(CampaignStatus.PROMOTED);
						dbCampaign.get().setUpdatedBy(userId);
						dbCampaign.get().setUpdatedDate(LocalDateTime.now());
						Campaign promottedCampaign = campaignRepository.save(dbCampaign.get());
						logger.info("Promotted successfully...");
						campaignDTO = DTOMapper.toCampaignDTO(promottedCampaign);
						
						messagePublishService.sendNotification(promottedCampaign);
						logger.info("Feed generated successfully...");
					} else {
						logger.info(
								"Promoting campaign Failed: startDate{} should not be greater than current date and endDate{} should not be less than current date...",
								startDate, endDate);
					}
				}
			}

		} catch (Exception ex) {
			logger.error("Campaign Promoting exception... {}", ex.toString());

		}
		return campaignDTO;
	}

	@Override
	public List<Campaign> findByDescription(String description) {
		// TODO Auto-generated method stub
		return campaignRepository.findByDescription(description);
	}

	@Override
	public Optional<Campaign> findById(String campaignId) {
		// TODO Auto-generated method stub
		return campaignRepository.findById(campaignId);
	}
	
	@Override
	public int expired() {
		int updatedCount = 0;
		List<Campaign> campaigns = campaignRepository.findByEndDateBeforeAndCampaignStatusNot(LocalDateTime.now(),
				CampaignStatus.EXPIRED);
		if (campaigns.size() > 0) {
			updatedCount = campaignRepository.updateExpiredCampaigns(LocalDateTime.now());
			logger.info("Updated {} campaigns", updatedCount);
		}
		return updatedCount;
	}

	
}