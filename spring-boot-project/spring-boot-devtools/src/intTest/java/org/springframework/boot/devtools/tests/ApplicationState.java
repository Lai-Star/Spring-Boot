/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.time.Instant;

import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;

/**
 * State of an application.
 *
 * @author Andy Wilkinson
 */
final class ApplicationState {

	private final Instant launchTime;

	private final Integer serverPort;

	private final FileContents out;

	private final FileContents err;

	ApplicationState(File serverPortFile, LaunchedJvm jvm) {
		this(serverPortFile, jvm.getStandardOut(), jvm.getStandardError(), jvm.getLaunchTime());
	}

	ApplicationState(File serverPortFile, LaunchedApplication application) {
		this(serverPortFile, application.getStandardOut(), application.getStandardError(), application.getLaunchTime());
	}

	private ApplicationState(File serverPortFile, File out, File err, Instant launchTime) {
		this.serverPort = new FileContents(serverPortFile).get(Integer::parseInt);
		this.out = new FileContents(out);
		this.err = new FileContents(err);
		this.launchTime = launchTime;
	}

	boolean hasServerPort() {
		return this.serverPort != null;
	}

	int getServerPort() {
		return this.serverPort;
	}

	@Override
	public String toString() {
		return String.format("Application launched at %s produced output:%n%s%n%s", this.launchTime, this.out,
				this.err);
	}

}
