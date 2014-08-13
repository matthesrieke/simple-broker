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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import com.github.matthesrieke.simplebroker.CheckFile.Callback;

public class DynamicTargetConsumer extends AbstractConsumer {

	private Set<String> urls;
	private Timer timerDaemon;
	private static final String TARGET_URL_FILE = "/DynamicTargetConsumer.cfg";

	public DynamicTargetConsumer() {
		try {
			this.urls = FileUtil.readConfigFilePerLine(TARGET_URL_FILE);
		} catch (RuntimeException e) {
			logger.warn(e.getMessage());
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		startWatchThread();
	}

	@Override
	public void destroy() {
		super.destroy();

		this.timerDaemon.cancel();
	}

	private void startWatchThread() {
		this.timerDaemon = new Timer(true);
		this.timerDaemon.scheduleAtFixedRate(new CheckFile(TARGET_URL_FILE,
				new Callback() {

					@Override
					public void updateStringSet(Set<String> newUrls) {
						synchronized (DynamicTargetConsumer.this) {
							urls = newUrls;
						}
					}

					@Override
					public Object getMutex() {
						return DynamicTargetConsumer.this;
					}

					@Override
					public Set<String> getCurrentStringSet() {
						return DynamicTargetConsumer.this.urls;
					}
				}), 0L, 60000L);
	}

	@Override
	protected List<String> getTargetUrls() {
		synchronized (this) {
			return new ArrayList<String>(this.urls);
		}

	}

	public static class Module extends AbstractConsumer.Module {

		@Override
		protected void configure() {
			bindConsumer(DynamicTargetConsumer.class);
		}

	}

}
