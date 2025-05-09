package sg.edu.nus.iss.voucher.core.workflow.search;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

@Getter
@Setter
public class CampaignSearchRequest {
       
	@Size(min = 1, max = 50, message = "Description must be between 1 and 50 characters")
	private String description;
    
	@Min(0)
    private int page = 0;

    @Min(1)
    private int size = 50;
    
    private CampaignStatus status;
	

}
