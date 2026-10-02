/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2024 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.isf.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every endpoint must be covered by a rule of {@link SecurityConfig}: a path without one is open to any logged-in user
 * ({@code anyRequest().authenticated()}), whatever the user's permissions. This happened to {@code /stockmovements/**},
 * an older copy of {@code /medicalstockmovements/**}.
 */
class SecurityRulesCoverageTest {

	/**
	 * Paths knowingly without a permission rule. {@code bills}: OH has no {@code bills.*} permissions yet, so a rule would
	 * lock everyone out of billing; it needs permission rows in the database first.
	 */
	private static final Set<String> KNOWN_WITHOUT_RULE = Set.of("bills");

	@Test
	void everyEndpointHasASecurityRule() throws Exception {
		String securityConfig = Files.readString(Path.of("src/main/java/org/isf/config/SecurityConfig.java"), StandardCharsets.UTF_8);
		Set<String> covered = new TreeSet<>();
		Matcher rule = Pattern.compile("requestMatchers\\(([^)]*)\\)").matcher(securityConfig);
		while (rule.find()) {
			Matcher path = Pattern.compile("\"/([^/\"*]*)").matcher(rule.group(1));
			while (path.find()) {
				covered.add(path.group(1));
			}
		}

		Set<String> uncovered = new TreeSet<>();
		for (String endpoint : endpointPaths()) {
			String first = endpoint.replaceFirst("^/", "").split("/")[0];
			if (!covered.contains(first) && !KNOWN_WITHOUT_RULE.contains(first)) {
				uncovered.add(endpoint);
			}
		}
		assertThat(uncovered).as("endpoints without a rule in SecurityConfig").isEmpty();
	}

	private static Set<String> endpointPaths() throws IOException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		Set<String> paths = new TreeSet<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents("org.isf")) {
			Class<?> controller = ClassUtils.resolveClassName(candidate.getBeanClassName(), SecurityRulesCoverageTest.class.getClassLoader());
			if (controller.getProtectionDomain().getCodeSource().getLocation().getPath().contains("test-classes")) {
				// a controller of the tests, not an endpoint of the API
				continue;
			}
			RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
			String prefix = classMapping == null ? "" : Stream.of(classMapping.path()).findFirst().orElse("");
			for (Method method : controller.getDeclaredMethods()) {
				RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
				if (mapping != null) {
					for (String path : mapping.path().length == 0 ? new String[] { "" } : mapping.path()) {
						paths.add(prefix + path);
					}
				}
			}
		}
		return paths;
	}
}
