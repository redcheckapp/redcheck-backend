package com.redcheck.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OllamaService {

    public String askLlama(String prompt){
        RestTemplate restTemplate = new RestTemplate();
        // Local URL where Ollama is listening
        String url = "http://localhost:11434/api/generate";

        // Petition body (JSON)
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama3.2");
        request.put("prompt", prompt);
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try{
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if(response.getBody() != null)
                return (String) response.getBody().get("response");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: No se ha podido contactar con SmartCheck AI";
        }

        return "Respuesta vacía.";
    }
}
