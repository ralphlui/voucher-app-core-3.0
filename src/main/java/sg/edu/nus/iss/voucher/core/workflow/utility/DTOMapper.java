package sg.edu.nus.iss.voucher.core.workflow.utility;

import sg.edu.nus.iss.voucher.core.workflow.dto.*;
import sg.edu.nus.iss.voucher.core.workflow.entity.*;

public class DTOMapper {

	public static StoreDTO mapStoreToResult(Store store) {
		StoreDTO storeDTO = new StoreDTO();
		storeDTO.setStoreId(store.getStoreId());
		storeDTO.setStoreName(store.getStoreName());
		storeDTO.setDescription(GeneralUtility.makeNotNull(store.getDescription()));

		String address = GeneralUtility.makeNotNull(store.getAddress1()).trim();
		address += address.isEmpty() ? "" : ", " + GeneralUtility.makeNotNull(store.getAddress2()).trim();
		address += address.isEmpty() ? "" : ", " + GeneralUtility.makeNotNull(store.getAddress3()).trim();
		address += address.isEmpty() ? "" : ", " + GeneralUtility.makeNotNull(store.getPostalCode());

		storeDTO.setAddress(GeneralUtility.makeNotNull(address));
		storeDTO.setAddress1(store.getAddress1());
		storeDTO.setAddress2(store.getAddress2());
		storeDTO.setAddress3(store.getAddress3());
		storeDTO.setCity(GeneralUtility.makeNotNull(store.getCity()));
		storeDTO.setState(GeneralUtility.makeNotNull(store.getState()));
		storeDTO.setCountry(GeneralUtility.makeNotNull(store.getCountry()));
		storeDTO.setContactNumber(GeneralUtility.makeNotNull(store.getContactNumber()));
		storeDTO.setPostalCode(GeneralUtility.makeNotNull(store.getPostalCode()));
		storeDTO.setImage(GeneralUtility.makeNotNull(store.getImage()));

		storeDTO.setCreatedDate(store.getCreatedDate());
		storeDTO.setUpdatedDate(store.getUpdatedDate());
		storeDTO.setCreatedBy(store.getCreatedBy());
		storeDTO.setUpdatedBy(store.getUpdatedBy());
		return storeDTO;
	}

	public static StoreDTO toStoreDTO(Store store) {
		StoreDTO storeDTO = new StoreDTO();
		storeDTO.setStoreId(store.getStoreId());
		storeDTO.setStoreName(store.getStoreName());
		storeDTO.setDescription(store.getDescription());
		storeDTO.setImage(store.getImage());
		storeDTO.setTagsJson(store.getTagsJson());
		storeDTO.setAddress1(store.getAddress1());
		storeDTO.setAddress2(store.getAddress2());
		storeDTO.setAddress3(store.getAddress3());
		storeDTO.setPostalCode(store.getPostalCode());
		storeDTO.setCity(store.getCity());
		storeDTO.setState(store.getState());
		storeDTO.setCountry(store.getCountry());
		storeDTO.setContactNumber(store.getContactNumber());
		storeDTO.setCreatedDate(store.getCreatedDate());
		storeDTO.setUpdatedDate(store.getUpdatedDate());
		storeDTO.setCreatedBy(store.getCreatedBy());
		storeDTO.setUpdatedBy(store.getUpdatedBy());
		return storeDTO;
	}

	public static CampaignDTO toCampaignDTO(Campaign campaign) {
		CampaignDTO campaignDTO = new CampaignDTO();
		campaignDTO.setCampaignId(campaign.getCampaignId());
		campaignDTO.setDescription(campaign.getDescription());
		campaignDTO.setStore(toStoreDTO(campaign.getStore()));
		campaignDTO.setCampaignStatus(campaign.getCampaignStatus());
		campaignDTO.setTagsJson(campaign.getTagsJson());
		campaignDTO.setNumberOfVouchers(campaign.getNumberOfVouchers());
		campaignDTO.setNumberOfLikes(campaign.getNumberOfLikes());
		campaignDTO.setPin(campaign.getPin());
		campaignDTO.setTandc(campaign.getTandc());
		campaignDTO.setAmount(campaign.getAmount());
		campaignDTO.setStartDate(campaign.getStartDate());
		campaignDTO.setEndDate(campaign.getEndDate());
		campaignDTO.setCreatedBy(campaign.getCreatedBy());

		campaignDTO.setCreatedDate(campaign.getCreatedDate());
		campaignDTO.setUpdatedBy(campaign.getUpdatedBy());
		campaignDTO.setUpdatedDate(campaign.getUpdatedDate());
		campaignDTO.setCategory(campaign.getCategory());

		if (campaign.getVoucher() != null) {
			campaignDTO.setNumberOfClaimedVouchers(campaign.getVoucher().size());
		}
		return campaignDTO;
	}
	
	public static VoucherDTO toVoucherDTO(Voucher voucher) {
		VoucherDTO voucherDTO = new VoucherDTO();
		voucherDTO.setVoucherId(voucher.getVoucherId());
		voucherDTO.setCampaign(toCampaignDTO(voucher.getCampaign()));
		voucherDTO.setVoucherStatus(voucher.getVoucherStatus());
		voucherDTO.setClaimTime(voucher.getClaimTime());
		voucherDTO.setConsumedTime(voucher.getConsumedTime());
		voucherDTO.setClaimedBy(voucher.getClaimedBy());
		voucherDTO.setAmount(voucherDTO.getCampaign().getAmount());
		return voucherDTO;
	}
}
