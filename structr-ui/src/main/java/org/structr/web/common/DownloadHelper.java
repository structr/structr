/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.structr.web.Importer;

//~--- classes ----------------------------------------------------------------
/**
 * Download utility class.
 */
public class DownloadHelper {

	private static final Logger logger = Logger.getLogger(DownloadHelper.class.getName());

	//~--- methods --------------------------------------------------------

	public static InputStream getInputStream(final String address) {

		try {
			final URL originalUrl = new URL(address);

			final HttpClient client = Importer.getHttpClient();

			final GetMethod get = new GetMethod(originalUrl.toString());
			get.addRequestHeader("User-Agent", "curl/7.35.0");
			get.addRequestHeader("Connection", "close");
			get.getParams().setParameter("http.protocol.single-cookie-header", true);

			get.setFollowRedirects(true);

			client.executeMethod(get);

			return get.getResponseBodyAsStream();

		} catch (MalformedURLException ex) {
			Logger.getLogger(DownloadHelper.class.getName()).log(Level.SEVERE, "Can't download content from malformed URL " + address, ex);
		} catch (IOException ex) {
			Logger.getLogger(DownloadHelper.class.getName()).log(Level.SEVERE, "Can't download content from URL " + address, ex);
		}

		return null;

	}
}
