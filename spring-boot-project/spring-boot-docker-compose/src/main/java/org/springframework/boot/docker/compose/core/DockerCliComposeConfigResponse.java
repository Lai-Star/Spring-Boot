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

package org.springframework.boot.docker.compose.core;

import java.util.Map;

/**
 * Response from {@link DockerCliCommand.ComposeConfig docker compose config}.
 *
 * @param name project name
 * @param services services
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
record DockerCliComposeConfigResponse(String name, Map<String, DockerCliComposeConfigResponse.Service> services) {

	/**
	 * Docker compose service.
	 *
	 * @param image the image
	 */
	record Service(String image) {

	}

}
