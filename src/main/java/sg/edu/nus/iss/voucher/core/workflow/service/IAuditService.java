package sg.edu.nus.iss.voucher.core.workflow.service;

import sg.edu.nus.iss.voucher.core.workflow.dto.AuditDTO;

public interface IAuditService {
	void sendMessage(AuditDTO autAuditDTO);
 
}
