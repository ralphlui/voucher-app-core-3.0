package sg.edu.nus.iss.voucher.core.workflow.dto;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResult {
	private boolean isValid;
	private String message;
	private String imageUrl;
	private HttpStatus status;

	public ValidationResult() {

	}
}
