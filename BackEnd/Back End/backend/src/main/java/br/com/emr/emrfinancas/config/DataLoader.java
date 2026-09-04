package br.com.emr.emrfinancas.config;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataLoader {
    @Bean
    CommandLineRunner seedUsuario(UsuarioRepository usuarioRepository) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario usuario = new Usuario();
                usuario.setNome("Administrador EMR");
                usuario.setEmail("admin@emrfinancas.com");
                usuario.setSenha("123456");
                usuarioRepository.save(usuario);
            }
        };
    }
}
