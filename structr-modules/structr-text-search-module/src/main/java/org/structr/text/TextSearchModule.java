/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.text;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.GenericProperty;
import org.structr.module.StructrModule;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
public class TextSearchModule implements FulltextIndexer, StructrModule {

	private static final GenericProperty contextKey = new GenericProperty("context");

	@Override
	public void onLoad() {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
		Functions.put(licenseManager, new StopWordsFunction());
	}

	@Override
	public void addToFulltextIndex(final NodeInterface node) throws FrameworkException {
		StructrApp.getInstance(node.getSecurityContext()).processTasks(new FulltextIndexingTask(node.getUuid()));
	}

	@Override
	public GraphObjectMap getContextObject(final String searchTerm, final String text, final int contextLength) {

		final GraphObjectMap contextObject = new GraphObjectMap();
		final Set<String> contextValues    = new LinkedHashSet<>();
		final String[] searchParts         = searchTerm.split("[\\s,;]+");

		for (final String searchString : searchParts) {

			final String lowerCaseSearchString = searchString.toLowerCase();
			final String lowerCaseText         = text.toLowerCase();
			final StringBuilder wordBuffer     = new StringBuilder();
			final StringBuilder lineBuffer     = new StringBuilder();
			final int textLength               = text.length();

			// modify these parameters to tune prefix and suffix word extraction
			// loop variables
			int newlineCount = 0;
			int wordCount = 0;	// wordCount starts at 1 because we include the matching word
			int pos = -1;

			do {

				// find next occurrence
				pos = lowerCaseText.indexOf(lowerCaseSearchString, pos + 1);
				if (pos >= 0) {

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

	public Set<String> getStopWords(final String language) {
		return FulltextIndexingAgent.languageStopwordMap.get(language);
	}

	// ----- interface StructrModule -----
	@Override
	public String getName() {
		return "text-search";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
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
