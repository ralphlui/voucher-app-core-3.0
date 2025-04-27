package sg.edu.nus.iss.voucher.core.workflow.utility;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizerUtil {

	private static final PolicyFactory POLICY = new HtmlPolicyBuilder().toFactory();

    public static String sanitize(String htmlInput) {
        return POLICY.sanitize(htmlInput);
    }
}
