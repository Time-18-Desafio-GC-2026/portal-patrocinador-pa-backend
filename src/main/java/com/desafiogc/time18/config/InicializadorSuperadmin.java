package com.desafiogc.time18.config;

import com.desafiogc.time18.common.ValidadorUtils;
import com.desafiogc.time18.entities.Perfil;
import com.desafiogc.time18.entities.Usuario;
import com.desafiogc.time18.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Garante que o SUPERADMIN da RBS esteja cadastrado no banco de dados assim que o servidor liga.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicializadorSuperadmin implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email}")
    private String emailSuperadmin;

    @Value("${app.superadmin.nome}")
    private String nomeSuperadmin;

    @Value("${app.superadmin.senha}")
    private String senhaSuperadmin;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = ValidadorUtils.normalizarEmail(emailSuperadmin);

        Usuario superadmin = usuarioRepository.findFirstByPerfil(Perfil.SUPERADMIN).orElse(null);

        if (superadmin == null) {
            usuarioRepository.save(Usuario.builder()
                    .email(email)
                    .nome(nomeSuperadmin)
                    .senhaHash(passwordEncoder.encode(senhaSuperadmin))
                    .perfil(Perfil.SUPERADMIN)
                    .ativo(true)
                    .precisaTrocarSenha(false)
                    .primeiroAcessoEm(Instant.now())
                    .build());
            log.info("✅ Conta SUPERADMIN da RBS criada no Supabase: {}", email);
            return;
        }

        // Mantém os dados do superadmin sincronizados caso mudem no application.properties
        superadmin.setEmail(email);
        superadmin.setNome(nomeSuperadmin);
        superadmin.setAtivo(true);
        if (!passwordEncoder.matches(senhaSuperadmin, superadmin.getSenhaHash())) {
            superadmin.setSenhaHash(passwordEncoder.encode(senhaSuperadmin));
            log.info("🔑 Senha do SUPERADMIN atualizada com sucesso.");
        }
    }
}