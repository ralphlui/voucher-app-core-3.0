package sg.edu.nus.iss.voucher.core.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import sg.edu.nus.iss.voucher.core.workflow.dto.CampaignDTO;
import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

public interface ICampaignService {

	Map<Long, List<CampaignDTO>> findAllActiveCampaigns(String description,Pageable pageable);

	Map<Long, List<CampaignDTO>> findAllCampaignsByStoreId(String storeId,String description, Pageable pageable);

	Map<Long, List<CampaignDTO>> findAllCampaignsByUserId(String userId,String description, Pageable pageable);

	CampaignDTO findByCampaignId(String campaignId);

	CampaignDTO create(Campaign campaign);

	CampaignDTO update(Campaign campaign);

	CampaignDTO promote(String campaignId,String userId);

	int expired();

	List<Campaign> findByDescription(String description);

	Optional<Campaign> findById(String campaignId);

	Map<Long, List<CampaignDTO>> findByStoreIdAndStatus(String storeId, CampaignStatus status, Pageable pageable);

}
