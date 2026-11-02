package com.desafiogc.time18.security;

import com.desafiogc.time18.entities.Perfil;
import com.desafiogc.time18.entities.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável por emitir e validar Tokens JWT (algoritmo HMAC-SHA256).
 * Princípio da Responsabilidade Única (S do SOLID): só lida com criptografia de tokens.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expiracaoHoras;

    public TokenService(
            @Value("${app.jwt.secret}") String segredoBase64,
            @Value("${app.jwt.expiracao-horas:8}") long expiracaoHoras) {

        byte[] bytesSegredo = Base64.getDecoder().decode(segredoBase64.trim());
        SecretKey chaveSecreta = new SecretKeySpec(bytesSegredo, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chaveSecreta));
        this.decoder = NimbusJwtDecoder.withSecretKey(chaveSecreta).macAlgorithm(MacAlgorithm.HS256).build();
        this.expiracaoHoras = expiracaoHoras;
    }

    /**
     * Cria um Token JWT assinado contendo ID, e-mail, perfil e marcaId do usuário.
     */
    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracaoHoras, ChronoUnit.HOURS))
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome() != null ? usuario.getNome() : usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .claim("precisaTrocarSenha", usuario.isPrecisaTrocarSenha());

        // Se for um patrocinador, crava o ID da marca dele dentro do token JWT!
        if (usuario.getMarca() != null) {
            claims.claim("marcaId", usuario.getMarca().getId().toString());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    /**
     * Lê um Token JWT recebido no header Authorization e devolve o UsuarioAutenticado (se o token for válido).
     */
    public Optional<UsuarioAutenticado> validarToken(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            String marcaIdStr = jwt.getClaimAsString("marcaId");

            return Optional.of(new UsuarioAutenticado(
                    UUID.fromString(jwt.getSubject()),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("nome"),
                    Perfil.valueOf(jwt.getClaimAsString("perfil")),
                    marcaIdStr == null ? null : UUID.fromString(marcaIdStr),
                    Boolean.TRUE.equals(jwt.getClaimAsBoolean("precisaTrocarSenha"))
            ));
        } catch (JwtException | IllegalArgumentException e) {
            // Token expirado, adulterado ou malformado
            return Optional.empty();
        }
    }
}

