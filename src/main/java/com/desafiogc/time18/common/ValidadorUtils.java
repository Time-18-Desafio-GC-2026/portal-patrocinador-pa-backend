package com.desafiogc.time18.common;

import java.nio.charset.StandardCharsets;
import java.util.Locale;


public final class ValidadorUtils {

    private static final int[] PESOS_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO_DIGITO  = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private ValidadorUtils() {}

    
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    
    public static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    
    public static boolean cnpjValido(String cnpjLimpo) {
        if (cnpjLimpo == null || !cnpjLimpo.matches("\\d{14}")) {
            return false;
        }
        
        if (cnpjLimpo.chars().distinct().count() == 1) {
            return false;
        }
        int primeiroDigito = calcularDigito(cnpjLimpo, PESOS_PRIMEIRO_DIGITO);
        int segundoDigito  = calcularDigito(cnpjLimpo, PESOS_SEGUNDO_DIGITO);
        return primeiroDigito == (cnpjLimpo.charAt(12) - '0')
                && segundoDigito == (cnpjLimpo.charAt(13) - '0');
    }

    private static int calcularDigito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    
    public static void validarSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw ExcecaoApi.dadosInvalidos("A senha deve ter no mínimo 8 caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ExcecaoApi.dadosInvalidos("A senha deve ter no máximo 72 bytes.");
        }
    }
}