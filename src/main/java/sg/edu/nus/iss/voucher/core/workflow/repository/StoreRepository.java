package sg.edu.nus.iss.voucher.core.workflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.voucher.core.workflow.entity.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, String> {

	Page<Store> findByIsDeletedFalse(Pageable pageable);
	
	Store findByStoreName(String storeName);
	
	@Query("SELECT s FROM Store s WHERE s.storeId = ?1 AND s.isDeleted = ?2")
	Optional<Store> findByStoreIdAndStatus(String storeId, Boolean isDeleted);
	
	@Query("SELECT s FROM Store s WHERE s.createdBy = :createdBy AND s.isDeleted = :isDeleted")
	Page<Store> findActiveStoreListByUserId(@Param("createdBy") String createdBy, @Param("isDeleted") boolean isDeleted, Pageable pageable);
	
	@Query("SELECT s FROM Store s WHERE s.storeName LIKE %?1% AND s.isDeleted = ?2")
	Page<Store> searchStoresByKeyword(String storeName, boolean isDeleted, Pageable pageable);

	
}
