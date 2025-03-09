package sg.edu.nus.iss.voucher.core.workflow.strategy;

import org.springframework.web.multipart.MultipartFile;

import sg.edu.nus.iss.voucher.core.workflow.dto.ValidationResult;

public interface IAPIHelperValidationStrategy<T> {
	
	ValidationResult validateCreation(T data, MultipartFile val, String header);
	
	ValidationResult validateUpdating(T data, MultipartFile val, String header);

	ValidationResult validateObject(String data);
	
	ValidationResult validateObject(String data, String header);

}
