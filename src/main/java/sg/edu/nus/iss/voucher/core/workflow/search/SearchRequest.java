package sg.edu.nus.iss.voucher.core.workflow.search;

import jakarta.validation.constraints.Size;

public class SearchRequest {
    
    @Size(min = 3, max = 10, message = "Query must be between 3 and 10 characters")
    private String query;
    
    private int page = 0;

    private int size = 50;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	} 

}
