package sg.edu.nus.iss.voucher.core.workflow.utility;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

public class HtmlSanitizerUtil {

	private static final PolicyFactory POLICY = Sanitizers.BLOCKS;
		 

    public static String sanitize(String htmlInput) {
        return POLICY.sanitize(htmlInput);
    }
}
