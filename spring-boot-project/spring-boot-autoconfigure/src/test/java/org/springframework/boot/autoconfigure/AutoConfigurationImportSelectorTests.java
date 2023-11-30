/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AutoConfigurationImportSelector}
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
class AutoConfigurationImportSelectorTests {

	private final TestAutoConfigurationImportSelector importSelector = new TestAutoConfigurationImportSelector();

	private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final MockEnvironment environment = new MockEnvironment();

	private final List<AutoConfigurationImportFilter> filters = new ArrayList<>();

	@BeforeEach
	void setup() {
		this.importSelector.setBeanFactory(this.beanFactory);
		this.importSelector.setEnvironment(this.environment);
		this.importSelector.setResourceLoader(new DefaultResourceLoader());
	}

	@Test
	void importsAreSelectedWhenUsingEnableAutoConfiguration() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSameSizeAs(getAutoConfigurationClassNames());
		assertThat(this.importSelector.getLastEvent().getExclusions()).isEmpty();
	}

	@Test
	void classExclusionsAreApplied() {
		String[] imports = selectImports(EnableAutoConfigurationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	void classExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(SpringBootApplicationWithClassExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	void classNamesExclusionsAreApplied() {
		String[] imports = selectImports(EnableAutoConfigurationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(MustacheAutoConfiguration.class.getName());
	}

	@Test
	void classNamesExclusionsAreAppliedWhenUsingSpringBootApplication() {
		String[] imports = selectImports(SpringBootApplicationWithClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(MustacheAutoConfiguration.class.getName());
	}

	@Test
	void propertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude", FreeMarkerAutoConfiguration.class.getName());
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 1);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(FreeMarkerAutoConfiguration.class.getName());
	}

	@Test
	void severalPropertyExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				FreeMarkerAutoConfiguration.class.getName() + "," + MustacheAutoConfiguration.class.getName());
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	void severalPropertyExclusionsAreAppliedWithExtraSpaces() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				FreeMarkerAutoConfiguration.class.getName() + " , " + MustacheAutoConfiguration.class.getName() + " ");
		testSeveralPropertyExclusionsAreApplied();
	}

	@Test
	void severalPropertyYamlExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude[0]", FreeMarkerAutoConfiguration.class.getName());
		this.environment.setProperty("spring.autoconfigure.exclude[1]", MustacheAutoConfiguration.class.getName());
		testSeveralPropertyExclusionsAreApplied();
	}

	private void testSeveralPropertyExclusionsAreApplied() {
		String[] imports = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 2);
		assertThat(this.importSelector.getLastEvent().getExclusions())
			.contains(FreeMarkerAutoConfiguration.class.getName(), MustacheAutoConfiguration.class.getName());
	}

	@Test
	void combinedExclusionsAreApplied() {
		this.environment.setProperty("spring.autoconfigure.exclude", ThymeleafAutoConfiguration.class.getName());
		String[] imports = selectImports(EnableAutoConfigurationWithClassAndClassNameExclusions.class);
		assertThat(imports).hasSize(getAutoConfigurationClassNames().size() - 3);
		assertThat(this.importSelector.getLastEvent().getExclusions()).contains(
				FreeMarkerAutoConfiguration.class.getName(), MustacheAutoConfiguration.class.getName(),
				ThymeleafAutoConfiguration.class.getName());
	}

	@Test
	void nonAutoConfigurationClassExclusionsShouldThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassExclude.class));
	}

	@Test
	void nonAutoConfigurationClassNameExclusionsWhenPresentOnClassPathShouldThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> selectImports(EnableAutoConfigurationWithFaultyClassNameExclude.class));
	}

	@Test
	void nonAutoConfigurationPropertyExclusionsWhenPresentOnClassPathShouldThrowException() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests.TestConfiguration");
		assertThatIllegalStateException().isThrownBy(() -> selectImports(BasicEnableAutoConfiguration.class));
	}

	@Test
	void nameAndPropertyExclusionsWhenNotPresentOnClasspathShouldNotThrowException() {
		this.environment.setProperty("spring.autoconfigure.exclude",
				"org.springframework.boot.autoconfigure.DoesNotExist2");
		selectImports(EnableAutoConfigurationWithAbsentClassNameExclude.class);
		assertThat(this.importSelector.getLastEvent().getExclusions()).containsExactlyInAnyOrder(
				"org.springframework.boot.autoconfigure.DoesNotExist1",
				"org.springframework.boot.autoconfigure.DoesNotExist2");
	}

	@Test
	void filterShouldFilterImports() {
		String[] defaultImports = selectImports(BasicEnableAutoConfiguration.class);
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 1));
		this.filters.add(new TestAutoConfigurationImportFilter(defaultImports, 3, 4));
		String[] filtered = selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filtered).hasSize(defaultImports.length - 3);
		assertThat(filtered).doesNotContain(defaultImports[1], defaultImports[3], defaultImports[4]);
	}

	@Test
	void filterShouldSupportAware() {
		TestAutoConfigurationImportFilter filter = new TestAutoConfigurationImportFilter(new String[] {});
		this.filters.add(filter);
		selectImports(BasicEnableAutoConfiguration.class);
		assertThat(filter.getBeanFactory()).isEqualTo(this.beanFactory);
	}

	@Test
	void getExclusionFilterReuseFilters() {
		String[] allImports = new String[] { "com.example.A", "com.example.B", "com.example.C" };
		this.filters.add(new TestAutoConfigurationImportFilter(allImports, 0));
		this.filters.add(new TestAutoConfigurationImportFilter(allImports, 2));
		assertThat(this.importSelector.getExclusionFilter().test("com.example.A")).isTrue();
		assertThat(this.importSelector.getExclusionFilter().test("com.example.B")).isFalse();
		assertThat(this.importSelector.getExclusionFilter().test("com.example.C")).isTrue();
	}

	private String[] selectImports(Class<?> source) {
		return this.importSelector.selectImports(AnnotationMetadata.introspect(source));
	}

	private List<String> getAutoConfigurationClassNames() {
		return ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();
	}

	private class TestAutoConfigurationImportSelector extends AutoConfigurationImportSelector {

		private AutoConfigurationImportEvent lastEvent;

		@Override
		protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
			return AutoConfigurationImportSelectorTests.this.filters;
		}

		@Override
		protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
			return Collections.singletonList((event) -> this.lastEvent = event);
		}

		AutoConfigurationImportEvent getLastEvent() {
			return this.lastEvent;
		}

	}

	static class TestAutoConfigurationImportFilter implements AutoConfigurationImportFilter, BeanFactoryAware {

		private final Set<String> nonMatching = new HashSet<>();

		private BeanFactory beanFactory;

		TestAutoConfigurationImportFilter(String[] configurations, int... nonMatching) {
			for (int i : nonMatching) {
				this.nonMatching.add(configurations[i]);
			}
		}

		@Override
		public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
			boolean[] result = new boolean[autoConfigurationClasses.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = !this.nonMatching.contains(autoConfigurationClasses[i]);
			}
			return result;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		BeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	private class TestConfiguration {

	}

	@EnableAutoConfiguration
	private class BasicEnableAutoConfiguration {

	}

	@EnableAutoConfiguration(exclude = FreeMarkerAutoConfiguration.class)
	private class EnableAutoConfigurationWithClassExclusions {

	}

	@SpringBootApplication(exclude = FreeMarkerAutoConfiguration.class)
	private class SpringBootApplicationWithClassExclusions {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration")
	private class EnableAutoConfigurationWithClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = MustacheAutoConfiguration.class,
			excludeName = "org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration")
	private class EnableAutoConfigurationWithClassAndClassNameExclusions {

	}

	@EnableAutoConfiguration(exclude = TestConfiguration.class)
	private class EnableAutoConfigurationWithFaultyClassExclude {

	}

	@EnableAutoConfiguration(
			excludeName = "org.springframework.boot.autoconfigure.AutoConfigurationImportSelectorTests.TestConfiguration")
	private class EnableAutoConfigurationWithFaultyClassNameExclude {

	}

	@EnableAutoConfiguration(excludeName = "org.springframework.boot.autoconfigure.DoesNotExist1")
	private class EnableAutoConfigurationWithAbsentClassNameExclude {

	}

	@SpringBootApplication(excludeName = "org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration")
	private class SpringBootApplicationWithClassNameExclusions {

	}

}
