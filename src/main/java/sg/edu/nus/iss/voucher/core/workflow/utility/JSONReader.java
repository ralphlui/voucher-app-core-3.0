package sg.edu.nus.iss.voucher.core.workflow.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.pojo.User;


@Component
public class JSONReader {

	@Autowired
	AuthAPICall apiCall;
	
	private static final Logger logger = LoggerFactory.getLogger(JSONReader.class);
	
	public JSONObject parseJsonResponse(String responseStr) throws ParseException {
		if (responseStr == null || responseStr.isEmpty()) {
			return null;
		}

		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(responseStr);
	}

	public JSONObject getDataFromResponse(JSONObject jsonResponse) {
		if (jsonResponse != null && !jsonResponse.isEmpty()) {
			return (JSONObject) jsonResponse.get("data");
		}
		return null;
	}

	public String getMessageFromResponse(JSONObject jsonResponse) {
		return (String) jsonResponse.get("message");
	}

	public Boolean getSuccessFromResponse(JSONObject jsonResponse) {
		return (Boolean) jsonResponse.get("success");
	}
	
	public int getStatusFromResponse(JSONObject jsonResponse) {
		Long status = (Long) jsonResponse.get("status");
		return status.intValue();
	}
	
	public JSONObject getActiveUser(String userId,String authorizationHeader) {

		JSONObject jsonResponse = new JSONObject();

		String responseStr = apiCall.validateActiveUser(userId, authorizationHeader);

		try {

			JSONParser parser = new JSONParser();
		    jsonResponse = (JSONObject) parser.parse(responseStr);
			return jsonResponse;

		} catch (ParseException e) {
			e.printStackTrace();
			logger.error("Error parsing JSON response for getActiveUserDetails... {}", e.toString());

		}

		return jsonResponse;
	}
	
	public User getUserObject(JSONObject userJSONObject) {
		User var = new User();
		JSONObject data = getDataFromResponse(userJSONObject);
		if (data != null) {
			logger.info("User: " + data.toJSONString());
			String userName = GeneralUtility.makeNotNull(data.get("username").toString());
			String email = GeneralUtility.makeNotNull(data.get("email").toString());
			String role = GeneralUtility.makeNotNull(data.get("role").toString());
			String userID = GeneralUtility.makeNotNull(data.get("userID").toString());
			var.setUserId(userID);
			var.setEmail(email);
			var.setRole(role);
			var.setUsername(userName);
		}

		return var;
	}
	
	public User getActiveUserDetails(String userId,String token) {

		User var = new User();

		String responseStr = apiCall.validateActiveUser(userId,token);

		try {

			JSONParser parser = new JSONParser();
			JSONObject jsonResponse = (JSONObject) parser.parse(responseStr);
			Boolean success =getSuccessFromResponse(jsonResponse);
			if (success) {
				JSONObject data = getDataFromResponse(jsonResponse);
				logger.info("User: " + data.toJSONString());
				String userName = GeneralUtility.makeNotNull(data.get("username").toString());
				String email = GeneralUtility.makeNotNull(data.get("email").toString());
				String role = GeneralUtility.makeNotNull(data.get("role").toString());
				var.setUserId(userId);
				var.setEmail(email);
				var.setRole(role);
				var.setUsername(userName);
			}

		} catch (ParseException e) {
			e.printStackTrace();
			logger.error("Error parsing JSON response for getActiveUserDetails... {}", e.toString());

		}

		return var;
	}
}
