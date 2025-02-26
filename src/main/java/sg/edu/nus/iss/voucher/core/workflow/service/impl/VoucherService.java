package sg.edu.nus.iss.voucher.core.workflow.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherRequest;
import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.entity.Voucher;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;
import sg.edu.nus.iss.voucher.core.workflow.exception.VoucherNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.repository.CampaignRepository;
import sg.edu.nus.iss.voucher.core.workflow.repository.VoucherRepository;
import sg.edu.nus.iss.voucher.core.workflow.service.IVoucherService;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;

@Service
public class VoucherService implements IVoucherService {
	
	private static final Logger logger = LoggerFactory.getLogger(VoucherService.class);

	@Autowired
	private VoucherRepository voucherRepository;
	

	@Autowired
	private CampaignRepository campaignRepository;
	
	@Autowired
	AuthAPICall apiCall;
	
	
	@Override
	public VoucherDTO findByVoucherId(String voucherId) throws Exception {
		try {
			Voucher voucher = voucherRepository.findById(voucherId).orElse(null);
			
			if (voucher == null) {
				logger.error("Voucher not found...");
				throw new VoucherNotFoundException("The voucher with the specified ID "+ voucherId+ "could not be found in the system.");
			}
			
			logger.info("Voucher found...");
			VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher);
			logger.info("Voucher DTO found...");
			List<Voucher> voucherList = voucherRepository
					.findByCampaignCampaignId(voucherDTO.getCampaign().getCampaignId());
			voucherDTO.getCampaign().setNumberOfClaimedVouchers(voucherList.size());
			return voucherDTO;
			
		} catch (Exception ex) {
			logger.error("Finding voucher by voucher Id exception... {}", ex.toString());
			throw ex;

		}
	}

	@Override
	public VoucherDTO findVoucherByCampaignIdAndUserId(Campaign campaign, String userId) {

		logger.info("Getting voucher for CampaignId" + campaign.getCampaignId() + " " + userId);
		VoucherDTO voucherDTO = new VoucherDTO();
		try {
			Voucher voucher = voucherRepository.findByCampaignAndClaimedBy(campaign, userId);
			if (voucher != null) {
				voucherDTO = DTOMapper.toVoucherDTO(voucher);
			}

		} catch (Exception ex) {
			logger.error("Finding voucher by campaing Id and userId exception... {}", ex.toString());

		}
		return voucherDTO;
	}

	@Override
	public List<Voucher> findVoucherListByCampaignId(String campaignId) {
		// TODO Auto-generated method stub
		return voucherRepository.findByCampaignCampaignId(campaignId);
	}

	@Override
	public VoucherDTO claimVoucher(VoucherRequest voucherRequest) throws Exception {
		try {

			Voucher voucher = new Voucher();
			Campaign campaign = campaignRepository.findById(voucherRequest.getCampaignId()).orElseThrow();
			voucher.setVoucherStatus(VoucherStatus.CLAIMED);
			voucher.setClaimedBy(voucherRequest.getClaimedBy());
			voucher.setClaimTime(LocalDateTime.now());
			voucher.setCampaign(campaign);
			logger.info("Saving voucher...");
			Voucher savedVoucher = voucherRepository.save(voucher);
			logger.info("Saved successfully...");

			if (savedVoucher == null) {
				logger.error("Voucher claim failed....");
				throw new Exception("The attempt to claim the voucher has been unsuccessful.");
			}

			VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(savedVoucher);
			List<Voucher> voucherList = voucherRepository
					.findByCampaignCampaignId(voucherDTO.getCampaign().getCampaignId());
			voucherDTO.getCampaign().setNumberOfClaimedVouchers(voucherList.size());

			return voucherDTO;
		} catch (Exception ex) {
			logger.error("Voucher saving exception... {}"+ ex.toString());
			throw ex;
		}
	}

	@Override
	public Map<Long, List<VoucherDTO>> findByClaimedByAndVoucherStatus(String userId,String status, Pageable pageable) {
		logger.info("Getting all claimed voucher for user {}...", userId);

		try {
			VoucherStatus voucherStatus = !status.isEmpty() ? VoucherStatus.valueOf(status): null;
			Page<Voucher> voucherPages = voucherStatus != null ? voucherRepository.findByClaimedByAndVoucherStatus(userId, voucherStatus,pageable) : voucherRepository.findByClaimedBy(userId,pageable);
			
			long totalRecord = voucherPages.getTotalElements();
			List<VoucherDTO> voucherDTOList = new ArrayList<VoucherDTO>();
			
			if (totalRecord > 0) {
				for (Voucher voucher : voucherPages) {
					VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher);
					List<Voucher> voucherList = voucherRepository
							.findByCampaignCampaignId(voucherDTO.getCampaign().getCampaignId());
					voucherDTO.getCampaign().setNumberOfClaimedVouchers(voucherList.size());
					voucherDTOList.add(voucherDTO);

				}
			}
			Map<Long, List<VoucherDTO>> result = new HashMap<>();
			result.put(totalRecord, voucherDTOList);
			return result;

		} catch (Exception ex) {
			logger.error("Retrieiving voucher by user exception " +ex.toString());
			throw ex;
		}
	}

	@Override
	public Map<Long, List<VoucherDTO>> findAllClaimedVouchersByCampaignId(String campaignId, Pageable pageable) {
		logger.info("Getting all claimed voucher for campaign id {}...", campaignId);
		try {
			Page<Voucher> voucherPages = voucherRepository.findByCampaignCampaignId(campaignId, pageable);
			long totalRecord = voucherPages.getTotalElements();
			List<VoucherDTO> voucherDTOList = new ArrayList<VoucherDTO>();
			if (totalRecord > 0) {

				for (Voucher voucher : voucherPages) {
					VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(voucher);
					voucherDTO.getCampaign().setNumberOfClaimedVouchers((int) totalRecord);
					voucherDTOList.add(voucherDTO);
				}
			}
			
			Map<Long, List<VoucherDTO>> result = new HashMap<>();
			result.put(totalRecord, voucherDTOList);
			return result;
		} catch (Exception ex) {
			logger.error("Retrieiving voucher by campaign id exception " + ex.toString());
			throw ex;
		}
	}

	@Override
	public VoucherDTO consumeVoucher(String voucherId)  {
		try {
			// Add validation here to make sure the same userId is passed

			Voucher dbVoucher = voucherRepository.findById(voucherId).orElseThrow();
			if (dbVoucher == null) {
				logger.info("Voucher Id {} is not found.", voucherId);
				throw new VoucherNotFoundException("The voucher with the specified ID "+ voucherId+ "could not be found in the system.");
			}
			dbVoucher.setConsumedTime(LocalDateTime.now());
			dbVoucher.setVoucherStatus(VoucherStatus.CONSUMED);
			logger.info("Consuming voucher...");
			Voucher savedVoucher = voucherRepository.save(dbVoucher);
			logger.info("Consumed successfully...");

			VoucherDTO voucherDTO = DTOMapper.toVoucherDTO(savedVoucher);
			List<Voucher> voucherList = voucherRepository
					.findByCampaignCampaignId(voucherDTO.getCampaign().getCampaignId());
			voucherDTO.getCampaign().setNumberOfClaimedVouchers(voucherList.size());

			return voucherDTO;
		} catch (Exception ex) {
			logger.error("Voucher consuming exception... {}", ex.toString());
			throw ex;
		}
	}

}
