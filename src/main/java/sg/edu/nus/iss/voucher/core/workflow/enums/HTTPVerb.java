package sg.edu.nus.iss.voucher.core.workflow.enums;

public enum HTTPVerb {
	GET, POST, PUT, PATCH;
	
	  public static HTTPVerb fromString(String method) {
        for (HTTPVerb verb : HTTPVerb.values()) {
            if (verb.name().equalsIgnoreCase(method)) {
                return verb;
            }
        }
        throw new IllegalArgumentException("No enum constant for method: " + method);
    }
}

