package sg.edu.nus.iss.voucher.core.workflow.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.nus.iss.voucher.core.workflow.api.connector.AuthAPICall;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.UserValidatorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;

@SpringBootTest
@ActiveProfiles("test")
public class UserValidatorServiceTest {
	
	@Autowired
	private UserValidatorService userValidatorService;

	    @MockBean
	    private AuthAPICall apiCall;

	  @Test
	    public void testValidateActiveUser() throws ParseException {
			String mockResponse = "{\"totalRecord\":1,\"success\":true,\"data\":{\"userID\":\"US1\",\"email\":\"test1@example.com\",\"username\":\"user1\",\"role\":\"MERCHANT\"}}";
	        when(apiCall.validateActiveUser(anyString())).thenReturn(mockResponse);

	        HashMap<Boolean, String> result = userValidatorService.validateActiveUser("US1", "MERCHANT");
	        Boolean success = false;
	        
	        for (Map.Entry<Boolean, String> entry : result.entrySet()) {
				success = entry.getKey();
			}

	        assertNotNull(result);
	        assertEquals(1, result.size());
	        assertEquals(success, true);
	        
	        mockResponse = "{\"totalRecord\":0,\"success\":false,\"data\": null}";

	        when(apiCall.validateActiveUser(anyString())).thenReturn(mockResponse);
	        result = userValidatorService.validateActiveUser("US1", "MERCHANT");
	        
	        for (Map.Entry<Boolean, String> entry : result.entrySet()) {
				success = entry.getKey();
			}

	        assertEquals(success, false);

	    }
}

