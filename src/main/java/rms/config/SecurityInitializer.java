package rms.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Puts Spring Security's filter chain (SecurityConfig) in front of every request. WebInitializer creates the
 * application context, so this class only registers the filter.
 */
public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {

}
