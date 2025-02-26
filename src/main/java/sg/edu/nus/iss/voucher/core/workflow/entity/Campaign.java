package sg.edu.nus.iss.voucher.core.workflow.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import sg.edu.nus.iss.voucher.core.workflow.enums.CampaignStatus;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class Campaign {

	public Campaign() {
		super();
	}

	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String campaignId;

	@Column(nullable = false)
	private String description;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "storeId")
	private Store store;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CampaignStatus campaignStatus = CampaignStatus.CREATED;

	@Column(nullable = true)
	private String tagsJson;

	@Column(nullable = false)
	private int numberOfVouchers;

	@Column(nullable = false)
	private int numberOfLikes = 0;

	@Column(nullable = true)
	private String pin;

	@Column(nullable = true)
	private String tandc;

	@Column(nullable = false)
	private double amount;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime startDate;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime endDate;

	@Column(nullable = true)
	private String createdBy;

	@Column(nullable = true)
	private String updatedBy;

	@Column(nullable = true, columnDefinition = "datetime default now()")
	private LocalDateTime createdDate;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime updatedDate;

	@OneToMany(mappedBy = "voucherId")
	private List<Voucher> voucher;
	
	@Column(nullable = true, columnDefinition = "varchar(255)")
	private String category;
	
	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean isDeleted;
	
}