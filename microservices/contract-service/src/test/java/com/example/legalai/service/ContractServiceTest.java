package com.example.legalai.service;

import com.example.legalai.model.ContractDocument;
import com.example.legalai.repository.ContractDocumentRepository;
import com.example.legalai.llm.LlamaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractDocumentRepository contractRepo;

    @Mock
    private LlamaService llamaService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ContractService contractService;

    private ContractDocument testContract;

    @BeforeEach
    void setUp() {
        testContract = new ContractDocument();
        testContract.setId("test-id");
        testContract.setTitle("Test Contract");
        testContract.setContent("This is a test contract content");
    }

    @Test
    void testAnalyzeContract() {
        String expectedResponse = "[{\"clause\":\"test\",\"reason\":\"test reason\",\"riskLevel\":\"LOW\"}]";
        when(llamaService.sendPrompt(anyString())).thenReturn(expectedResponse);
        when(contractRepo.save(any(ContractDocument.class))).thenReturn(testContract);

        String result = contractService.analyzeContract(testContract);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(contractRepo, times(1)).save(testContract);
        verify(llamaService, times(1)).sendPrompt(anyString());
    }

    @Test
    void testGetAllContracts() {
        List<ContractDocument> expectedContracts = Arrays.asList(testContract);
        when(contractRepo.findAll()).thenReturn(expectedContracts);

        List<ContractDocument> result = contractService.getAllContracts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testContract.getId(), result.get(0).getId());
        verify(contractRepo, times(1)).findAll();
    }

    @Test
    void testCachingBehavior() {
        List<ContractDocument> expectedContracts = Arrays.asList(testContract);
        when(contractRepo.findAll()).thenReturn(expectedContracts);

        // First call should hit the database
        contractService.getAllContracts();
        
        // Second call should use cache (verify findAll is called only once)
        contractService.getAllContracts();
        
        verify(contractRepo, times(1)).findAll();
    }

    @Test
    void testAnalyzeContractWithNullContent() {
        ContractDocument nullContentContract = new ContractDocument();
        nullContentContract.setTitle("Null Content Contract");
        nullContentContract.setContent(null);

        when(contractRepo.save(any(ContractDocument.class))).thenReturn(nullContentContract);
        when(llamaService.sendPrompt(anyString())).thenReturn("[]");

        String result = contractService.analyzeContract(nullContentContract);

        assertNotNull(result);
        assertEquals("[]", result);
    }

    @Test
    void testAnalyzeContractWithLlamaServiceError() {
        when(contractRepo.save(any(ContractDocument.class))).thenReturn(testContract);
        when(llamaService.sendPrompt(anyString())).thenThrow(new RuntimeException("LLM Service Error"));

        assertThrows(RuntimeException.class, () -> {
            contractService.analyzeContract(testContract);
        });
    }
}