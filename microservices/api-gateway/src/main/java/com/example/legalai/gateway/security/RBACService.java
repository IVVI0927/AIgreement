package com.example.legalai.gateway.security;

import com.example.legalai.gateway.model.Role;
import com.example.legalai.gateway.model.User;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class RBACService {

    private static final Map<Role, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // Admin has all permissions
        ROLE_PERMISSIONS.put(Role.ADMIN, Set.of(
            "contract:read", "contract:write", "contract:delete",
            "analysis:read", "analysis:write", "analysis:delete",
            "user:read", "user:write", "user:delete",
            "settings:read", "settings:write"
        ));

        // Reviewer can read and write contracts and analyses
        ROLE_PERMISSIONS.put(Role.REVIEWER, Set.of(
            "contract:read", "contract:write",
            "analysis:read", "analysis:write",
            "user:read",
            "settings:read"
        ));

        // Viewer has read-only access
        ROLE_PERMISSIONS.put(Role.VIEWER, Set.of(
            "contract:read",
            "analysis:read",
            "user:read"
        ));
    }

    public boolean hasPermission(User user, String permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        
        Set<String> rolePermissions = ROLE_PERMISSIONS.get(user.getRole());
        return rolePermissions != null && rolePermissions.contains(permission);
    }

    public Set<String> getUserPermissions(User user) {
        if (user == null || user.getRole() == null) {
            return Set.of();
        }
        return ROLE_PERMISSIONS.getOrDefault(user.getRole(), Set.of());
    }

    public boolean canAccessContract(User user, String action) {
        return hasPermission(user, "contract:" + action);
    }

    public boolean canAccessAnalysis(User user, String action) {
        return hasPermission(user, "analysis:" + action);
    }

    public boolean canManageUsers(User user) {
        return hasPermission(user, "user:write");
    }
}