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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.text.FulltextTokenizer;
import org.structr.web.importer.Importer;

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
			logger.log(Level.SEVERE, "Can't download content from malformed URL " + address, ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Can't download content from URL " + address, ex);
		}

		return null;

	}

	public static GraphObjectMap getContextObject(final String searchTerm, final String text, final int contextLength) {

		final GraphObjectMap contextObject = new GraphObjectMap();
		final Set<String> contextValues = new LinkedHashSet<>();
		final String[] searchParts = searchTerm.split("[\\s,;]+");
		final GenericProperty contextKey = new GenericProperty("context");

		for (final String searchString : searchParts) {

			final String lowerCaseSearchString = searchString.toLowerCase();
			final String lowerCaseText = text.toLowerCase();
			final StringBuilder wordBuffer = new StringBuilder();
			final StringBuilder lineBuffer = new StringBuilder();
			final int textLength = text.length();

			/*
				 * we take an average word length of 8 characters, multiply
				 * it by the desired prefix and suffix word count, add 20%
				 * and try to extract up to prefixLength words.
			 */
			// modify these parameters to tune prefix and suffix word extraction
			// loop variables
			int newlineCount = 0;
			int wordCount = 0;	// wordCount starts at 1 because we include the matching word
			int pos = -1;

			do {

				// find next occurrence
				pos = lowerCaseText.indexOf(lowerCaseSearchString, pos + 1);
				if (pos > 0) {

					lineBuffer.setLength(0);
					wordBuffer.setLength(0);

					wordCount = 0;
					newlineCount = 0;

					// fetch context words before search hit
					for (int i = pos; i >= 0; i--) {

						final char c = text.charAt(i);

						if (!Character.isAlphabetic(c) && !Character.isDigit(c) && !FulltextTokenizer.SpecialChars.contains(c)) {

							wordCount += flushWordBuffer(lineBuffer, wordBuffer, true);

							// store character in buffer
							wordBuffer.insert(0, c);

							if (c == '\n') {

								// increase newline count
								newlineCount++;

							} else {

								// reset newline count
								newlineCount = 0;
							}

							// paragraph boundary reached
							if (newlineCount > 1) {
								break;
							}

							// stop if we collected half of the desired word count
							if (wordCount > contextLength / 2) {
								break;
							}

						} else {

							// store character in buffer
							wordBuffer.insert(0, c);

							// reset newline count
							newlineCount = 0;
						}
					}

					wordCount += flushWordBuffer(lineBuffer, wordBuffer, true);

					wordBuffer.setLength(0);

					// fetch context words after search hit
					for (int i = pos + 1; i < textLength; i++) {

						final char c = text.charAt(i);

						if (!Character.isAlphabetic(c) && !Character.isDigit(c) && !FulltextTokenizer.SpecialChars.contains(c)) {

							wordCount += flushWordBuffer(lineBuffer, wordBuffer, false);

							// store character in buffer
							wordBuffer.append(c);

							if (c == '\n') {

								// increase newline count
								newlineCount++;

							} else {

								// reset newline count
								newlineCount = 0;
							}

							// paragraph boundary reached
							if (newlineCount > 1) {
								break;
							}

							// stop if we collected enough words
							if (wordCount > contextLength) {
								break;
							}

						} else {

							// store character in buffer
							wordBuffer.append(c);

							// reset newline count
							newlineCount = 0;
						}
					}

					wordCount += flushWordBuffer(lineBuffer, wordBuffer, false);

					// replace single newlines with space
					contextValues.add(lineBuffer.toString().trim());
				}

			} while (pos >= 0);
		}

		contextObject.put(contextKey, contextValues);

		return contextObject;

	}

	//~--- private methods --------------------------------------------------------
	private static int flushWordBuffer(final StringBuilder lineBuffer, final StringBuilder wordBuffer, final boolean prepend) {

		int wordCount = 0;

		if (wordBuffer.length() > 0) {

			final String word = wordBuffer.toString().replaceAll("[\\n\\t]+", " ");
			if (StringUtils.isNotBlank(word)) {

				if (prepend) {

					lineBuffer.insert(0, word);

				} else {

					lineBuffer.append(word);
				}

				// increase word count
				wordCount = 1;
			}

			wordBuffer.setLength(0);
		}

		return wordCount;
	}

}