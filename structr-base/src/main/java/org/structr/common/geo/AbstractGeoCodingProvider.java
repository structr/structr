/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.geo;

import org.apache.commons.jxpath.JXPathContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Abstract base class for geocoding providers.
 *
 *
 */
public abstract class AbstractGeoCodingProvider implements GeoCodingProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGeoCodingProvider.class.getName());
	protected String apiKey            = null;

	public AbstractGeoCodingProvider() {
		this.apiKey = Settings.GeocodingApiKey.getValue();
	}

	protected <T> T extract(Map source, String path, Class<T> type) {

		JXPathContext context = JXPathContext.newContext(source);
		T value               = (T)context.getValue(path);

		return value;
	}

	protected String encodeURL(String source) {

		try {
			return URLEncoder.encode(source, "UTF-8");

		} catch (UnsupportedEncodingException ex) {

			logger.warn("Unsupported Encoding", ex);
		}

		// fallback, unencoded
		return source;
	}
}
