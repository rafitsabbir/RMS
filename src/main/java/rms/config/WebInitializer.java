package rms.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import rms.service.DocumentRules;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer{

	private static final Logger LOG = LoggerFactory.getLogger(WebInitializer.class);

	/** Set to true where RMS is only reached over HTTPS: the session cookie then always carries Secure. */
	static final String SECURE_COOKIE_VARIABLE = "RMS_SECURE_COOKIE";

	/** A whole upload request: the 5 MB file plus room for the other form fields. */
	static final long MAX_UPLOAD_REQUEST = DocumentRules.MAX_BYTES + 1024 * 1024;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		super.onStartup(servletContext);
		applySecureCookie(servletContext, System.getenv(SECURE_COOKIE_VARIABLE));
	}

	/**
	 * The Secure flag on the session cookie (release-tracker item 8). Tomcat already sets it on a request that
	 * came over HTTPS; this also covers HTTPS ended at a proxy without RemoteIpValve. Off by default, so the
	 * local trial and CI keep working over plain HTTP. Only "true" turns it on.
	 */
	static void applySecureCookie(ServletContext servletContext, String setting) {
		boolean secure = "true".equalsIgnoreCase(setting == null ? null : setting.trim());
		if (secure) {
			servletContext.getSessionCookieConfig().setSecure(true);
		}
		LOG.info("Session cookie Secure flag: {} ({})", secure ? "always" : "when the request is HTTPS",
				SECURE_COOKIE_VARIABLE);
	}

	@Override
	protected Class<?>[] getRootConfigClasses() {
		// TODO Auto-generated method stub
		return new Class[] {WebConfig.class};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getServletMappings() {
		// TODO Auto-generated method stub
		return new String[] {"/"};
	}

	/**
	 * Document uploads (Phase 2): at most 5 MB per file and 6 MB per request, held in the container's temporary
	 * folder while the request runs. A larger upload is refused by the container before RMS reads it
	 * (SecurityConfig sends the browser back to the candidate profile with a message).
	 */
	@Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
		registration.setMultipartConfig(
				new MultipartConfigElement("", DocumentRules.MAX_BYTES, MAX_UPLOAD_REQUEST, 0));
	}

}
