package sg.edu.nus.iss.voucher.core.workflow.strategy.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.ValidationResult;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.StoreService;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;
import sg.edu.nus.iss.voucher.core.workflow.strategy.IAPIHelperValidationStrategy;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;

@Service
public class StoreValidationStrategy implements IAPIHelperValidationStrategy<Store> {
	
	@Autowired
	private StoreService storeService;
	
	@Autowired
	private UserValidatorService userValidatorService;

	@Override
	public ValidationResult validateCreation(Store store, MultipartFile val) {

		ValidationResult validationResult = new ValidationResult();
		String userId = GeneralUtility.makeNotNull(store.getCreatedBy());

		if (userId.isEmpty()) {
			validationResult.setMessage("Bad Request: User id field could not be blank.");
			validationResult.setValid(false);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			return validationResult;
		}

		ValidationResult validationObjResult = validateObject(userId);
		if (!validationObjResult.isValid()) {
			return validationObjResult;
		}

		if (store.getStoreName() == null || store.getStoreName().isEmpty()) {
			validationResult.setMessage("Bad Request: Store name could not be blank.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;

		}

		StoreDTO storeDTO = storeService.findByStoreName(store.getStoreName());
		try {
			if (GeneralUtility.makeNotNull(storeDTO.getStoreName().toLowerCase()).equals(store.getStoreName().toLowerCase())) {
				validationResult.setMessage("Store already exists.");
				validationResult.setStatus(HttpStatus.BAD_REQUEST);
				validationResult.setValid(false);
				return validationResult;

			}
		} catch (Exception ex) {
			if (storeDTO.getStoreId() != null) {

				validationResult.setMessage("Store already exists.");
				validationResult.setValid(false);
				return validationResult;

			}
		}
		validationResult.setValid(true);
		return validationResult;
	}
	
	@Override
	public ValidationResult validateObject(String userId) {

		ValidationResult validationResult = new ValidationResult();
		HashMap<Boolean, String> response = userValidatorService.validateActiveUser(userId, "MERCHANT");
		Boolean success = false;
		String message = "";
		for (Map.Entry<Boolean, String> entry : response.entrySet()) {
			success = entry.getKey();
			message = entry.getValue();
		}
		validationResult.setValid(success);
		validationResult.setMessage(message);
		if (!success) {
			validationResult.setStatus(HttpStatus.UNAUTHORIZED);
		}
		return validationResult;

	}


	@Override
	public ValidationResult validateUpdating(Store store, MultipartFile val) {
		ValidationResult validationResult = new ValidationResult();
		// Validate store ID
		String storeId = GeneralUtility.makeNotNull(store.getStoreId());
		if (storeId.isEmpty()) {
			validationResult.setMessage("Bad Request: Store ID could not be blank.");
			validationResult.setValid(false);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			return validationResult;

		}

		StoreDTO storeDTO = storeService.findByStoreId(storeId);

		if (storeDTO == null || storeDTO.getStoreId() == null || storeDTO.getStoreId().isEmpty()) {
			validationResult.setMessage("Invalid store Id: " + storeId);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		// Check for updated by user
		String userId = GeneralUtility.makeNotNull(store.getUpdatedBy());
		if (userId.isEmpty()) {
			validationResult.setMessage("Bad Request: User ID field could not be blank.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}

		ValidationResult validationObjResult = validateObject(userId);
		if (!validationObjResult.isValid()) {
			return validationObjResult;
		}

		validationResult.setValid(true);
		return validationResult;
	}

}
