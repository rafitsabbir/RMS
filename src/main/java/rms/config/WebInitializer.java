package rms.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import rms.service.DocumentRules;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer{

	/** A whole upload request: the 5 MB file plus room for the other form fields. */
	static final long MAX_UPLOAD_REQUEST = DocumentRules.MAX_BYTES + 1024 * 1024;

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
