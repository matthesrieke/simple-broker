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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;

public class DynamicTargetConsumer extends AbstractConsumer {

	private List<String> urls;
	private Timer timerDaemon;
	private static final String TARGET_URL_FILE = "/DynamicTargetConsumer.cfg";

	public DynamicTargetConsumer() {
		super();

		try {
			urls = readUrlsFromFile();
		} catch (RuntimeException e) {
			logger.warn(e.getMessage());
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		startWatchThread();
	}

	private void startWatchThread() {
		timerDaemon = new Timer(false);
		timerDaemon.scheduleAtFixedRate(new CheckFile(), 0, 60000);
	}

	@Override
	protected List<String> getTargetUrl() {
		synchronized (this) {
			return this.urls;
		}

	}

	@Override
	protected HttpClient createClient() throws Exception {
		// FIXME kind of bad practice...
		SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {
			@Override
			public boolean isTrusted(final X509Certificate[] chain,
					String authType) {
				return true;
			}
		}, new AllowAllHostnameVerifier());

		BasicClientConnectionManager cm = new BasicClientConnectionManager();
		Scheme httpsScheme = new Scheme("https", 443, sslsf);
		cm.getSchemeRegistry().register(httpsScheme);

		return new DefaultHttpClient(cm);
	}
	
	public static void main(String[] args) {
		new DynamicTargetConsumer();
	}
	
	protected List<String> readUrlsFromFile() throws IOException {
		URL resURL = getClass().getResource(TARGET_URL_FILE);
		URLConnection resConn = resURL.openConnection();
		resConn.setUseCaches(false);
		InputStream contents = resConn.getInputStream();

		List<String> result = new ArrayList<String>();

		Scanner sc = new Scanner(contents);
		while (sc.hasNext()) {
			URL newUrl = new URL(sc.nextLine().trim());
			result.add(newUrl.toExternalForm());
		}
		sc.close();

		return result;
	}

	private class CheckFile extends TimerTask {

		@Override
		public void run() {
			logger.debug("Checking file for new URLs");
			try {
				List<String> newUrl = readUrlsFromFile();
				logger.debug("URL from File: {}", newUrl);
				synchronized (DynamicTargetConsumer.this) {
					if (!urls.equals(newUrl)) {
						logger.info("Changing consumer endpoints to {}", newUrl);
						urls = newUrl;
					}
				}
			} catch (RuntimeException e) {
				logger.warn(e.getMessage());
			} catch (IOException e) {
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
