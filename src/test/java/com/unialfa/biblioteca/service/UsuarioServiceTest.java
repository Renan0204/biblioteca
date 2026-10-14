package com.unialfa.biblioteca.service;

import com.unialfa.biblioteca.model.Usuario;
import com.unialfa.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

	private final UsuarioRepository repository = mock(UsuarioRepository.class);
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	private final UsuarioService service = new UsuarioService(repository, encoder);

	@Test
	void salvarCriptografaSenhaEmTextoPuro() {
		// o repositório devolve a entidade recebida, como o JPA faz ao persistir
		when(repository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

		Usuario salvo = service.salvar(new Usuario(null, "maria", "senha123", "Maria", "USER"));

		assertNotEquals("senha123", salvo.getPassword(), "a senha não pode ser salva em texto puro");
		assertTrue(encoder.matches("senha123", salvo.getPassword()), "a senha salva deve ser um hash BCrypt da senha original");
	}
}
