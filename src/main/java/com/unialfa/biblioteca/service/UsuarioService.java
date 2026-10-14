package com.unialfa.biblioteca.service;

import com.unialfa.biblioteca.model.Usuario;
import com.unialfa.biblioteca.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByLogin(username);
    }

    public List<Usuario> listAll() {
        return repository.findAll();
    }

    public Usuario salvar(Usuario usuario) {
        if (usuario.getId() != null) {
            var usuarioDb = repository.findById(usuario.getId()).orElse(null);
            if ((usuario.getPassword() == null || usuario.getPassword().isBlank()) && usuarioDb != null) {
                usuario.setPassword(usuarioDb.getPassword());
            }
        }

        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            var pwd = usuario.getPassword();
            if (!(pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"))) {
                usuario.setPassword(passwordEncoder.encode(pwd));
            }
        }

        return repository.save(usuario);
    }

    public Usuario listar(Long id) {
        return repository.findById(id).get();
    }

    public List<Usuario> listar() {
        return repository.findAll();
    }

    public void remover(Long id) {
        repository.deleteById(id);
    }
}
