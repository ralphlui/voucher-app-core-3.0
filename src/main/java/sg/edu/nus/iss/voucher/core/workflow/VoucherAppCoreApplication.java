package sg.edu.nus.iss.voucher.core.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VoucherAppCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoucherAppCoreApplication.class, args);
	}

}
