package com.csassist.service;

import com.csassist.service.enrichment.EnrichmentClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ServiceApplicationTests {

	@MockitoBean
	private EnrichmentClient enrichmentClient;

	@Test
	void contextLoads() {
	}

}
