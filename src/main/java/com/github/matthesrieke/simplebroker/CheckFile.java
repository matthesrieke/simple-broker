/**
 * Copyright (C) 2013
 * by Matthes Rieke
 *
 * Contact: http://matthesrieke.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.matthesrieke.simplebroker;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckFile extends TimerTask {
	private static final Logger logger = LoggerFactory
			.getLogger(CheckFile.class);
	private String file;
	private Callback callback;

	public CheckFile(String file, Callback cb) {
		this.file = file;
		this.callback = cb;
	}

	public void run() {
		logger.debug("Checking file for new URL");
		try {
			Set<String> newUrl = FileUtil.readConfigFilePerLine(this.file);
			logger.debug("URL from File: {}", newUrl);
			synchronized (this.callback.getMutex()) {
				if (!this.callback.getCurrentStringSet().equals(newUrl)) {
					this.callback.updateStringSet(newUrl);
					logger.info("Updating contents for {} to {}", this.file,
							Arrays.toString(newUrl.toArray()));
				}
			}
		} catch (RuntimeException e) {
			logger.warn(e.getMessage());
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
	}

	public interface Callback {
		Object getMutex();

		void updateStringSet(Set<String> paramSet);

		Set<String> getCurrentStringSet();
	}
}