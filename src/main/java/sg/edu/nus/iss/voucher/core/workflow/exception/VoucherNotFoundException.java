package sg.edu.nus.iss.voucher.core.workflow.exception;

public class VoucherNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public VoucherNotFoundException(String message) {
		super(message);
	}
}
