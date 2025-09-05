package com.example.legalai.security;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import com.example.legalai.dto.ContractAnalysisRequest;
import com.example.legalai.dto.ContractUploadResponse;
import com.example.legalai.model.Contract;
import com.example.legalai.service.ContractService;
import com.example.legalai.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Fuzzing Test Suite for Security Testing
 * Uses Jazzer for Java fuzzing to detect security vulnerabilities
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Fuzzing Test Suite")
public class FuzzingTestSuite {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractRepository contractRepository;

    private static final int MAX_ITERATIONS = 10000;
    private static final long TIMEOUT_MS = 5000;

    @BeforeEach
    void setUp() {
        // Clear any existing test data
        contractRepository.deleteAll();
    }

    /**
     * Fuzz test for contract upload functionality
     * Tests with random file content and metadata
     */
    @FuzzTest(maxDuration = "2m")
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void fuzzContractUpload(FuzzedDataProvider data) {
        // Generate random file content
        byte[] fileContent = data.consumeBytes(data.consumeInt(1, 10000));
        String fileName = data.consumeString(100);
        String contentType = data.consumeString(50);

        // Sanitize filename to prevent path traversal
        fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (fileName.isEmpty()) {
            fileName = "test.pdf";
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            contentType,
            fileContent
        );

        try {
            // Attempt to upload the fuzzed file
            ContractUploadResponse response = contractService.uploadContract(
                file,
                "testuser",
                data.consumeString(200) // Random description
            );

            // Verify no SQL injection or XSS in response
            assertNotNull(response);
            assertNoInjectionPatterns(response.getContractId());
            assertNoInjectionPatterns(response.getMessage());

            // Verify file size limits are enforced
            assertTrue(response.getFileSize() <= 10_000_000, "File size limit exceeded");

        } catch (Exception e) {
            // Ensure graceful error handling
            assertTrue(e.getMessage() != null, "Error message should not be null");
            assertFalse(e.getMessage().contains("java.sql"), "SQL details should not leak");
            assertFalse(e.getMessage().contains("org.springframework"), "Framework details should not leak");
        }
    }

    /**
     * Fuzz test for contract analysis with random input
     * Tests SQL injection and XSS prevention
     */
    @FuzzTest(maxDuration = "2m")
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void fuzzContractAnalysis(FuzzedDataProvider data) {
        // Create a contract with fuzzed data
        Contract contract = new Contract();
        contract.setId(UUID.randomUUID().toString());
        contract.setTitle(data.consumeString(200));
        contract.setContent(data.consumeString(5000));
        contract.setOwnerId("testuser");
        
        contractRepository.save(contract);

        // Create analysis request with fuzzed parameters
        ContractAnalysisRequest request = new ContractAnalysisRequest();
        request.setContractId(contract.getId());
        request.setAnalysisType(data.consumeString(50));
        request.setParameters(data.consumeRemainingAsString());

        try {
            var response = contractService.analyzeContract(request);
            
            // Verify response integrity
            assertNotNull(response);
            assertTrue(response.getRiskScore() >= 0 && response.getRiskScore() <= 100);
            
            // Check for injection patterns in output
            assertNoInjectionPatterns(response.getAnalysisText());
            assertNoInjectionPatterns(response.getRecommendations());

        } catch (Exception e) {
            // Verify secure error handling
            assertSecureErrorHandling(e);
        }
    }

    /**
     * Fuzz test for search functionality
     * Tests for SQL injection in search queries
     */
    @FuzzTest(maxDuration = "1m")
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void fuzzSearchContracts(FuzzedDataProvider data) {
        // Generate random search queries
        String searchTerm = data.consumeString(500);
        int limit = data.consumeInt(1, 100);
        
        try {
            var results = contractService.searchContracts(searchTerm, limit);
            
            // Verify results are properly bounded
            assertNotNull(results);
            assertTrue(results.size() <= limit, "Result limit exceeded");
            
            // Verify no data leakage
            results.forEach(contract -> {
                assertNotNull(contract.getId());
                assertFalse(contract.getContent().contains("password"));
                assertFalse(contract.getContent().contains("secret"));
            });

        } catch (Exception e) {
            assertSecureErrorHandling(e);
        }
    }

    /**
     * Fuzz test for authentication bypass attempts
     * Tests various injection patterns in auth headers
     */
    @FuzzTest(maxDuration = "1m")
    public void fuzzAuthenticationBypass(FuzzedDataProvider data) {
        String username = data.consumeString(100);
        String password = data.consumeString(100);
        String token = data.consumeString(500);
        
        // Common injection patterns to test
        String[] injectionPatterns = {
            "' OR '1'='1",
            "admin' --",
            "<script>alert('xss')</script>",
            "../../../etc/passwd",
            "'; DROP TABLE users; --"
        };

        for (String pattern : injectionPatterns) {
            try {
                // Attempt authentication with injection patterns
                contractService.validateUserAccess(username + pattern, token);
                
                // If no exception, verify proper validation
                fail("Injection pattern should have been rejected: " + pattern);
                
            } catch (Exception e) {
                // Verify injection was properly rejected
                assertTrue(e.getMessage().contains("Invalid") || 
                          e.getMessage().contains("Unauthorized"));
                assertSecureErrorHandling(e);
            }
        }
    }

    /**
     * Fuzz test for file path traversal prevention
     */
    @FuzzTest(maxDuration = "1m")
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void fuzzPathTraversal(FuzzedDataProvider data) {
        String[] traversalPatterns = {
            "../",
            "..\\",
            "%2e%2e%2f",
            "..%2F",
            "..%5C"
        };

        for (String pattern : traversalPatterns) {
            String maliciousPath = data.consumeString(50) + pattern + data.consumeString(50);
            
            try {
                contractService.getContractFile(maliciousPath);
                fail("Path traversal should have been blocked: " + maliciousPath);
                
            } catch (Exception e) {
                // Verify path traversal was blocked
                assertFalse(e.getMessage().contains("/etc/"));
                assertFalse(e.getMessage().contains("C:\\Windows"));
                assertSecureErrorHandling(e);
            }
        }
    }

    /**
     * Fuzz test for rate limiting effectiveness
     */
    @FuzzTest(maxDuration = "30s")
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void fuzzRateLimiting(FuzzedDataProvider data) {
        int requestCount = data.consumeInt(50, 200);
        int successCount = 0;
        
        for (int i = 0; i < requestCount; i++) {
            try {
                contractService.searchContracts(
                    data.consumeString(10),
                    10
                );
                successCount++;
                
                // Small delay to simulate real requests
                Thread.sleep(10);
                
            } catch (Exception e) {
                if (e.getMessage().contains("Rate limit") || 
                    e.getMessage().contains("Too many requests")) {
                    // Rate limiting working correctly
                    break;
                }
            }
        }
        
        // Verify rate limiting kicked in
        assertTrue(successCount < requestCount, 
                  "Rate limiting should have blocked some requests");
        assertTrue(successCount <= 50, 
                  "Too many requests succeeded before rate limiting");
    }

    /**
     * Property-based testing for contract validation
     */
    @FuzzTest
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void propertyBasedContractValidation(FuzzedDataProvider data) {
        Contract contract = new Contract();
        contract.setTitle(data.consumeString(data.consumeInt(0, 1000)));
        contract.setContent(data.consumeString(data.consumeInt(0, 50000)));
        contract.setRiskScore(data.consumeInt());
        
        // Property: Risk score should always be normalized
        if (contractService.validateContract(contract)) {
            assertTrue(contract.getRiskScore() >= 0 && contract.getRiskScore() <= 100,
                      "Risk score not properly normalized");
        }
        
        // Property: Title and content should be sanitized
        assertNoInjectionPatterns(contract.getTitle());
        assertNoInjectionPatterns(contract.getContent());
        
        // Property: Contract ID should be UUID format if generated
        if (contract.getId() != null) {
            assertTrue(contract.getId().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Invalid UUID format");
        }
    }

    /**
     * Helper method to check for injection patterns in strings
     */
    private void assertNoInjectionPatterns(String value) {
        if (value == null) return;
        
        String[] dangerousPatterns = {
            "<script", "</script>", "javascript:",
            "onclick=", "onerror=", "onload=",
            "DROP TABLE", "INSERT INTO", "UPDATE SET",
            "DELETE FROM", "UNION SELECT", "OR 1=1",
            "../", "..\\", "%00", "\0"
        };
        
        String lowerValue = value.toLowerCase();
        for (String pattern : dangerousPatterns) {
            assertFalse(lowerValue.contains(pattern.toLowerCase()),
                       "Dangerous pattern found: " + pattern);
        }
    }

    /**
     * Helper method to verify secure error handling
     */
    private void assertSecureErrorHandling(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            // No sensitive information leakage
            assertFalse(message.contains("password"));
            assertFalse(message.contains("secret"));
            assertFalse(message.contains("java.sql"));
            assertFalse(message.contains("org.hibernate"));
            assertFalse(message.contains("stack trace"));
            
            // No system paths
            assertFalse(message.contains("/home/"));
            assertFalse(message.contains("C:\\"));
            assertFalse(message.contains("/etc/"));
        }
    }
}