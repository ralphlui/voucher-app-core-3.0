package sg.edu.nus.iss.voucher.core.workflow.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;

import sg.edu.nus.iss.voucher.core.workflow.configuration.AWSConfig;
import sg.edu.nus.iss.voucher.core.workflow.dto.StoreDTO;
import sg.edu.nus.iss.voucher.core.workflow.entity.Store;
import sg.edu.nus.iss.voucher.core.workflow.exception.StoreNotFoundException;
import sg.edu.nus.iss.voucher.core.workflow.repository.StoreRepository;
import sg.edu.nus.iss.voucher.core.workflow.service.IStoreService;
import sg.edu.nus.iss.voucher.core.workflow.utility.DTOMapper;
import sg.edu.nus.iss.voucher.core.workflow.utility.GeneralUtility;
import sg.edu.nus.iss.voucher.core.workflow.utility.ImageUploadToS3;

@Service
public class StoreService implements IStoreService {

	private static final Logger logger = LoggerFactory.getLogger(StoreService.class);

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private AmazonS3 s3Client;

	@Autowired
	private AWSConfig securityConfig;
	

	@Override
	public Map<Long, List<StoreDTO>> getAllActiveStoreList(String storeSearchKeyword, Pageable pageable) {
		try {
			
			Page<Store> storePages = storeSearchKeyword.isEmpty() ?  storeRepository.findByIsDeletedFalse(pageable) : storeRepository.searchStoresByKeyword(storeSearchKeyword, false, pageable);
			long totalRecord = storePages.getTotalElements();
			List<StoreDTO> storeDTOList = new ArrayList<>();
			if (totalRecord > 0) {

				for (Store store : storePages.getContent()) {
					StoreDTO storeDTO = DTOMapper.mapStoreToResult(store);
					storeDTOList.add(storeDTO);
				}
			}
			logger.info("Total record in getAllActiveStoreList1 " + totalRecord);
			Map<Long, List<StoreDTO>> result = new HashMap<>();
			result.put(totalRecord, storeDTOList);
			return result;

		} catch (Exception ex) {
			logger.error("getAllActiveStoreList exception... {}", ex.toString());
			throw ex;
		}

	}

	@Override
	public StoreDTO createStore(Store store, MultipartFile uploadFile) throws Exception {

		try {
			store = this.uploadImage(store, uploadFile);
			store.setCreatedDate(LocalDateTime.now());
			logger.info("Saving store...");
			Store createdStore = storeRepository.save(store);
			logger.info("Saved successfully...{}", createdStore.getStoreId());
			StoreDTO storeDTO = DTOMapper.toStoreDTO(createdStore);
			if (storeDTO == null) {
				throw new Exception("Store creation failed: Unable to create a new store at this time.");
			}
			return storeDTO;

		} catch (Exception e) {
			logger.error("Error occurred while user creating, " + e.toString());
			e.printStackTrace();
			throw e;

		}

	}
	

	@Override
	public StoreDTO findByStoreName(String storename) {
		StoreDTO storeDTO = new StoreDTO();
		try {
			Store store = storeRepository.findByStoreName(storename);
		    storeDTO = DTOMapper.toStoreDTO(store);
		} catch (Exception ex) {
			logger.error("findByStoreId exception... {}", ex.toString());
		}
		return storeDTO;

	}
	
	@Override
	public StoreDTO findByStoreId(String storeId) {
		try {
			Optional<Store> store = storeRepository.findByStoreIdAndStatus(storeId, false);
			if (store.isPresent()) {
				StoreDTO storeDTO = DTOMapper.toStoreDTO(store.get());
				return storeDTO;
			}
			throw new StoreNotFoundException("Unable to find active store with this store id:" + storeId);
		} catch (Exception ex) {
			logger.error("findByStoreId exception... {}", ex.toString());
			throw ex;
		}
	}
	

	@Override
	public Store uploadImage(Store store, MultipartFile uploadFile) {
		try {
			if (!GeneralUtility.makeNotNull(uploadFile).equals("")) {
				logger.info("create store: " + store.getStoreName() + "::" + uploadFile.getOriginalFilename());
				if (securityConfig != null) {

					boolean isImageUploaded = ImageUploadToS3.checkImageExistBeforeUpload(s3Client, uploadFile,
							securityConfig, securityConfig.getS3ImagePublic().trim());
					if (isImageUploaded) {
						String imageUrl = securityConfig.getS3ImageUrlPrefix().trim() + "/"
								+ securityConfig.getS3ImagePublic().trim() + uploadFile.getOriginalFilename().trim();
						store.setImage(imageUrl);
					}
				}

			}
		} catch (Exception e) {
			logger.error("Error occurred while uploading Image, " + e.toString());
			e.printStackTrace();

		}
		return store;
	}

	@Override
	public Map<Long, List<StoreDTO>> findActiveStoreListByUserId(String createdBy, boolean isDeleted,
			Pageable pageable) {
		try {
			Page<Store> storePages = storeRepository.findActiveStoreListByUserId(createdBy, isDeleted, pageable);
			long totalRecord = storePages.getTotalElements();
			Map<Long, List<StoreDTO>> result = new HashMap<>();
			List<StoreDTO> storeDTOList = new ArrayList<>();

			if (totalRecord > 0) {
				for (Store store : storePages.getContent()) {
					StoreDTO storeDTO = DTOMapper.mapStoreToResult(store);
					storeDTOList.add(storeDTO);
				}
			}

			logger.info("Total record in findActiveStoreListByUserId " + totalRecord);
			result.put(totalRecord, storeDTOList);
			return result;
		} catch (Exception ex) {
			logger.error("findByIsDeletedFalse exception... {}", ex.toString());
			throw ex;

		}

	}
	
	@Override
	public StoreDTO updateStore(Store store, MultipartFile uploadFile) throws Exception {
		StoreDTO storeDTO = new StoreDTO();
		try {

			Optional<Store> dbStore = storeRepository.findById(store.getStoreId());
			store = this.uploadImage(store, uploadFile);
			dbStore.get().setDescription(GeneralUtility.makeNotNull(store.getDescription()));
			dbStore.get().setAddress1(GeneralUtility.makeNotNull(store.getAddress1()));
			dbStore.get().setAddress2(GeneralUtility.makeNotNull(store.getAddress2()));
			dbStore.get().setAddress3(GeneralUtility.makeNotNull(store.getAddress3()));
			dbStore.get().setCity(GeneralUtility.makeNotNull(store.getCity()));
			dbStore.get().setState(GeneralUtility.makeNotNull(store.getState()));
			dbStore.get().setCountry(GeneralUtility.makeNotNull(store.getCountry()));
			dbStore.get().setContactNumber(GeneralUtility.makeNotNull(store.getContactNumber()));
			dbStore.get().setPostalCode(GeneralUtility.makeNotNull(store.getPostalCode()));
			dbStore.get().setDeleted(store.isDeleted());
			dbStore.get().setImage(store.getImage());
			dbStore.get().setUpdatedBy(store.getUpdatedBy());
			dbStore.get().setUpdatedDate(LocalDateTime.now());

			logger.info("Updating store...");
			Store updatedStore = storeRepository.save(dbStore.get());
			if (updatedStore == null) {
				throw new Exception("Store update failed: Unable to apply changes to the store:" + store.getStoreName());
			}
			logger.info("Updated successfully...{}", updatedStore.getStoreId());
			storeDTO = DTOMapper.toStoreDTO(updatedStore);
			return storeDTO;

		} catch (Exception ex) {
			logger.error("Error occurred while user creating, " + ex.toString());
			ex.printStackTrace();
			throw ex;
		}

	}

}
