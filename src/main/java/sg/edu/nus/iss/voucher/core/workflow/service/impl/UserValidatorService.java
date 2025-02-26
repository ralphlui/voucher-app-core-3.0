package sg.edu.nus.iss.voucher.core.workflow.service.impl;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;
import sg.edu.nus.iss.voucher.core.workflow.utility.JSONReader;

@Service
public class UserValidatorService {
	
	private static final Logger logger = LoggerFactory.getLogger(UserValidatorService.class);
	
	@Autowired
	AuthAPICall apiCall;
	
	@Autowired
	private JSONReader jsonReader;

	public HashMap<Boolean, String> validateActiveUser(String userId, String validatedRole) {
	    HashMap<Boolean, String> resultMap = new HashMap<Boolean, String>();

	    try {
	        String responseStr = apiCall.validateActiveUser(userId);
	        JSONObject jsonResponse = jsonReader.parseJsonResponse(responseStr);

	        if (jsonResponse == null) {
	            resultMap.put(false, "Invalid API response.");
	            return resultMap;
	        }

	        JSONObject data = jsonReader.getDataFromResponse(jsonResponse);
	        Boolean success = jsonReader.getSuccessFromResponse(jsonResponse);
	        String message = jsonReader.getMessageFromResponse(jsonResponse);

	        if (data != null && !GeneralUtility.makeNotNull(data).isEmpty()) {
	            String id = (String) data.get("userID");
	            String userRole = (String) data.get("role");
	            if (!id.equals(userId) || !userRole.toUpperCase().equals(validatedRole)) {
	                message = "Invalid user Id or role";
	                resultMap.put(false, message);
	                return resultMap;
	            }
	        }
	        logger.info("validateActiveUser message "+ message);
	        logger.info("validateActiveUser success "+ success);
	        resultMap.put(success, message);

	    } catch (ParseException e) {
	        e.printStackTrace();
	        logger.error("Error parsing JSON response... {}", e.toString());
	    }

	    return resultMap;
	}
	
}
