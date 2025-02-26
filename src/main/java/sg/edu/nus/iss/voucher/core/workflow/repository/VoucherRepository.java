package sg.edu.nus.iss.voucher.core.workflow.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.entity.Voucher;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

	@Query("SELECT v FROM Voucher v WHERE v.campaign= ?1 AND v.claimedBy = ?2")
	Voucher findByCampaignAndClaimedBy(Campaign campaign, String claimedBy);

	List<Voucher> findByCampaignCampaignId(String campaignId);
	
	Page<Voucher> findByClaimedByAndVoucherStatus(String claimedBy, VoucherStatus voucherStatus,Pageable pageable);

	Page<Voucher> findByCampaignCampaignId(String campaignId, Pageable pageable);
	
	Page<Voucher> findByClaimedBy(String claimedBy, Pageable pageable);

}
