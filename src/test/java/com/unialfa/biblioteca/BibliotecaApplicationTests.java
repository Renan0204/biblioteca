package com.unialfa.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// chave falsa, válida só nos testes: o build não depende do segredo real (JWT_SECRET)
@SpringBootTest(properties = "api.security.token.secret=chave-falsa-apenas-para-testes-0123456789abcdef")
class BibliotecaApplicationTests {

	@Test
	void contextLoads() {
	}

}
