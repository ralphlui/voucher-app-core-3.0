package sg.edu.nus.iss.voucher.core.workflow.exception;

public class CampaignNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public CampaignNotFoundException(String message) {
		super(message);
	}
}
