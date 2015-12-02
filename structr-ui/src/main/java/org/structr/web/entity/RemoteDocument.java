/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.web.entity;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.files.text.FulltextIndexingTask;
import org.structr.files.text.FulltextTokenizer;
import org.structr.web.Importer;

/**
 *
 *
 */
public class RemoteDocument extends AbstractNode implements Indexable {

	private static final Logger logger = Logger.getLogger(RemoteDocument.class.getName());

	public static final Property<String> url                     = new StringProperty("url");
	public static final Property<Long> checksum                  = new LongProperty("checksum").indexed().unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                = new IntProperty("version").indexed().readOnly();

	public static final View publicView = new View(RemoteDocument.class, PropertyView.Public, type, name, contentType, url, owner);
	public static final View uiView = new View(RemoteDocument.class, PropertyView.Ui, type, contentType, url, checksum, version, cacheForSeconds, owner, extractedContent, indexedWords);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer)) {

			//StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
			return true;
		}

		return false;
	}

	@Override
	public void onNodeCreation() {

		//final String uuid = getUuid();

		try {

		} catch (Throwable t) {

		}
	}

	@Override
	public void onNodeDeletion() {

		try {

		} catch (Throwable t) {

		}

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
			
		} catch (Throwable t) {

		}

	}

	@Export
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		// TODO: Move this method (and the corresponding methods in FileBase.java) to a common helper class
		
		final String text = getProperty(extractedContent);
		if (text != null) {

			final String[] searchParts         = searchTerm.split("[\\s]+");
			final GenericProperty contextKey   = new GenericProperty("context");
			final GraphObjectMap contextObject = new GraphObjectMap();
			final Set<String> contextValues    = new LinkedHashSet<>();

			for (final String searchString : searchParts) {

				final String lowerCaseSearchString = searchString.toLowerCase();
				final String lowerCaseText         = text.toLowerCase();
				final StringBuilder wordBuffer     = new StringBuilder();
				final StringBuilder lineBuffer     = new StringBuilder();
				final int textLength               = text.length();

				/*
				 * we take an average word length of 8 characters, multiply
				 * it by the desired prefix and suffix word count, add 20%
				 * and try to extract up to prefixLength words.
				 */

				// modify these parameters to tune prefix and suffix word extraction
				// loop variables
				int newlineCount = 0;
				int wordCount    = 0;	// wordCount starts at 1 because we include the matching word
				int pos          = -1;

				do {

					// find next occurrence
					pos = lowerCaseText.indexOf(lowerCaseSearchString, pos + 1);
					if (pos > 0) {

						lineBuffer.setLength(0);
						wordBuffer.setLength(0);

						wordCount    = 0;
						newlineCount = 0;

						// fetch context words before search hit
						for (int i=pos; i>=0; i--) {

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
						for (int i=pos+1; i<textLength; i++) {

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

		return null;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(RemoteDocument.version);

		unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			setProperty(RemoteDocument.version, 1);

		} else {

			setProperty(RemoteDocument.version, _version + 1);
		}
	}

	@Override
	public InputStream getInputStream() {

		try {
			final URL originalUrl = new URL(getProperty(url));

			HttpClient client = Importer.getHttpClient();

			GetMethod get = new GetMethod(originalUrl.toString());
			get.addRequestHeader("User-Agent", "curl/7.35.0");
			get.addRequestHeader("Connection", "close");
			get.getParams().setParameter("http.protocol.single-cookie-header", true);

			get.setFollowRedirects(true);

			client.executeMethod(get);

			return get.getResponseBodyAsStream();

		} catch (Throwable t) {

		}

		return null;		


	}

	// ----- private methods -----

	private int flushWordBuffer(final StringBuilder lineBuffer, final StringBuilder wordBuffer, final boolean prepend) {

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
