/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

	/**
	 * A SAXReader that will not resolve anything the document points at. The three providers each built
	 * their own and each set only setIncludeExternalDTDDeclarations(false), which turns off the external
	 * DTD subset but leaves entity declarations in an internal subset alone -- so a response could still
	 * name a local file or a URL and have the parser fetch it. None of the three geocoding formats uses a
	 * DOCTYPE, so the whole construct is refused here instead, which is the same line the BPMN importer
	 * and XmlFunction take.
	 *
	 * <p>The response is a foreign document in every case: it arrives over the network from a service
	 * whose operator is not us.
	 */
	protected SAXReader newSecureSAXReader() throws IOException {

		final SAXReader reader = new SAXReader();

		try {

			reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
			reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		} catch (SAXException sex) {

			// a parser that cannot be told to refuse a DOCTYPE is one we must not feed a foreign document to
			throw new IOException("Unable to configure a safe XML parser for the geocoding response", sex);
		}

		reader.setIncludeExternalDTDDeclarations(false);
		reader.setIncludeInternalDTDDeclarations(false);

		return reader;
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
