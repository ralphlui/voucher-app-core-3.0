package sg.edu.nus.iss.voucher.core.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CampaignRequest {
	private String campaignId;
	private String storeId;
	private String userId;

}
