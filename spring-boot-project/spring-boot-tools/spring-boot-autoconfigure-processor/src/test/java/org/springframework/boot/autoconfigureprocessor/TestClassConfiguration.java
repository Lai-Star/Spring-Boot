/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import org.springframework.boot.autoconfigureprocessor.TestConditionalOnWebApplication.Type;

/**
 * Test configuration with an annotated class.
 *
 * @author Madhura Bhave
 */
@TestConditionalOnClass(name = { "org.springframework.foo", "java.io.InputStream" },
		value = TestClassConfiguration.Nested.class)
@TestConditionalOnBean(type = "java.io.OutputStream")
@TestConditionalOnSingleCandidate(type = "java.io.OutputStream")
@TestConditionalOnWebApplication(type = Type.SERVLET)
public class TestClassConfiguration {

	@TestAutoConfigureOrder
	static class Nested {

	}

}
