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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;
import java.util.Timer;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public abstract class AbstractConsumer implements Consumer {

	private static final String TRUSTED_HOSTS_CFG_FILE = "/AbstractConsumer_trustedHosts.cfg";
	protected static final Logger logger = LoggerFactory
			.getLogger(AbstractConsumer.class);
	private HttpClient client;
	private Set<String> trustedHosts;
	private Timer timerDaemon;

	public AbstractConsumer() {
		try {
			this.client = createClient();
			this.trustedHosts = FileUtil
					.readConfigFilePerLine(TRUSTED_HOSTS_CFG_FILE);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		startWatchThread();
	}

	private void startWatchThread() {
		this.timerDaemon = new Timer(true);
		this.timerDaemon
				.scheduleAtFixedRate(new CheckFile(
						TRUSTED_HOSTS_CFG_FILE, new LocalCallback()),
						0L, 60000L);
	}

	protected abstract Collection<String> getTargetUrls();

	protected HttpClient createClient() throws Exception {
		DefaultHttpClient result = new DefaultHttpClient();
		SchemeRegistry sr = result.getConnectionManager().getSchemeRegistry();

		SSLSocketFactory sslsf = new SSLSocketFactory(new TrustStrategy() {

			@Override
			public boolean isTrusted(X509Certificate[] arg0, String arg1)
					throws CertificateException {
				return true;
			}
		}, new AllowTrustedHostNamesVerifier());

		Scheme httpsScheme2 = new Scheme("https", 443, sslsf);
		sr.register(httpsScheme2);

		return result;
	}

	public void destroy() {
		this.timerDaemon.cancel();
	}

	public Set<String> getTrustedHosts() {
		return this.trustedHosts;
	}

	@Override
	public synchronized void consume(String cotent, ContentType contentType,
			String origin) {
		for (String url : getTargetUrls()) {
			HttpPost post = new HttpPost(url);

			post.setEntity(createEntity(cotent, contentType));
			HttpResponse response = null;
			try {
				response = this.client.execute(post);
				logger.info("Content posted to " + url);
			} catch (ClientProtocolException e) {
				logger.warn(e.getMessage(), e);
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			} finally {
				if ((response != null) && (response.getEntity() != null))
					try {
						EntityUtils.consume(response.getEntity());
					} catch (IOException e) {
						logger.warn(e.getMessage(), e);
					}
			}
		}
	}

	private HttpEntity createEntity(String cotent, ContentType contentType) {
		StringEntity result = new StringEntity(cotent, contentType);
		return result;
	}

	public class AllowTrustedHostNamesVerifier implements X509HostnameVerifier {
		private StrictHostnameVerifier delegate;

		public AllowTrustedHostNamesVerifier() {
			this.delegate = new StrictHostnameVerifier();
		}

		public boolean verify(String hostname, SSLSession session) {
			boolean result = this.delegate.verify(hostname, session);
			if ((!result)
					&& (AbstractConsumer.this.trustedHosts.contains(hostname))) {
				return true;
			}

			return result;
		}

		public void verify(String host, SSLSocket ssl) throws IOException {
			try {
				this.delegate.verify(host, ssl);
			} catch (IOException e) {
				if (!AbstractConsumer.this.getTrustedHosts().contains(host))
					throw e;
			}
		}

		public void verify(String host, X509Certificate cert)
				throws SSLException {
			try {
				this.delegate.verify(host, cert);
			} catch (SSLException e) {
				if (!AbstractConsumer.this.getTrustedHosts().contains(host))
					throw e;
			}
		}

		public void verify(String host, String[] cns, String[] subjectAlts)
				throws SSLException {
			try {
				this.delegate.verify(host, cns, subjectAlts);
			} catch (SSLException e) {
				if (!AbstractConsumer.this.getTrustedHosts().contains(host))
					throw e;
			}
		}
	}

	public class LocalCallback implements CheckFile.Callback {

		public void updateStringSet(Set<String> newUrls) {
			synchronized (AbstractConsumer.this) {
				AbstractConsumer.this.trustedHosts = newUrls;
			}
		}

		public Object getMutex() {
			return AbstractConsumer.this;
		}

		public Set<String> getCurrentStringSet() {
			return AbstractConsumer.this.trustedHosts;
		}
	}

	public abstract static class Module extends AbstractModule {

		protected void bindConsumer(Class<? extends Consumer> c) {
			Multibinder<Consumer> binder = Multibinder.newSetBinder(binder(),
					Consumer.class);
			binder.addBinding().to(c);
		}

	}
}
