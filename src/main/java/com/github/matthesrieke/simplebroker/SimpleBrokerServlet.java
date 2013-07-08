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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(SimpleBrokerServlet.class);
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);

	@Inject
	private Set<Consumer> consumers;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		final StringEntity post = createPayload(req, resp);
		final String remoteHost = req.getRemoteHost();

		threadPool.submit(new Runnable() {
			
			@Override
			public void run() {
				for (Consumer c : consumers) {
					try {
						c.consume(post, remoteHost);
					} catch (RuntimeException e) {
						logger.warn(e.getMessage());
					}
				}				
			}
		});
		

		resp.setStatus(HttpStatus.SC_NO_CONTENT);
	}

	private StringEntity createPayload(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		Scanner sc = new Scanner(req.getInputStream());
		StringBuilder sb = new StringBuilder();

		while (sc.hasNext()) {
			sb.append(sc.nextLine());
		}

		sc.close();

		StringEntity result = new StringEntity(sb.toString(),
				ContentType.parse(req.getContentType()));
		return result;
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
