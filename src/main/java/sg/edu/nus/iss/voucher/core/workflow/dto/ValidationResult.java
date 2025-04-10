package sg.edu.nus.iss.voucher.core.workflow.dto;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ValidationResult {
	private boolean isValid;
	private String message;
	private String imageUrl;
	private HttpStatus status;

}
