package sg.edu.nus.iss.voucher.core.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.nus.iss.voucher.core.workflow.exception.StoreNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class StoreNotFoundExceptionTest {

	@Test
    void testConstructor() {
        // Create an instance of StoreNotFoundException
        String errorMessage = "User not found";
        StoreNotFoundException exception = new StoreNotFoundException(errorMessage);

        // Verify that the message is correctly set
        assertEquals(errorMessage, exception.getMessage());
    }
}
