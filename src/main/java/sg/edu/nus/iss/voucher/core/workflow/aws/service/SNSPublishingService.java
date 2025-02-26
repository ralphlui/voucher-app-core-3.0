package sg.edu.nus.iss.voucher.core.workflow.aws.service;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.*;

import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SNSPublishingService {

    @Autowired
    private AmazonSNS amazonSNSClient;

    @Value("${aws.sns.feed.topic.arn}") String topicArn;
    
    private static final Logger logger = LoggerFactory.getLogger(SNSPublishingService.class);

    public void sendNotification(Campaign campaign) {
    	
    	// Creating the campaign object
        JSONObject campaignObject = new JSONObject();
        campaignObject.put("campaignId", campaign.getCampaignId());
        campaignObject.put("description", campaign.getDescription());

        // Creating the store object
        JSONObject storeObject = new JSONObject();
        storeObject.put("storeId", campaign.getStore().getStoreId());
        storeObject.put("name", campaign.getStore().getStoreName());

        // Creating the main message object
        JSONObject jsonObjectMsg = new JSONObject();
        jsonObjectMsg.put("category", campaign.getCategory());
        jsonObjectMsg.put("campaign", campaignObject);
        jsonObjectMsg.put("store", storeObject);
 
    	logger.info("Message published to SNS: " + jsonObjectMsg.toString());
        PublishRequest request = new PublishRequest().withTopicArn(topicArn.trim()).withMessage(jsonObjectMsg.toString());
        PublishResult result= amazonSNSClient.publish(request);
        logger.info("Message published successfully to SNS with Id: " + result.getMessageId());
    }
}
