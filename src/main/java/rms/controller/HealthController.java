package rms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import rms.service.HealthService;

/**
 * The health check for monitoring and the container's HEALTHCHECK: 200 "ok" when the application and its database
 * answer, 503 "unavailable" when the database doesn't. Public (SecurityConfig), so it says nothing more: no versions,
 * no names, no error text.
 */
@Controller
@RequestMapping(value = "/")
public class HealthController {

	@Autowired
	HealthService healthservice;

	@RequestMapping(value = "/health", method = RequestMethod.GET)
	public ResponseEntity<String> health() {
		boolean up = healthservice.isDatabaseUp();
		return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
				.contentType(MediaType.TEXT_PLAIN).body(up ? "ok" : "unavailable");
	}
}
