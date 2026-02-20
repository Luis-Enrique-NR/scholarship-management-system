package pe.com.security.scholarship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecurityResourceServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityResourceServerApplication.class, args);
	}

}
