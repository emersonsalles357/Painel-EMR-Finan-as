package br.com.emr.emrfinancas.config;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "app.dev.seed-user.enabled", havingValue = "true")
public class DataLoader {
    @Bean
    CommandLineRunner seedUsuario(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${DEV_SEED_USER_NAME}") String nome,
            @Value("${DEV_SEED_USER_EMAIL}") String email,
            @Value("${DEV_SEED_USER_PASSWORD}") String senha) {
        return args -> {
            if (usuarioRepository.count() == 0) {
                Usuario usuario = new Usuario();
                usuario.setNome(nome);
                usuario.setEmail(email);
                usuario.setSenha(passwordEncoder.encode(senha));
                usuarioRepository.save(usuario);
            }
        };
    }
}
