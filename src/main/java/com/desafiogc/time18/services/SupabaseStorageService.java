package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.bucket}")
    private String bucket;

    @Value("${app.supabase.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String fazerUpload(MultipartFile arquivo, String pasta) {
        if (arquivo == null || arquivo.isEmpty()) return null;

        try {
            String nomeOriginal = arquivo.getOriginalFilename();
            String extensao = (nomeOriginal != null && nomeOriginal.contains("."))
                    ? nomeOriginal.substring(nomeOriginal.lastIndexOf("."))
                    : "";

            String nomeUnicoArquivo = pasta + "/" + UUID.randomUUID() + extensao;
            String urlUpload = supabaseUrl + "/storage/v1/object/" + bucket + "/" + nomeUnicoArquivo;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceKey);
            headers.setContentType(MediaType.parseMediaType(arquivo.getContentType() != null ? arquivo.getContentType() : "application/octet-stream"));

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(arquivo.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(URI.create(urlUpload), HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + nomeUnicoArquivo;
            } else {
                throw ExcecaoApi.erroInterno("Falha no Supabase: " + response.getBody());
            }
        } catch (Exception e) {
            throw ExcecaoApi.erroInterno("Erro ao processar arquivo: " + e.getMessage());
        }
    }
}