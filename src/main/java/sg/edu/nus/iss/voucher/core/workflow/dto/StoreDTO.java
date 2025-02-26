package sg.edu.nus.iss.voucher.core.workflow.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreDTO {

    private String storeId;
	private String storeName;
	private String description;
	private String image;
	private String tagsJson;
	private String address;
	private String address1;
	private String address2;
	private String address3;
	private String postalCode;
	private String city;
	private String state;
	private String country;
	private String contactNumber;
	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	private String createdBy;
	private String updatedBy;

    public StoreDTO() {
    }
    
}
