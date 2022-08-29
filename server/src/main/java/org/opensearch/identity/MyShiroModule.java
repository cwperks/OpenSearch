package org.opensearch.identity;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.opensearch.common.Strings;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prototyping use of identity management (authentication &amp; authorization) implemented with Shiro
 * See more at https://shiro.apache.org/
 */
public class MyShiroModule {

    public static String REALM_NAME = "OpenSearch";

    public static MyRealm realm = new MyRealm();

    public MyShiroModule() {
        final SecurityManager securityManager = new DefaultSecurityManager(realm);
        // Note; this sets up security for the JVM, if we are crossing a JVM boundary will need to look at how this is made available
        SecurityUtils.setSecurityManager(securityManager);
    }

    /**
     * Generates an internal subject to run requests in this context
     */
    public static Subject getSubjectOrInternal() {
        final Subject existingSubject = SecurityUtils.getSubject();
        if (existingSubject.isAuthenticated()) {
            return existingSubject;
        }

        // Used for tracing where this function is used that is visible in the log output
        final Exception e = new Exception();
        final StackTraceElement current = e.getStackTrace()[1];
        final String sourceAnnotation = current.getFileName() + "." + current.getMethodName() + "@" + current.getLineNumber();

        SimplePrincipalCollection spc = new SimplePrincipalCollection("INTERNAL", REALM_NAME);
        final Subject internalSubject = new Subject.Builder().authenticated(true)
            .principals(spc) // How can we ensure the roles this             // princpal resolves?
            .contextAttribute("NodeId", "???") // Can we use this to source the originating node in a cluster?
            .buildSubject();
        // Session for INTERNAL subject should not expire
        internalSubject.getSession().setTimeout(-1);
        return internalSubject;
    }

    /**
     * Attempt to authenticate via authorization header, ignores if already authenticated, throws exceptions if unable
     *
     * NOTE: this was quickly built for test scenarios and will not be used log term, there is a supplimental library for
     * Shiro that supports web based applications, including https://shiro.apache.org/static/1.9.1/apidocs/org/apache/shiro/web/filter/authc/BasicHttpAuthenticationFilter.html
     * if this can be reused and to what degree will be useful in future investigations.
     */
    public static void authenticateViaAuthorizationHeader(final Optional<String> authHeader) throws AuthenticationException {
        final Subject currentSubject = SecurityUtils.getSubject();
        if (currentSubject.isAuthenticated()) {
            // No need to authenticate already ready
            return;
        }

        if (authHeader.isPresent() && !(Strings.isNullOrEmpty(authHeader.get())) && authHeader.get().startsWith("Basic ")) {
            final byte[] decodedAuthHeader = Base64.getDecoder().decode(authHeader.get().substring(6));
            final String[] decodedUserNamePassword = new String(decodedAuthHeader).split(":");
            currentSubject.login(new UsernamePasswordToken(decodedUserNamePassword[0], decodedUserNamePassword[1]));
            // Successful login - return!
            return;
        }

        throw new AuthenticationException("Unable to authenticate user!");
    }

    public static boolean doesPrincipalExist(PrincipalCollection principalCollection) {
        return realm.accountExists(principalCollection.getPrimaryPrincipal().toString());
    }

    public static List<String> getPermissionsForUser(PrincipalCollection principalCollection) {
        List<String> myPermissions = new ArrayList<>();
        AuthorizationInfo authInfo = realm.getAuthorizationInfo(principalCollection);
        if (authInfo != null && authInfo.getStringPermissions() != null) {
            myPermissions.addAll(authInfo.getStringPermissions());
        }
        return myPermissions;
    }

    public static Map<String, List<String>> getRolesAndPermissionsForUser(PrincipalCollection principalCollection) {
        Map<String, List<String>> rolesAndPermissions = new HashMap<>();
        AuthorizationInfo authInfo = realm.getAuthorizationInfo(principalCollection);
        if (authInfo != null && authInfo.getRoles() != null) {
            for (String role : authInfo.getRoles()) {
                Collection<Permission> rolePermissions = realm.getRolePermissionResolver().resolvePermissionsInRole(role);
                if (rolePermissions != null) {
                    List<String> rolePermissionStrings = rolePermissions.stream().map(p -> p.toString()).collect(Collectors.toList());
                    rolesAndPermissions.put(role, rolePermissionStrings);
                }
            }
        }
        return rolesAndPermissions;
    }

    /* Super basic role management */
    private enum Roles {
        ALL_ACCESS,
        ALL_CLUSTER,
        ALL_INDEX,
        CLUSTER_MONITOR,
        INTERNAL,
        KIBANA_USER
    }

    /** Very basic user pool and permissions ecosystem */
    private static class MyRealm extends SimpleAccountRealm {
        private MyRealm() {
            super(REALM_NAME);

            /* Default account configuration */
            this.addAccount("admin", "admin", Roles.ALL_ACCESS.name());
            this.addAccount("user", "user", Roles.KIBANA_USER.name());
            this.addAccount("user2", "user", Roles.KIBANA_USER.name());
            this.addAccount("user3", "user", Roles.ALL_CLUSTER.name());

            /* Attempt to grant access for internal accounts, but wasn't able to correlate them via
              the Just-In-Time subject creation, will need to do additional investigation */
            this.addAccount("INTERNAL", "INTERNAL", Roles.INTERNAL.name());

            /* Looking at how roles can be translated into permissions */
            this.setRolePermissionResolver(new RolePermissionResolver() {
                @Override
                public Collection<Permission> resolvePermissionsInRole(final String roleString) {
                    switch (Roles.valueOf(roleString)) {
                        case ALL_ACCESS:
                            return Collections.singleton(new AllPermission());
                        case ALL_CLUSTER:
                            return Collections.singleton(new WildcardPermission("cluster"));
                        case INTERNAL:
                            return Collections.singleton(new OpenSearchWildcardPermission("cluster,indices,internal"));
                        case CLUSTER_MONITOR:
                            return Collections.singleton(new OpenSearchWildcardPermission("cluster,indices"));
                        case KIBANA_USER:
                            return List.of(
                                new OpenSearchWildcardPermission("indices:data/write/*"),
                                new OpenSearchWildcardPermission("indices:data/admin/aliases*"),
                                new OpenSearchWildcardPermission("indices:data/admin/mapping/put"),
                                new OpenSearchWildcardPermission("indices:data/admin/mappings/fields/get*"),
                                new OpenSearchWildcardPermission("indices:data/admin/resolve/index"),
                                new OpenSearchWildcardPermission("indices:data/read/*"),
                                new OpenSearchWildcardPermission("indices:admin/*"),
                                new OpenSearchWildcardPermission("indices:monitor/*"),
                                new OpenSearchWildcardPermission("cluster:monitor/*")
                            );
                        case ALL_INDEX:
                            return Collections.singleton(new WildcardPermission("indices"));
                        default:
                            throw new RuntimeException("Unknown Permission: " + roleString);
                    }
                }
            });
        }

        public AuthorizationInfo getAuthorizationInfo(PrincipalCollection principalCollection) {
            AuthorizationInfo authInfo = doGetAuthorizationInfo(principalCollection);
            return authInfo;
        }
    }
}
