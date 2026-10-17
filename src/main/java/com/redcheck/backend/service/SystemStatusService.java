package com.redcheck.backend.service;

import com.redcheck.backend.dto.response.SystemStatusResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class SystemStatusService {

    // Short on purpose: the dashboard waits for this, and a hung engine
    // should read as "down", not freeze the page.
    private static final Duration AI_PING_TIMEOUT = Duration.ofSeconds(3);
    private static final long MB = 1024 * 1024;

    // Absent when the jar wasn't built with spring-boot-maven-plugin's
    // build-info goal (e.g. running straight from an IDE).
    private final ObjectProvider<BuildProperties> buildProperties;
    private final String aiEngineUrl;
    private final String gitSha;

    public SystemStatusService(ObjectProvider<BuildProperties> buildProperties,
                               @Value("${ai.engine.url}") String aiEngineUrl,
                               @Value("${app.git-sha:unknown}") String gitSha) {
        this.buildProperties = buildProperties;
        this.aiEngineUrl = aiEngineUrl;
        this.gitSha = gitSha;
    }

    public SystemStatusResponseDTO getStatus() {
        BuildProperties build = buildProperties.getIfAvailable();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Runtime jvm = Runtime.getRuntime();
        Long aiLatencyMs = pingAiEngine();

        return SystemStatusResponseDTO.builder()
                .version(build != null ? build.getVersion() : null)
                .buildTime(build != null ? build.getTime() : null)
                .gitSha(gitSha)
                .startedAt(Instant.ofEpochMilli(runtime.getStartTime()))
                .uptimeSeconds(runtime.getUptime() / 1000)
                .javaVersion(System.getProperty("java.version"))
                .memoryUsedMb((jvm.totalMemory() - jvm.freeMemory()) / MB)
                .memoryMaxMb(jvm.maxMemory() / MB)
                .aiEngineReachable(aiLatencyMs != null)
                .aiEngineLatencyMs(aiLatencyMs)
                .build();
    }

    // GET {engine}/health (smartcheck-ai-engine's heartbeat). Returns the
    // round-trip in ms, or null if it's unreachable, times out or errors.
    private Long pingAiEngine() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(AI_PING_TIMEOUT);
        requestFactory.setReadTimeout(AI_PING_TIMEOUT);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        long start = System.currentTimeMillis();
        try {
            restTemplate.getForEntity(aiEngineUrl + "/health", String.class);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            log.warn("SmartCheck AI engine health check failed: {}", e.getMessage());
            return null;
        }
    }
}
