package rms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import rms.config.LoginThrottle;
import rms.config.SecurityConfig;

/**
 * Builds the production beans of the controller, service and DAO packages, as WebConfig's scan does, and checks that
 * every field holding another RMS bean (a *Service, *Dao or LoginThrottle) was injected. The unit tests set these
 * fields themselves, so a lost @Autowired only showed on Tomcat: four services' activity log setters had none, and
 * assigning an interviewer, an evaluation, a decision and an interview answered HTTP 500 after saving.
 */
class WiringTest {

	@Test
	void everyRmsBeanFieldIsInjected() throws Exception {
		List<String> missing = new ArrayList<>();
		try (AnnotationConfigApplicationContext app = new AnnotationConfigApplicationContext()) {
			app.registerBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class));
			app.registerBean(PasswordEncoder.class, () -> new SecurityConfig().passwordEncoder());
			app.registerBean(LoginThrottle.class);
			app.scan("rms.controller", "rms.service", "rms.dao");
			app.refresh();

			for (String name : app.getBeanDefinitionNames()) {
				Object bean = app.getBean(name);
				if (!bean.getClass().getName().startsWith("rms.")) {
					continue;
				}
				for (Class<?> type = bean.getClass(); type != Object.class; type = type.getSuperclass()) {
					for (Field field : type.getDeclaredFields()) {
						if (Modifier.isStatic(field.getModifiers()) || !isRmsBean(field.getType())) {
							continue;
						}
						field.setAccessible(true);
						if (field.get(bean) == null) {
							missing.add(type.getSimpleName() + "." + field.getName());
						}
					}
				}
			}
		}
		assertThat(missing).as("RMS bean fields left null (a missing @Autowired?)").isEmpty();
	}

	private static boolean isRmsBean(Class<?> type) {
		String name = type.getName();
		return name.startsWith("rms.") && (name.endsWith("Service") || name.endsWith("Dao") || type == LoginThrottle.class);
	}
}
