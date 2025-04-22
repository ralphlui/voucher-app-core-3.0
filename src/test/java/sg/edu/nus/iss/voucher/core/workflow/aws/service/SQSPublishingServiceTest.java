package sg.edu.nus.iss.voucher.core.workflow.aws.service;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class SQSPublishingServiceTest {

    @Mock
    private AmazonSQS amazonSQS;

    @InjectMocks
    private SQSPublishingService sqsPublishingService;

    private AuditDTO auditDTO;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        auditDTO = new AuditDTO();
        auditDTO.setRemarks("This is a test remark that may be too long for the message size.");
       
    }

    @Test
    public void testSendMessageSuccess() throws Exception {
     
        SendMessageResult sendMessageResult = new SendMessageResult();
        sendMessageResult.setMessageId("12345");
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        sqsPublishingService.sendMessage(auditDTO);

        verify(amazonSQS, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    public void testMessageTooLarge() throws Exception {
   
        String largeRemark = "This is a very large remark that exceeds the size limit of the SQS message. "
                + "It should be truncated properly in the sendMessage method to ensure the size limit is respected.";
        auditDTO.setRemarks(largeRemark);

        SendMessageResult sendMessageResult = new SendMessageResult();
        sendMessageResult.setMessageId("12345");
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        sqsPublishingService.sendMessage(auditDTO);

        verify(amazonSQS, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    
    @Test
    public void testNoTruncationNeeded() {
        String remarks = "Short remark.";
        int maxSize = 256 * 1024; 
        String currentMessage = "Current message.";

        String truncatedRemarks = sqsPublishingService.truncateMessage(remarks, maxSize, currentMessage);

        assertEquals("Remarks should not be truncated", remarks, truncatedRemarks);
        
        int totalLength = currentMessage.length() + truncatedRemarks.length();
        assertTrue("Total message length should be within maxSize", totalLength <= maxSize);
    }


    
}
