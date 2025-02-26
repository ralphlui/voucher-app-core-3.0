package sg.edu.nus.iss.voucher.core.workflow.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;

@Getter
@Setter
public class VoucherDTO {

    private String voucherId;
	private CampaignDTO campaign;
	private VoucherStatus voucherStatus = VoucherStatus.CLAIMED;
	private double amount;
	private LocalDateTime claimTime;
	private LocalDateTime consumedTime;
	private String claimedBy;

    public VoucherDTO(){
    	
    }

}
