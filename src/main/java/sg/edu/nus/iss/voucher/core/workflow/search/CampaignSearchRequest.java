package sg.edu.nus.iss.voucher.core.workflow.search;

import jakarta.validation.constraints.Size;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

public class CampaignSearchRequest {
       
    @Size(min = 3, max = 50, message = "Description must be between 3 and 50 characters")
    private String description;
    
    private int page = 0;

    private int size = 10;
    
    private CampaignStatus status;


	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CampaignStatus getStatus() {
		return status;
	}

	public void setStatus(CampaignStatus status) {
		this.status = status;
	} 	
	
	

}
