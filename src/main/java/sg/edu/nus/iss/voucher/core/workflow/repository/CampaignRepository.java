package sg.edu.nus.iss.voucher.core.workflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, String> {

    
    @Query("SELECT c FROM Campaign c WHERE c.campaignStatus IN ?1")
    List<Campaign> findByCampaignStatusIn(List<CampaignStatus> statuses);

    Page<Campaign> findByStoreStoreId(String storeId,Pageable pageable);
    
    Page<Campaign> findByStoreStoreIdAndCampaignStatus(String storeId,CampaignStatus status,Pageable pageable);

    Page<Campaign> findByCreatedBy(String userId,Pageable pageable);
    
    List<Campaign>  findByDescription(String description);
    
    @Query("SELECT c FROM Campaign c WHERE c.campaignStatus IN ?1")
    Page<Campaign> findByCampaignStatusIn(List<CampaignStatus> statuses,Pageable pageable);
    
    Page<Campaign> findByEndDateBefore(LocalDateTime now, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.campaignStatus IN ?1 AND c.description LIKE %?2%")
    Page<Campaign> findByCampaignStatusInAndDescriptionLike(List<CampaignStatus> statuses,String description,Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.createdBy = ?1 AND c.description LIKE %?2%")
    Page<Campaign> findByCreatedByAndDescriptionLike(String userId,String description,Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.store.storeId = ?1 AND c.description LIKE %?2%")
    Page<Campaign> findByStoreStoreIdAndDescriptionLike(String storeId,String description,Pageable pageable);
    
    @Transactional
    @Modifying
    @Query("UPDATE Campaign c SET c.campaignStatus = 'EXPIRED', c.updatedDate = :now WHERE c.endDate < :now")
    int updateExpiredCampaigns(@Param("now") LocalDateTime now);
    
    List<Campaign>  findByEndDateBeforeAndCampaignStatusNot(LocalDateTime now,CampaignStatus status );


}