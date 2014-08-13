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
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.matthesrieke.simplebroker.CheckFile.Callback;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

@Singleton
public class SimpleBrokerServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4589872023160154399L;
	private static final Logger logger = LoggerFactory
			.getLogger(SimpleBrokerServlet.class);
	private static final String PRODUCERS_FILE = "/allowed_producers.cfg";
	private ExecutorService executor;

	@Inject
	private Set<Consumer> consumers;
	private Set<String> allowedProducers;
	private Timer timerDaemon;

	public SimpleBrokerServlet() {
		this.executor = Executors.newCachedThreadPool();
	}

	public void init() throws ServletException {
		super.init();
		try {
			this.allowedProducers = FileUtil
					.readConfigFilePerLine(PRODUCERS_FILE);
		} catch (IOException e) {
			throw new ServletException(e);
		}

		startWatchThread();
	}

	@Override
	public void destroy() {
		super.destroy();

		this.timerDaemon.cancel();

		for (Consumer string : consumers) {
			string.destroy();
		}
	}

	private void startWatchThread() {
		this.timerDaemon = new Timer(true);
		this.timerDaemon.scheduleAtFixedRate(new CheckFile(PRODUCERS_FILE,
				new Callback() {

					@Override
					public void updateStringSet(Set<String> newSet) {
						synchronized (SimpleBrokerServlet.this) {
							SimpleBrokerServlet.this.allowedProducers = newSet;
						}
					}

					@Override
					public Object getMutex() {
						return SimpleBrokerServlet.this;
					}

					@Override
					public Set<String> getCurrentStringSet() {
						return SimpleBrokerServlet.this.allowedProducers;
					}
				}), 0L, 60000L);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (verifyRemoteHost(req.getRemoteHost())) {
			final String content;
			final ContentType type;
			final String remoteHost;
			try {
				content = readContent(req);
				type = ContentType.parse(req.getContentType());
				remoteHost = req.getRemoteHost();
			} catch (IOException e) {
				logger.warn(e.getMessage());
				return;
			}

			this.executor.submit(new Runnable() {

				public void run() {
					for (Consumer c : consumers)
						try {
							c.consume(content, type, remoteHost);
						} catch (RuntimeException e) {
							logger.warn(e.getMessage());
						} catch (IOException e) {
							logger.warn(e.getMessage());
						}
				}
			});
		} else {
			logger.info("Host {} is not whitelisted. Ignoring request.",
					req.getRemoteHost());
		}

		resp.setStatus(HttpStatus.SC_NO_CONTENT);
	}

	public synchronized Set<String> getAllowedProducers() {
		return this.allowedProducers;
	}

	private synchronized boolean verifyRemoteHost(String remoteHost) {
		for (String prod : getAllowedProducers()) {
			if (remoteHost.contains(prod)) {
				return true;
			}
		}
		return false;
	}

	protected String readContent(HttpServletRequest req) throws IOException {
		String enc = req.getCharacterEncoding();
		Scanner sc = new Scanner(req.getInputStream(), enc == null ? "utf-8"
				: enc);
		StringBuilder sb = new StringBuilder();

		while (sc.hasNext()) {
			sb.append(sc.nextLine());
		}

		sc.close();
		return sb.toString();
	}

	public static class SimpleBrokerGuiceServletConfig extends
			GuiceServletContextListener {

		@Override
		protected Injector getInjector() {
			ServiceLoader<Module> loader = ServiceLoader.load(Module.class);

			List<Module> modules = new ArrayList<Module>();
			for (Module module : loader) {
				modules.add(module);
			}

			modules.add(new ServletModule() {

				@Override
				protected void configureServlets() {
					serve("/*").with(SimpleBrokerServlet.class);
				}

			});

			return Guice.createInjector(modules);
		}
	}

}
