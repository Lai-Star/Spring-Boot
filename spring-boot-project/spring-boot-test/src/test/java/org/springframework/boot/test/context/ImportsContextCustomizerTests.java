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

package org.springframework.boot.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import kotlin.Metadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.spockframework.runtime.model.SpecMetadata;
import spock.lang.Issue;
import spock.lang.Stepwise;

import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImportsContextCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Laurent Martelli
 */
class ImportsContextCustomizerTests {

	@Test
	void importSelectorsCouldUseAnyAnnotations() {
		assertThat(new ImportsContextCustomizer(FirstImportSelectorAnnotatedClass.class))
			.isNotEqualTo(new ImportsContextCustomizer(SecondImportSelectorAnnotatedClass.class));
	}

	@Test
	void determinableImportSelector() {
		assertThat(new ImportsContextCustomizer(FirstDeterminableImportSelectorAnnotatedClass.class))
			.isEqualTo(new ImportsContextCustomizer(SecondDeterminableImportSelectorAnnotatedClass.class));
	}

	@Test
	void customizersForTestClassesWithDifferentKotlinMetadataAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstKotlinAnnotatedTestClass.class))
			.isEqualTo(new ImportsContextCustomizer(SecondKotlinAnnotatedTestClass.class));
	}

	@Test
	void customizersForTestClassesWithDifferentSpockFrameworkAnnotationsAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstSpockFrameworkAnnotatedTestClass.class))
			.isEqualTo(new ImportsContextCustomizer(SecondSpockFrameworkAnnotatedTestClass.class));
	}

	@Test
	void customizersForTestClassesWithDifferentSpockLangAnnotationsAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstSpockLangAnnotatedTestClass.class))
			.isEqualTo(new ImportsContextCustomizer(SecondSpockLangAnnotatedTestClass.class));
	}

	@Test
	void customizersForTestClassesWithDifferentJUnitAnnotationsAreEqual() {
		assertThat(new ImportsContextCustomizer(FirstJUnitAnnotatedTestClass.class))
			.isEqualTo(new ImportsContextCustomizer(SecondJUnitAnnotatedTestClass.class));
	}

	@Test
	void customizersForClassesWithDifferentImportsAreNotEqual() {
		assertThat(new ImportsContextCustomizer(FirstAnnotatedTestClass.class))
			.isNotEqualTo(new ImportsContextCustomizer(SecondAnnotatedTestClass.class));
	}

	@Test
	void customizersForClassesWithDifferentMetaImportsAreNotEqual() {
		assertThat(new ImportsContextCustomizer(FirstMetaAnnotatedTestClass.class))
			.isNotEqualTo(new ImportsContextCustomizer(SecondMetaAnnotatedTestClass.class));
	}

	@Test
	void customizersForClassesWithDifferentAliasedImportsAreNotEqual() {
		assertThat(new ImportsContextCustomizer(FirstAliasAnnotatedTestClass.class))
			.isNotEqualTo(new ImportsContextCustomizer(SecondAliasAnnotatedTestClass.class));
	}

	@Test
	void importsCanBeScatteredOnMultipleAnnotations() {
		assertThat(new ImportsContextCustomizer(SingleImportAnnotationTestClass.class))
			.isEqualTo(new ImportsContextCustomizer(MultipleImportAnnotationTestClass.class));
	}

	@Import(TestImportSelector.class)
	@Indicator1
	static class FirstImportSelectorAnnotatedClass {

	}

	@Import(TestImportSelector.class)
	@Indicator2
	static class SecondImportSelectorAnnotatedClass {

	}

	@Import(TestDeterminableImportSelector.class)
	@Indicator1
	static class FirstDeterminableImportSelectorAnnotatedClass {

	}

	@Import(TestDeterminableImportSelector.class)
	@Indicator2
	static class SecondDeterminableImportSelectorAnnotatedClass {

	}

	@Metadata(d2 = "foo")
	@Import(TestImportSelector.class)
	static class FirstKotlinAnnotatedTestClass {

	}

	@Metadata(d2 = "bar")
	@Import(TestImportSelector.class)
	static class SecondKotlinAnnotatedTestClass {

	}

	@SpecMetadata(filename = "foo", line = 10)
	@Import(TestImportSelector.class)
	static class FirstSpockFrameworkAnnotatedTestClass {

	}

	@SpecMetadata(filename = "bar", line = 10)
	@Import(TestImportSelector.class)
	static class SecondSpockFrameworkAnnotatedTestClass {

	}

	@Stepwise
	@Import(TestImportSelector.class)
	static class FirstSpockLangAnnotatedTestClass {

	}

	@Issue("1234")
	@Import(TestImportSelector.class)
	static class SecondSpockLangAnnotatedTestClass {

	}

	@Nested
	@Import(TestImportSelector.class)
	static class FirstJUnitAnnotatedTestClass {

	}

	@Tag("test")
	@Import(TestImportSelector.class)
	static class SecondJUnitAnnotatedTestClass {

	}

	@Import({ FirstImportedClass.class, SecondImportedClass.class })
	static class SingleImportAnnotationTestClass {

	}

	@FirstMetaImport
	@Import(SecondImportedClass.class)
	static class MultipleImportAnnotationTestClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Indicator2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Import(AliasFor.class)
	public @interface AliasedImport {

		@AliasFor(annotation = Import.class)
		Class<?>[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Import(FirstImportedClass.class)
	public @interface FirstMetaImport {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Import(SecondImportedClass.class)
	public @interface SecondMetaImport {

	}

	static class FirstImportedClass {

	}

	static class SecondImportedClass {

	}

	@AliasedImport(FirstImportedClass.class)
	static class FirstAliasAnnotatedTestClass {

	}

	@AliasedImport(SecondImportedClass.class)
	static class SecondAliasAnnotatedTestClass {

	}

	@FirstMetaImport
	static class FirstMetaAnnotatedTestClass {

	}

	@SecondMetaImport
	static class SecondMetaAnnotatedTestClass {

	}

	@Import(FirstImportedClass.class)
	static class FirstAnnotatedTestClass {

	}

	@Import(SecondImportedClass.class)
	static class SecondAnnotatedTestClass {

	}

	static class TestImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata arg0) {
			return new String[] {};
		}

	}

	static class TestDeterminableImportSelector implements ImportSelector, DeterminableImports {

		@Override
		public String[] selectImports(AnnotationMetadata arg0) {
			return new String[] { TestConfig.class.getName() };
		}

		@Override
		public Set<Object> determineImports(AnnotationMetadata metadata) {
			return Collections.singleton(TestConfig.class.getName());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

	}

}
