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

import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFileConsumer extends AbstractConsumer {

	private static final Logger logger = LoggerFactory.getLogger(LogFileConsumer.class);
	
	@Override
	public void consume(HttpEntity entity, String origin) {
		logger.info("Received Request from {} with content type {} and length {}",
				origin, entity.getContentType(), entity.getContentLength());
	}
	
	@Override
	protected String getTargetUrl() {
		return null;
	}
	
	public static class Module extends AbstractConsumer.Module {

		@Override
		protected void configure() {
			bindConsumer(LogFileConsumer.class);
		}
		
	}

}
