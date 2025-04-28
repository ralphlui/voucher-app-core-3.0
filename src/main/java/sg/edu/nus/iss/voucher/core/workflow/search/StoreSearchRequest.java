package sg.edu.nus.iss.voucher.core.workflow.search;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreSearchRequest {
    
	@Size(min = 1, max = 50, message = "Query must be between 1 and 50 characters")
    private String query;
    
	@Min(0)
    private int page = 0;

    @Min(1)
    private int size = 50;
	

}
