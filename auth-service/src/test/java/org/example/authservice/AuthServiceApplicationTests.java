package org.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.security.oauth2.enabled=false")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
