package sg.edu.nus.iss.voucher.core.workflow.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

@Component
public class JSONReader {

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
}
