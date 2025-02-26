package sg.edu.nus.iss.voucher.core.workflow.aws.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class SQSPublishingServiceTest {

	@Mock
	private AmazonSQS amazonSQS;

	@InjectMocks
	private SQSPublishingService sqsPublishingService;

	 AuditDTO auditDTO;
	
	@Value("${aws.sqs.queue.audit.url}")
	String auditQueueURL;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		auditDTO = new AuditDTO();
		auditDTO.setUserId("123");
		auditDTO.setRemarks("This is a test audit message.");
	}
	
	@Test
    public void testSendMessage_Success() throws Exception {
		 ObjectMapper objectMapper = new ObjectMapper();

        String messageBody = objectMapper.writeValueAsString(auditDTO);

        // Call the sendMessage method
        sqsPublishingService.sendMessage(auditDTO);

        // Capture the SendMessageRequest
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSQS).sendMessage(captor.capture());

        // Verify the contents of the SendMessageRequest
        SendMessageRequest request = captor.getValue();
        assertEquals("http://mock-sqs-url", request.getQueueUrl());
        assertEquals(messageBody, request.getMessageBody());
       
    }

}
