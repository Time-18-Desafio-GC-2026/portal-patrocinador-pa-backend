package com.desafiogc.time18.services;

import com.desafiogc.time18.dtos.PythonEtlDtos.ResultadoEtlPythonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class PythonEtlClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.python-etl.url:http://localhost:8000}")
    private String pythonBaseUrl;

    /**
     * Envia um arquivo .xlsx recebido no Java diretamente para o motor Pandas do Python validar e extrair KPIs.
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
            log.error("Planilha reprovada pelo serviço Python ETL: {}", e.getResponseBodyAsString());
            throw new IllegalArgumentException("A planilha enviada contém inconsistências apontadas pelo serviço de dados: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Não foi possível processar a planilha no serviço Python ({}): {}", url, e.getMessage());
            throw new IllegalStateException("Erro ao comunicar com o serviço Python de Dados.", e);
        }
    }
}