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

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public abstract class AbstractConsumer implements Consumer {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractConsumer.class);
	private HttpClient client;

	public AbstractConsumer() {
		this.client = createClient();
	}
	
	private HttpClient createClient() {
		DefaultHttpClient result = new DefaultHttpClient();
		return result;
	}

	@Override
	public void consume(HttpEntity entity, String origin) {
		HttpPost post = new HttpPost(getTargetUrl());
		post.setEntity(entity);
		try {
			this.client.execute(post);
		} catch (ClientProtocolException e) {
			logger.warn(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
	}
	
	protected abstract String getTargetUrl();
	
	public abstract static class Module extends AbstractModule {
		
		protected void bindConsumer(Class<? extends Consumer> c) {
			Multibinder<Consumer> binder = Multibinder.newSetBinder(binder(), Consumer.class);
		    binder.addBinding().to(c);		
		}
		
	}
}
