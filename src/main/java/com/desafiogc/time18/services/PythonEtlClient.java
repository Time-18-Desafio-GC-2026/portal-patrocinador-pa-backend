package com.desafiogc.time18.services;

import com.desafiogc.time18.dtos.PythonEtlDtos.ResultadoEtlPythonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class PythonEtlClient {

    private final RestTemplate restTemplate;

    @Value("${app.python-etl.url:https://portal-patrocinador-python-etl.onrender.com}")
    private String pythonBaseUrl;

    public PythonEtlClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(6000);
        factory.setReadTimeout(12000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Envia um arquivo .xlsx recebido no Java para o motor Pandas do Python validar e extrair KPIs.
     */
    public ResultadoEtlPythonResponse processarPlanilhaUpload(MultipartFile arquivo) {
        String url = pythonBaseUrl + "/api/v1/etl/processar-upload";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(arquivo.getBytes()) {
                @Override
                public String getFilename() {
                    return arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "planilha.xlsx";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("arquivo", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<ResultadoEtlPythonResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    ResultadoEtlPythonResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("Planilha não seguiu o layout estrito do Python ({}): ativando extração de contingência no Java.", e.getStatusCode());
            throw new IllegalStateException("Layout alternativo de planilha.", e);
        } catch (Exception e) {
            log.warn("Serviço Python indisponível em ({}): ativando extração de contingência no Java.", url);
            throw new IllegalStateException("Erro ao comunicar com o serviço Python de Dados.", e);
        }
    }
}