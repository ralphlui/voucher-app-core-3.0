package sg.edu.nus.iss.voucher.core.workflow.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherDTO;
import sg.edu.nus.iss.voucher.core.workflow.dto.VoucherRequest;
import sg.edu.nus.iss.voucher.core.workflow.entity.Campaign;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.entity.Voucher;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;
import sg.edu.nus.iss.voucher.core.workflow.enums.VoucherStatus;
import sg.edu.nus.iss.voucher.core.workflow.repository.CampaignRepository;
import sg.edu.nus.iss.voucher.core.workflow.repository.StoreRepository;
import sg.edu.nus.iss.voucher.core.workflow.repository.VoucherRepository;
import sg.edu.nus.iss.voucher.core.workflow.service.impl.VoucherService;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class VoucherServiceTest {

	@MockBean
	private VoucherRepository voucherRepository;

	@Autowired
	private VoucherService voucherService;
	
	@MockBean
	private CampaignRepository campaignRepository;
	
	@MockBean
	private StoreRepository storeRepository;


	private static List<Voucher> mockVouchers = new ArrayList<>();
	private static Store store = new Store("1", "MUJI",
			"MUJI offers a wide variety of good quality items from stationery to household items and apparel.", "Test",
			"#04-36/40 Paragon Shopping Centre", "290 Orchard Rd", "", "238859", "Singapore", "Singapore", "Singapore",
			null, null, null, null, false, null, "M1", "");
	private static Campaign campaign = new Campaign("1", "new voucher 1", store, CampaignStatus.CREATED, null, 0, 0,
			null, null, 0, null, null, "US1", "US1", null, null, mockVouchers, "", false);
	private static Voucher voucher1 = new Voucher("1", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
			"U1");
	private static Voucher voucher2 = new Voucher("2", campaign, VoucherStatus.CLAIMED, LocalDateTime.now(), null,
			"U1");

	@BeforeAll
	static void setUp() {
		mockVouchers.add(voucher1);
		mockVouchers.add(voucher2);
	}
	
	@Test
	void findSingleVoucher() throws Exception {
		Mockito.when(voucherRepository.findById(voucher1.getVoucherId())).thenReturn(Optional.of(voucher1));
		VoucherDTO voucherDTO = voucherService.findByVoucherId(voucher1.getVoucherId());
		assertEquals(voucherDTO.getVoucherId(), voucher1.getVoucherId());
	}
	

	@Test
	void claimVoucher() throws Exception {
		VoucherRequest voucherRequest = new VoucherRequest();
		
		Mockito.when(voucherRepository.save(Mockito.any(Voucher.class))).thenReturn(voucher1);
		Mockito.when(campaignRepository.findById(campaign.getCampaignId())).thenReturn(Optional.of(campaign));
		
		voucherRequest.setCampaignId(voucher1.getCampaign().getCampaignId());
		voucherRequest.setClaimedBy(voucher1.getClaimedBy());
		voucher1.setClaimTime(LocalDateTime.now());
		
		VoucherDTO voucherDTO = voucherService.claimVoucher(voucherRequest);
		assertEquals(voucherDTO.getClaimedBy(), voucher1.getClaimedBy());
		assertEquals(voucherDTO.getCampaign().getCampaignId(), voucher1.getCampaign().getCampaignId());
	}
	

	@Test
	void findVoucherByCampaignIdAndUserId() throws Exception {
		Mockito.when(voucherRepository.findByCampaignAndClaimedBy(voucher1.getCampaign(), voucher1.getClaimedBy())).thenReturn(voucher1);
		VoucherDTO voucherDTO = voucherService.findVoucherByCampaignIdAndUserId(voucher1.getCampaign(), voucher1.getClaimedBy());
		assertEquals(voucherDTO.getClaimedBy(), voucher1.getClaimedBy());
		assertEquals(voucherDTO.getCampaign().getCampaignId(), voucher1.getCampaign().getCampaignId());
	}
	
	@Test
	void findAllClaimedVouchersClaimedBy() {
		List<VoucherDTO> voucherDTOList = new ArrayList<VoucherDTO>();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Voucher> mockVoucherPage = new PageImpl<>(mockVouchers, pageable, mockVouchers.size());

		Mockito.when(voucherRepository.findByClaimedByAndVoucherStatus("U1", VoucherStatus.CLAIMED,pageable))
				.thenReturn(mockVoucherPage);
		Map<Long, List<VoucherDTO>> voucherPage = voucherService.findByClaimedByAndVoucherStatus("U1",VoucherStatus.CLAIMED.toString(),
				pageable);

		for (Map.Entry<Long, List<VoucherDTO>> entry : voucherPage.entrySet()) {
			voucherDTOList = entry.getValue();

		}
		assertEquals(mockVouchers.size(), voucherDTOList.size());
		assertEquals(mockVouchers.get(0).getVoucherId(), voucherDTOList.get(0).getVoucherId());
		assertEquals(mockVouchers.get(1).getVoucherId(), voucherDTOList.get(1).getVoucherId());
	}

	@Test
	void findAllClaimedVouchersByCampaignId() {
		List<VoucherDTO> voucherDTOList = new ArrayList<VoucherDTO>();
		Pageable pageable = PageRequest.of(0, 10);
		Page<Voucher> mockVoucherPage = new PageImpl<>(mockVouchers, pageable, mockVouchers.size());

		Mockito.when(voucherRepository.findByCampaignCampaignId("1", pageable)).thenReturn(mockVoucherPage);
		Map<Long, List<VoucherDTO>> voucherPage = voucherService.findAllClaimedVouchersByCampaignId("1", pageable);
		for (Map.Entry<Long, List<VoucherDTO>> entry : voucherPage.entrySet()) {
			voucherDTOList = entry.getValue();

		}
		assertEquals(mockVouchers.size(), voucherDTOList.size());
		assertEquals(mockVouchers.get(0).getVoucherId(), voucherDTOList.get(0).getVoucherId());
		assertEquals(mockVouchers.get(1).getVoucherId(), voucherDTOList.get(1).getVoucherId());
	}
	
	
	@Test
	void consumeVoucher() throws Exception {
		Mockito.when(voucherRepository.findById(voucher1.getVoucherId())).thenReturn(Optional.of(voucher1));
		Mockito.when(voucherRepository.save(Mockito.any(Voucher.class))).thenReturn(voucher1);
		Mockito.when(storeRepository.findById(store.getStoreId())).thenReturn(Optional.of(store));
		voucher1.setConsumedTime(LocalDateTime.now());
		;
		VoucherDTO voucherDTO = voucherService.consumeVoucher(voucher1.getVoucherId());
		assertNotNull(voucherDTO.getConsumedTime());
	}

}
