package pe.com.security.scholarship;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pe.com.security.scholarship.config.TestSecurityConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SecurityResourceServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
