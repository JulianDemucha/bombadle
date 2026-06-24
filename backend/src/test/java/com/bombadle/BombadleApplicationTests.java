package com.bombadle;

import com.bombadle.integration.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BombadleApplicationTests extends BaseIT {

	@Test
	void contextLoads() {
	}

}
