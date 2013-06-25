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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class DynamicTargetConsumer extends AbstractConsumer {

	private URL url;
	private Timer timerDaemon;
	private static final String TARGET_URL_FILE = "/DynamicTargetConsumer.cfg";

	public DynamicTargetConsumer() {
		super();
		try {
			url = new URL("http://localhost:8082");
		} catch (MalformedURLException e) {
			logger.warn(e.getMessage());
		}
		startWatchThread();
	}

	private void startWatchThread() {
		timerDaemon = new Timer(true);
		timerDaemon.scheduleAtFixedRate(new CheckFile(), 0, 10000);
	}

	@Override
	protected String getTargetUrl() {
		synchronized (this) {
			return this.url.toExternalForm();
		}

	}

	private class CheckFile extends TimerTask {

		@Override
		public void run() {
			try {
				InputStream contents = getClass().getResourceAsStream(
						TARGET_URL_FILE);

				Scanner sc = new Scanner(contents);
				StringBuilder sb = new StringBuilder();
				while (sc.hasNext()) {
					sb.append(sc.nextLine());
				}
				sc.close();

				URL newUrl;
				newUrl = new URL(sb.toString().trim());

				synchronized (DynamicTargetConsumer.this) {
					if (!url.equals(newUrl)) {
						logger.info("Changing consumer endpoint to {}", newUrl);
						url = newUrl;
					}
				}
			} catch (RuntimeException e) {
				logger.warn(e.getMessage());
			} catch (MalformedURLException e) {
				logger.warn(e.getMessage());
			}
		}

	}
	
	public static class Module extends AbstractConsumer.Module {

		@Override
		protected void configure() {
			bindConsumer(DynamicTargetConsumer.class);
		}
		
	}

}
