package sg.edu.nus.iss.voucher.core.workflow.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;



@SpringBootTest
@ActiveProfiles("test")
public class VoucherNotFoundExceptionTest {
	@Test
    void testConstructor() {
        
        String errorMessage = "Voucher not found";
        VoucherNotFoundException exception = new VoucherNotFoundException(errorMessage);

        assertEquals(errorMessage, exception.getMessage());
    }
}
