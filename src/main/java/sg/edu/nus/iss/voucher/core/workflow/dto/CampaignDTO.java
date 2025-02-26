package sg.edu.nus.iss.voucher.core.workflow.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

@Getter
@Setter
public class CampaignDTO {

    private String campaignId;
	private String description;
    private StoreDTO store;
	private CampaignStatus campaignStatus;
	private String tagsJson;
	private int numberOfVouchers;
	private int numberOfLikes;
	private String pin;
	private String tandc;
	private double amount;
    private LocalDateTime startDate;
	private LocalDateTime endDate;
    private String createdBy;
    private String updatedBy;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
    private int numberOfClaimedVouchers;
    private String category;

    public CampaignDTO(){
    }
   
}