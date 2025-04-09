package sg.edu.nus.iss.voucher.core.workflow.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.nus.iss.voucher.core.workflow.exception.CampaignNotFoundException;


@SpringBootTest
@ActiveProfiles("test")
public class CampaignNotFoundExceptionTest {
	@Test
    void testConstructor() {
         
        String errorMessage = "Campaign not found";
        CampaignNotFoundException exception = new CampaignNotFoundException(errorMessage);

        // Verify that the message is correctly set
        assertEquals(errorMessage, exception.getMessage());
    }
}
