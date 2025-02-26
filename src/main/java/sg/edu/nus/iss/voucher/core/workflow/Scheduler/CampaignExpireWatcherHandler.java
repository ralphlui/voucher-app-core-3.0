package sg.edu.nus.iss.voucher.core.workflow.Scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import sg.edu.nus.iss.voucher.core.workflow.service.impl.CampaignService;

@Component
public class CampaignExpireWatcherHandler {

	private static final Logger logger = LoggerFactory.getLogger(CampaignExpireWatcherHandler.class);

	@Autowired
	private CampaignService campaignService;


	@Scheduled(fixedDelay = 300000)
	public void run() {

		logger.info("Star Run CampaignExpireWatcherHandler...");

		try {
			campaignService.expired();

		} catch (Exception ex) {
			logger.error("CampaignExpireWatcherHandler exception... {}", ex.toString());
			ex.printStackTrace();
		}
	}

}
