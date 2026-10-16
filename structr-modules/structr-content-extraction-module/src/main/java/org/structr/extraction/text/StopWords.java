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
package org.structr.extraction.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stop word lists for about 30 languages, read from the bundled stop-words.zip on first use.
 *
 * The archive holds one or more files per language, named like stop-words_english_1_en.txt; the two
 * letters before the extension are the language code, and all files of a language are merged.
 */
public final class StopWords {

	private static final Logger logger   = LoggerFactory.getLogger(StopWords.class);
	private static final String RESOURCE = "/stopwords/stop-words.zip";

	/** The archive files Czech under "cz"; the ISO 639-1 code is "cs". */
	private static final Map<String, String> ALIASES = Map.of("cs", "cz");

	private static Map<String, Set<String>> languages = null;

	private StopWords() {
	}

	/**
	 * The stop words of a language, or an empty set for a language without a list.
	 *
	 * @param language a two-letter language code; longer tags such as "en-US" use their first two letters
	 */
	public static Set<String> forLanguage(final String language) {

		if (language == null || language.length() < 2) {

			return Set.of();
		}

		final String code = language.substring(0, 2).toLowerCase(Locale.ROOT);

		return all().getOrDefault(ALIASES.getOrDefault(code, code), Set.of());
	}

	/**
	 * The language codes that have a stop word list.
	 */
	public static Set<String> languages() {

		return all().keySet();
	}

	// ----- private methods -----
	private static synchronized Map<String, Set<String>> all() {

		if (languages == null) {

			languages = load();
		}

		return languages;
	}

	private static Map<String, Set<String>> load() {

		final Map<String, Set<String>> map = new HashMap<>();

		try (final InputStream in = StopWords.class.getResourceAsStream(RESOURCE)) {

			if (in == null) {

				logger.warn("Stop word lists not found at {}", RESOURCE);

				return Map.of();
			}

			try (final ZipInputStream zip = new ZipInputStream(in)) {

				for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {

					final String code = languageCode(entry);
					if (code != null) {

						readWords(zip, map.computeIfAbsent(code, k -> new LinkedHashSet<>()));
					}
				}
			}

		} catch (final IOException ex) {

			logger.warn("Unable to read the stop word lists: {}", ex.getMessage());
		}

		map.replaceAll((code, words) -> Collections.unmodifiableSet(words));

		return Collections.unmodifiableMap(map);
	}

	private static String languageCode(final ZipEntry entry) {

		final String name = entry.getName();
		final int dot     = name.lastIndexOf(".txt");
		final int sep     = name.lastIndexOf('_');

		if (entry.isDirectory() || dot < 0 || sep < 0 || dot - sep != 3) {

			return null;
		}

		return name.substring(sep + 1, dot).toLowerCase(Locale.ROOT);
	}

	private static void readWords(final InputStream zip, final Set<String> words) throws IOException {

		// the reader must not close the zip stream, which holds the remaining entries
		final BufferedReader reader = new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));

		for (String line = reader.readLine(); line != null; line = reader.readLine()) {

			final String word = line.strip();
			if (!word.isEmpty()) {

				words.add(word);
			}
		}
	}
}
