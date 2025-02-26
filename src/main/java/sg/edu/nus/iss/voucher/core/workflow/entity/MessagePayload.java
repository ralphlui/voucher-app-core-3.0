package sg.edu.nus.iss.voucher.core.workflow.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessagePayload {
	private String campaign;
	private String store;
	private String preference;

}
