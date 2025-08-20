package com.example.legalai.controller;

import com.example.legalai.model.ContractDocument;
import com.example.legalai.service.ContractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ContractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractService contractService;

    private ContractDocument testContract;

    @BeforeEach
    void setUp() {
        testContract = new ContractDocument();
        testContract.setId("test-id");
        testContract.setTitle("Test Contract");
        testContract.setContent("Test contract content");
    }

    @Test
    void testGetAllContracts() throws Exception {
        List<ContractDocument> contracts = Arrays.asList(testContract);
        when(contractService.getAllContracts()).thenReturn(contracts);

        mockMvc.perform(get("/api/contracts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-id"))
                .andExpect(jsonPath("$[0].title").value("Test Contract"));
    }

    @Test
    void testAnalyzeContract() throws Exception {
        String analysisResult = "[{\"clause\":\"test\",\"reason\":\"risk\",\"riskLevel\":\"HIGH\"}]";
        when(contractService.analyzeContract(any(ContractDocument.class))).thenReturn(analysisResult);

        mockMvc.perform(post("/api/contracts/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testContract)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Contract"))
                .andExpect(jsonPath("$.analysisResult[0].clause").value("test"));
    }

    @Test
    void testAnalyzeContractWithInvalidData() throws Exception {
        ContractDocument invalidContract = new ContractDocument();
        // Missing required fields

        mockMvc.perform(post("/api/contracts/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidContract)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testUploadContract() throws Exception {
        mockMvc.perform(multipart("/api/contracts/upload")
                .file("file", "test content".getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/contracts")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}