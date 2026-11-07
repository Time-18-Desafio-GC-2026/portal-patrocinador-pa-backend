package com.desafiogc.time18.security;

import com.desafiogc.time18.entities.Perfil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        extrairTokenDoHeader(request)
                .flatMap(tokenService::validarToken)
                .ifPresent(usuario -> {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken.authenticated(
                            usuario, null, mapearPermissoes(usuario.perfil())
                    );
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extrairTokenDoHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    
    private List<GrantedAuthority> mapearPermissoes(Perfil perfil) {
        return switch (perfil) {
            case SUPERADMIN -> List.of(
                    new SimpleGrantedAuthority("ROLE_SUPERADMIN"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            case ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case PATROCINADOR -> List.of(new SimpleGrantedAuthority("ROLE_PATROCINADOR"));
        };
    }
}