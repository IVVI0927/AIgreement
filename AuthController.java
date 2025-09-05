package com.legal.auth.controller;

import com.legal.auth.dto.*;
import com.legal.auth.service.AuthService;
import com.legal.auth.util.JwtUtil;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // Rate limiting buckets per IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    /**
     * 用户登录接口
     * 实现了防暴力破解、SQL注入防护、XSS防护
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, 
                                  HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            
            // Rate limiting - 防止暴力破解
            if (!tryConsume(clientIp)) {
                logger.warn("Rate limit exceeded for IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Too many login attempts. Please try again later."));
            }
            
            // Input validation - 防止SQL注入
            if (!isValidInput(request.getUsername()) || !isValidInput(request.getPassword())) {
                logger.warn("Invalid input detected from IP: {}", clientIp);
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid input characters detected"));
            }
            
            // Authenticate user
            LoginResponse response = authService.login(request);
            
            // Log successful login
            logger.info("User {} logged in successfully from IP: {}", 
                       request.getUsername(), clientIp);
            
            // Add security headers
            return ResponseEntity.ok()
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "DENY")
                .header("X-XSS-Protection", "1; mode=block")
                .body(response);
                
        } catch (Exception e) {
            logger.error("Login failed for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Authentication failed"));
        }
    }
    
    /**
     * 用户注册接口
     * 实现了密码强度验证、邮箱验证
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Password strength validation
            if (!isStrongPassword(request.getPassword())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Password must be at least 8 characters with uppercase, lowercase, number and special character"));
            }
            
            // Email validation
            if (!isValidEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid email format"));
            }
            
            UserResponse response = authService.register(request);
            
            logger.info("New user registered: {}", request.getUsername());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Registration failed", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    /**
     * 启用2FA双因素认证
     * 返回QR码URL供用户扫描
     */
    @PostMapping("/enable-2fa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enable2FA(@RequestHeader("Authorization") String token) {
        try {
            String username = extractUsername(token);
            String qrCodeUrl = authService.enable2FA(username);
            
            logger.info("2FA enabled for user: {}", username);
            
            return ResponseEntity.ok(Map.of(
                "message", "2FA enabled successfully",
                "qrCodeUrl", qrCodeUrl
            ));
            
        } catch (Exception e) {
            logger.error("Failed to enable 2FA", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to enable 2FA"));
        }
    }
    
    /**
     * 验证2FA代码
     */
    @PostMapping("/verify-2fa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verify2FA(@RequestHeader("Authorization") String token,
                                       @RequestBody @Valid TwoFactorRequest request) {
        try {
            String username = extractUsername(token);
            boolean isValid = authService.verify2FA(username, request.getCode());
            
            if (isValid) {
                logger.info("2FA verification successful for user: {}", username);
                return ResponseEntity.ok(Map.of("message", "2FA verification successful"));
            } else {
                logger.warn("2FA verification failed for user: {}", username);
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid 2FA code"));
            }
            
        } catch (Exception e) {
            logger.error("2FA verification error", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("2FA verification failed"));
        }
    }
    
    /**
     * 刷新JWT Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            String refreshedToken = authService.refreshToken(token);
            return ResponseEntity.ok(Map.of("token", refreshedToken));
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Token refresh failed"));
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            String username = extractUsername(token);
            authService.logout(token);
            
            logger.info("User {} logged out", username);
            
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Logout failed"));
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String username = extractUsername(token);
            UserResponse user = authService.getUserInfo(username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Failed to get user info", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to get user information"));
        }
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestHeader("Authorization") String token,
                                           @Valid @RequestBody ChangePasswordRequest request) {
        try {
            String username = extractUsername(token);
            
            // Validate new password strength
            if (!isStrongPassword(request.getNewPassword())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("New password does not meet security requirements"));
            }
            
            authService.changePassword(username, request);
            
            logger.info("Password changed for user: {}", username);
            
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            logger.error("Password change failed", e);
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Failed to change password"));
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Rate limiting implementation
     * 每个IP每分钟最多10次请求
     */
    private boolean tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, k -> {
            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
        return bucket.tryConsume(1);
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 输入验证 - 防止SQL注入和XSS
     */
    private boolean isValidInput(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        // Check for SQL injection patterns
        String[] sqlPatterns = {"'", "\"", "--", "/*", "*/", "xp_", "sp_", "0x"};
        String lowerInput = input.toLowerCase();
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return false;
            }
        }
        
        // Check for XSS patterns
        String[] xssPatterns = {"<script", "<iframe", "javascript:", "onerror=", "onload="};
        for (String pattern : xssPatterns) {
            if (lowerInput.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 密码强度验证
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * 邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * 从Token中提取用户名
     */
    private String extractUsername(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getClaimsFromToken(token).getSubject();
    }
}

// ==================== DTO Classes ====================

class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private Integer totpCode; // Optional for 2FA
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getTotpCode() { return totpCode; }
    public void setTotpCode(Integer totpCode) { this.totpCode = totpCode; }
}

class RegisterRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Email is required")
    private String email;
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class LoginResponse {
    private String token;
    private String username;
    private Map<String, Object> claims;
    
    public LoginResponse(String token, String username, Map<String, Object> claims) {
        this.token = token;
        this.username = username;
        this.claims = claims;
    }
    
    // Getters and setters
    public String getToken() { return token; }
    public String getUsername() { return username; }
    public Map<String, Object> getClaims() { return claims; }
}

class UserResponse {
    private Long id;
    private String username;
    private String email;
    private boolean twoFactorEnabled;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
}

class ErrorResponse {
    private String error;
    private long timestamp;
    
    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public String getError() { return error; }
    public long getTimestamp() { return timestamp; }
}

class TwoFactorRequest {
    @NotBlank(message = "2FA code is required")
    private Integer code;
    
    // Getter and setter
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
}

class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
    
    // Getters and setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}