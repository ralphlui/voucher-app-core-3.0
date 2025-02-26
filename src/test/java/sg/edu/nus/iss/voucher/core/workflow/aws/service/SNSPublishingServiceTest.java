package sg.edu.nus.iss.voucher.core.workflow.aws.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import sg.edu.nus.iss.voucher.core.workflow.entity.*;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class SNSPublishingServiceTest {

	@Mock
	private AmazonSNS amazonSNSClient;

	@InjectMocks
	private SNSPublishingService messagePublishService;

	@Value("${aws.sns.feed.topic.arn}")
	String topicArn;

	private Campaign campaign;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		messagePublishService.topicArn = topicArn;

		Store store = new Store();
		store.setStoreId("123");
		store.setStoreName("Test Store");

		campaign = new Campaign();
		campaign.setCampaignId("112");
		campaign.setDescription("Test Campaign Description");
		campaign.setCategory("Test Category");
		campaign.setStore(store);
	}

	@Test
	public void testSendNotification() {
		// Mock the response of the SNS publish
		PublishResult publishResult = new PublishResult();
		publishResult.setMessageId("12345");
		when(amazonSNSClient.publish(any(PublishRequest.class))).thenReturn(publishResult);

		messagePublishService.sendNotification(campaign);

		verify(amazonSNSClient, times(1)).publish(any(PublishRequest.class));

	}
}
