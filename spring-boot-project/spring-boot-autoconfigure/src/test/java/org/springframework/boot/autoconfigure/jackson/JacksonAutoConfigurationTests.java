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

package org.springframework.boot.autoconfigure.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.JacksonAutoConfigurationRuntimeHints;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.boot.jackson.JsonMixinModule;
import org.springframework.boot.jackson.JsonMixinModuleEntries;
import org.springframework.boot.jackson.JsonObjectSerializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JacksonAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Marcel Overdijk
 * @author Sebastien Deleuze
 * @author Johannes Edmeier
 * @author Grzegorz Poznachowski
 * @author Ralf Ueberfuhr
 * @author Eddú Meléndez
 */
class JacksonAutoConfigurationTests {

	protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

	@Test
	void doubleModuleRegistration() {
		this.contextRunner.withUserConfiguration(DoubleModulesConfig.class)
			.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(mapper.writeValueAsString(new Foo())).isEqualTo("{\"foo\":\"bar\"}");
			});
	}

	@Test
	void jsonMixinModuleShouldBeAutoConfiguredWithBasePackages() {
		this.contextRunner.withUserConfiguration(MixinConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JsonMixinModule.class).hasSingleBean(JsonMixinModuleEntries.class);
			JsonMixinModuleEntries moduleEntries = context.getBean(JsonMixinModuleEntries.class);
			assertThat(moduleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
				.contains(entry(Person.class, EmptyMixin.class));
		});
	}

	@Test
	void noCustomDateFormat() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.getDateFormat()).isInstanceOf(StdDateFormat.class);
		});
	}

	@Test
	void customDateFormat() {
		this.contextRunner.withPropertyValues("spring.jackson.date-format:yyyyMMddHHmmss").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			DateFormat dateFormat = mapper.getDateFormat();
			assertThat(dateFormat).isInstanceOf(SimpleDateFormat.class);
			assertThat(((SimpleDateFormat) dateFormat).toPattern()).isEqualTo("yyyyMMddHHmmss");
		});
	}

	@Test
	void customDateFormatClass() {
		this.contextRunner.withPropertyValues(
				"spring.jackson.date-format:org.springframework.boot.autoconfigure.jackson.JacksonAutoConfigurationTests.MyDateFormat")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(mapper.getDateFormat()).isInstanceOf(MyDateFormat.class);
			});
	}

	@Test
	void noCustomPropertyNamingStrategy() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.getPropertyNamingStrategy()).isNull();
		});
	}

	@Test
	void customPropertyNamingStrategyField() {
		this.contextRunner.withPropertyValues("spring.jackson.property-naming-strategy:SNAKE_CASE").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.getPropertyNamingStrategy()).isInstanceOf(SnakeCaseStrategy.class);
		});
	}

	@Test
	void customPropertyNamingStrategyClass() {
		this.contextRunner.withPropertyValues(
				"spring.jackson.property-naming-strategy:com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(mapper.getPropertyNamingStrategy()).isInstanceOf(SnakeCaseStrategy.class);
			});
	}

	@Test
	void enableSerializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.indent_output:true").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(SerializationFeature.INDENT_OUTPUT.enabledByDefault()).isFalse();
			assertThat(mapper.getSerializationConfig()
				.hasSerializationFeatures(SerializationFeature.INDENT_OUTPUT.getMask())).isTrue();
		});
	}

	@Test
	void disableSerializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.serialization.write_dates_as_timestamps:false")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.enabledByDefault()).isTrue();
				assertThat(mapper.getSerializationConfig()
					.hasSerializationFeatures(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.getMask())).isFalse();
			});
	}

	@Test
	void enableDeserializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.use_big_decimal_for_floats:true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.enabledByDefault()).isFalse();
				assertThat(mapper.getDeserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.getMask())).isTrue();
			});
	}

	@Test
	void disableDeserializationFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.deserialization.fail-on-unknown-properties:false")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.enabledByDefault()).isTrue();
				assertThat(mapper.getDeserializationConfig()
					.hasDeserializationFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask())).isFalse();
			});
	}

	@Test
	void enableMapperFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.require_setters_for_getters:true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.enabledByDefault()).isFalse();

				assertThat(mapper.getSerializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS))
					.isTrue();
				assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS))
					.isTrue();
			});
	}

	@Test
	void disableMapperFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.mapper.use_annotations:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(MapperFeature.USE_ANNOTATIONS.enabledByDefault()).isTrue();
			assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS)).isFalse();
			assertThat(mapper.getSerializationConfig().isEnabled(MapperFeature.USE_ANNOTATIONS)).isFalse();
		});
	}

	@Test
	void enableParserFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.parser.allow_single_quotes:true").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(JsonParser.Feature.ALLOW_SINGLE_QUOTES.enabledByDefault()).isFalse();
			assertThat(mapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES)).isTrue();
		});
	}

	@Test
	void disableParserFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.parser.auto_close_source:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(JsonParser.Feature.AUTO_CLOSE_SOURCE.enabledByDefault()).isTrue();
			assertThat(mapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE)).isFalse();
		});
	}

	@Test
	void enableGeneratorFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.generator.strict_duplicate_detection:true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				JsonGenerator.Feature feature = JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION;
				assertThat(feature.enabledByDefault()).isFalse();
				assertThat(mapper.getFactory().isEnabled(feature)).isTrue();
			});
	}

	@Test
	void disableGeneratorFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.generator.auto_close_target:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(JsonGenerator.Feature.AUTO_CLOSE_TARGET.enabledByDefault()).isTrue();
			assertThat(mapper.getFactory().isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET)).isFalse();
		});
	}

	@Test
	void defaultObjectMapperBuilder() {
		this.contextRunner.run((context) -> {
			Jackson2ObjectMapperBuilder builder = context.getBean(Jackson2ObjectMapperBuilder.class);
			ObjectMapper mapper = builder.build();
			assertThat(MapperFeature.DEFAULT_VIEW_INCLUSION.enabledByDefault()).isTrue();
			assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
			assertThat(MapperFeature.DEFAULT_VIEW_INCLUSION.enabledByDefault()).isTrue();
			assertThat(mapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
			assertThat(mapper.getSerializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
			assertThat(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.enabledByDefault()).isTrue();
			assertThat(mapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
				.isFalse();
		});
	}

	@Test
	void enableEnumFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.enum.write-enums-to-lowercase=true")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(EnumFeature.WRITE_ENUMS_TO_LOWERCASE.enabledByDefault()).isFalse();
				assertThat(mapper.getSerializationConfig().isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)).isTrue();
			});
	}

	@Test
	void disableJsonNodeFeature() {
		this.contextRunner.withPropertyValues("spring.jackson.datatype.json-node.write-null-properties:false")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				assertThat(JsonNodeFeature.WRITE_NULL_PROPERTIES.enabledByDefault()).isTrue();
				assertThat(mapper.getDeserializationConfig().isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES))
					.isFalse();
			});
	}

	@Test
	void moduleBeansAndWellKnownModulesAreRegisteredWithTheObjectMapperBuilder() {
		this.contextRunner.withUserConfiguration(ModuleConfig.class).run((context) -> {
			ObjectMapper objectMapper = context.getBean(Jackson2ObjectMapperBuilder.class).build();
			assertThat(context.getBean(CustomModule.class).getOwners()).contains(objectMapper);
			assertThat(objectMapper.canSerialize(Baz.class)).isTrue();
		});
	}

	@Test
	void defaultSerializationInclusion() {
		this.contextRunner.run((context) -> {
			ObjectMapper objectMapper = context.getBean(Jackson2ObjectMapperBuilder.class).build();
			assertThat(objectMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.USE_DEFAULTS);
		});
	}

	@Test
	void customSerializationInclusion() {
		this.contextRunner.withPropertyValues("spring.jackson.default-property-inclusion:non_null").run((context) -> {
			ObjectMapper objectMapper = context.getBean(Jackson2ObjectMapperBuilder.class).build();
			assertThat(objectMapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
				.isEqualTo(JsonInclude.Include.NON_NULL);
		});
	}

	@Test
	void customTimeZoneFormattingADate() {
		this.contextRunner.withPropertyValues("spring.jackson.time-zone:GMT+10", "spring.jackson.date-format:z")
			.run((context) -> {
				ObjectMapper objectMapper = context.getBean(Jackson2ObjectMapperBuilder.class).build();
				Date date = new Date(1436966242231L);
				assertThat(objectMapper.writeValueAsString(date)).isEqualTo("\"GMT+10:00\"");
			});
	}

	@Test
	void enableDefaultLeniency() {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:true").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			Person person = mapper.readValue("{\"birthDate\": \"2010-12-30\"}", Person.class);
			assertThat(person.getBirthDate()).isNotNull();
		});
	}

	@Test
	void disableDefaultLeniency() {
		this.contextRunner.withPropertyValues("spring.jackson.default-leniency:false").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThatExceptionOfType(InvalidFormatException.class)
				.isThrownBy(() -> mapper.readValue("{\"birthDate\": \"2010-12-30\"}", Person.class))
				.withMessageContaining("expected format")
				.withMessageContaining("yyyyMMdd");
		});
	}

	@Test
	void constructorDetectorWithNoStrategyUseDefault() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.getDeserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithDefaultStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=default").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.getDeserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.HEURISTIC);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithUsePropertiesBasedStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-properties-based")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				ConstructorDetector cd = mapper.getDeserializationConfig().getConstructorDetector();
				assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.PROPERTIES);
				assertThat(cd.requireCtorAnnotation()).isFalse();
				assertThat(cd.allowJDKTypeConstructors()).isFalse();
			});
	}

	@Test
	void constructorDetectorWithUseDelegatingStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=use-delegating").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.getDeserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.DELEGATING);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void constructorDetectorWithExplicitOnlyStrategy() {
		this.contextRunner.withPropertyValues("spring.jackson.constructor-detector=explicit-only").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			ConstructorDetector cd = mapper.getDeserializationConfig().getConstructorDetector();
			assertThat(cd.singleArgMode()).isEqualTo(SingleArgConstructor.REQUIRE_MODE);
			assertThat(cd.requireCtorAnnotation()).isFalse();
			assertThat(cd.allowJDKTypeConstructors()).isFalse();
		});
	}

	@Test
	void additionalJacksonBuilderCustomization() {
		this.contextRunner.withUserConfiguration(ObjectMapperBuilderCustomConfig.class).run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.getDateFormat()).isInstanceOf(MyDateFormat.class);
		});
	}

	@Test
	void parameterNamesModuleIsAutoConfigured() {
		assertParameterNamesModuleCreatorBinding(Mode.DEFAULT, JacksonAutoConfiguration.class);
	}

	@Test
	void customParameterNamesModuleCanBeConfigured() {
		assertParameterNamesModuleCreatorBinding(Mode.DELEGATING, ParameterNamesModuleConfig.class,
				JacksonAutoConfiguration.class);
	}

	@Test
	void writeDurationAsTimestampsDefault() {
		this.contextRunner.run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			Duration duration = Duration.ofHours(2);
			assertThat(mapper.writeValueAsString(duration)).isEqualTo("\"PT2H\"");
		});
	}

	@Test
	void writeWithVisibility() {
		this.contextRunner
			.withPropertyValues("spring.jackson.visibility.getter:none", "spring.jackson.visibility.field:any")
			.run((context) -> {
				ObjectMapper mapper = context.getBean(ObjectMapper.class);
				String json = mapper.writeValueAsString(new VisibilityBean());
				assertThat(json).contains("property1");
				assertThat(json).contains("property2");
				assertThat(json).doesNotContain("property3");
			});
	}

	@Test
	void builderIsNotSharedAcrossMultipleInjectionPoints() {
		this.contextRunner.withUserConfiguration(ObjectMapperBuilderConsumerConfig.class).run((context) -> {
			ObjectMapperBuilderConsumerConfig consumer = context.getBean(ObjectMapperBuilderConsumerConfig.class);
			assertThat(consumer.builderOne).isNotNull();
			assertThat(consumer.builderTwo).isNotNull();
			assertThat(consumer.builderOne).isNotSameAs(consumer.builderTwo);
		});
	}

	@Test
	void jsonComponentThatInjectsObjectMapperCausesBeanCurrentlyInCreationException() {
		this.contextRunner.withUserConfiguration(CircularDependencySerializerConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasRootCauseInstanceOf(BeanCurrentlyInCreationException.class);
		});
	}

	@Test
	void shouldRegisterPropertyNamingStrategyHints() {
		shouldRegisterPropertyNamingStrategyHints(PropertyNamingStrategies.class, "LOWER_CAMEL_CASE",
				"UPPER_CAMEL_CASE", "SNAKE_CASE", "UPPER_SNAKE_CASE", "LOWER_CASE", "KEBAB_CASE", "LOWER_DOT_CASE");
	}

	private void shouldRegisterPropertyNamingStrategyHints(Class<?> type, String... fieldNames) {
		RuntimeHints hints = new RuntimeHints();
		new JacksonAutoConfigurationRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
		Stream.of(fieldNames)
			.map((name) -> reflection.onField(type, name))
			.forEach((predicate) -> assertThat(predicate).accepts(hints));
	}

	private void assertParameterNamesModuleCreatorBinding(Mode expectedMode, Class<?>... configClasses) {
		this.contextRunner.withUserConfiguration(configClasses).run((context) -> {
			DeserializationConfig deserializationConfig = context.getBean(ObjectMapper.class)
				.getDeserializationConfig();
			AnnotationIntrospector annotationIntrospector = deserializationConfig.getAnnotationIntrospector()
				.allIntrospectors()
				.iterator()
				.next();
			assertThat(annotationIntrospector).hasFieldOrPropertyWithValue("creatorBinding", expectedMode);
		});
	}

	static class MyDateFormat extends SimpleDateFormat {

		MyDateFormat() {
			super("yyyy-MM-dd HH:mm:ss");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockObjectMapperConfig {

		@Bean
		@Primary
		ObjectMapper objectMapper() {
			return mock(ObjectMapper.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BazSerializer.class)
	static class ModuleConfig {

		@Bean
		CustomModule jacksonModule() {
			return new CustomModule();
		}

	}

	@Configuration
	static class DoubleModulesConfig {

		@Bean
		Module jacksonModule() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(Foo.class, new JsonSerializer<>() {

				@Override
				public void serialize(Foo value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
					jgen.writeStartObject();
					jgen.writeStringField("foo", "bar");
					jgen.writeEndObject();
				}
			});
			return module;
		}

		@Bean
		@Primary
		ObjectMapper objectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(jacksonModule());
			return mapper;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ParameterNamesModuleConfig {

		@Bean
		ParameterNamesModule parameterNamesModule() {
			return new ParameterNamesModule(JsonCreator.Mode.DELEGATING);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObjectMapperBuilderCustomConfig {

		@Bean
		Jackson2ObjectMapperBuilderCustomizer customDateFormat() {
			return (builder) -> builder.dateFormat(new MyDateFormat());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObjectMapperBuilderConsumerConfig {

		Jackson2ObjectMapperBuilder builderOne;

		Jackson2ObjectMapperBuilder builderTwo;

		@Bean
		String consumerOne(Jackson2ObjectMapperBuilder builder) {
			this.builderOne = builder;
			return "one";
		}

		@Bean
		String consumerTwo(Jackson2ObjectMapperBuilder builder) {
			this.builderTwo = builder;
			return "two";
		}

	}

	protected static final class Foo {

		private String name;

		private Foo() {
		}

		static Foo create() {
			return new Foo();
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	static class Bar {

		private String propertyName;

		String getPropertyName() {
			return this.propertyName;
		}

		void setPropertyName(String propertyName) {
			this.propertyName = propertyName;
		}

	}

	@JsonComponent
	static class BazSerializer extends JsonObjectSerializer<Baz> {

		@Override
		protected void serializeObject(Baz value, JsonGenerator jgen, SerializerProvider provider) {
		}

	}

	static class Baz {

	}

	static class CustomModule extends SimpleModule {

		private final Set<ObjectCodec> owners = new HashSet<>();

		@Override
		public void setupModule(SetupContext context) {
			this.owners.add(context.getOwner());
		}

		Set<ObjectCodec> getOwners() {
			return this.owners;
		}

	}

	@SuppressWarnings("unused")
	static class VisibilityBean {

		private String property1;

		public String property2;

		String getProperty3() {
			return null;
		}

	}

	static class Person {

		@JsonFormat(pattern = "yyyyMMdd")
		private Date birthDate;

		Date getBirthDate() {
			return this.birthDate;
		}

		void setBirthDate(Date birthDate) {
			this.birthDate = birthDate;
		}

	}

	@JsonMixin(type = Person.class)
	static class EmptyMixin {

	}

	@AutoConfigurationPackage
	static class MixinConfiguration {

	}

	@JsonComponent
	static class CircularDependencySerializer extends JsonSerializer<String> {

		CircularDependencySerializer(ObjectMapper objectMapper) {

		}

		@Override
		public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

		}

	}

	@Import(CircularDependencySerializer.class)
	@Configuration(proxyBeanMethods = false)
	static class CircularDependencySerializerConfiguration {

	}

}
