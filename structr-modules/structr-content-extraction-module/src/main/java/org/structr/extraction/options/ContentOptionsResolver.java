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
package org.structr.extraction.options;

import org.structr.core.traits.StructrTraits;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.api.options.SizeUnit;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.api.options.SplitterType;
import org.structr.extraction.api.processing.AutoExtractionDecision;
import org.structr.extraction.configuration.ContentConfigurations;
import org.structr.web.entity.AbstractFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Decides whether a file is extracted automatically, and with which options.
 *
 * Both come from the nearest content extraction configuration on the file's path (its own, or one of
 * its folders'). That configuration applies completely: options it leaves empty use the settings, not
 * the values of configurations further up. Per-call options override everything.
 */
public final class ContentOptionsResolver {

	// option names, shared with the script functions
	public static final String SPLITTER             = "splitter";
	public static final String CHUNK_SIZE           = "chunkSize";
	public static final String CHUNK_OVERLAP        = "chunkOverlap";
	public static final String SIZE_UNIT            = "sizeUnit";
	public static final String SEPARATORS           = "separators";
	public static final String INCLUDE_HEADING_PATH = "includeHeadingPath";
	public static final String TOKEN_ENCODING       = "tokenEncoding";
	public static final String OCR                  = "ocr";
	public static final String OCR_LANGUAGES        = "ocrLanguages";
	public static final String MAX_CHARACTERS       = "maxCharacters";
	public static final String INCLUDE_EMBEDDED     = "includeEmbedded";
	public static final String INCLUDE_IMAGES       = "includeImages";

	/** The options that can be overridden per call. */
	public static final Set<String> OPTIONS = Set.of(SPLITTER, CHUNK_SIZE, CHUNK_OVERLAP, SIZE_UNIT, SEPARATORS, INCLUDE_HEADING_PATH, TOKEN_ENCODING, OCR, OCR_LANGUAGES, MAX_CHARACTERS, INCLUDE_EMBEDDED, INCLUDE_IMAGES);

	private static final String CALL    = "call";
	private static final String SETTING = "setting";
	private static final String CONFIG  = "config:";

	private ContentOptionsResolver() {
	}

	/**
	 * Whether the file is extracted automatically: the nearest content extraction configuration on its
	 * path decides (attached to the file itself, or to one of its folders); without one, it is not.
	 *
	 * For a Folder, this tells whether the files directly in it are extracted.
	 */
	public static AutoExtractionDecision shouldExtract(final AbstractFile node) {

		final ContentConfigurations.Found found = ContentConfigurations.nearest(node);
		if (found != null) {

			final String source = found.target().is(StructrTraits.FOLDER) ? "folder" : "file";

			return new AutoExtractionDecision(found.configuration().isEnabled(), source, found.configuration().getUuid(), found.targetPath());
		}

		return new AutoExtractionDecision(false, "default", null, null);
	}

	/**
	 * The effective options for a file (or, for a Folder, for the files directly in it).
	 *
	 * @param overrides per-call options by name (see {@link #OPTIONS}); may be empty
	 * @throws IllegalArgumentException if an override has an invalid value
	 */
	public static EffectiveContentOptions resolve(final AbstractFile file, final Map<String, Object> overrides) {

		final Map<String, EffectiveContentOptions.Value> values = new LinkedHashMap<>();
		final ExtractionOptions extraction                     = ExtractionOptions.fromSettings();
		final SplitOptions split                               = SplitOptions.fromSettings();
		final List<String> warnings                            = new ArrayList<>();

		// settings first, then the nearest configuration, then the call
		for (final Map.Entry<String, Object> setting : defaults(extraction, split).entrySet()) {

			values.put(setting.getKey(), new EffectiveContentOptions.Value(setting.getValue(), SETTING));
		}

		applyConfiguration(file, values);

		for (final Map.Entry<String, Object> override : overrides.entrySet()) {

			if (!OPTIONS.contains(override.getKey())) {

				throw new IllegalArgumentException("Unknown option '" + override.getKey() + "'.");
			}

			values.put(override.getKey(), new EffectiveContentOptions.Value(override.getValue(), CALL));
		}

		apply(values, extraction, split);
		clampOverlap(values, split, warnings);

		return new EffectiveContentOptions(extraction, split.validate(), values, warnings, hash(values));
	}

	/**
	 * The value of every option as given by the contentextraction.* settings, by option name.
	 */
	public static Map<String, Object> defaults() {

		return defaults(ExtractionOptions.fromSettings(), SplitOptions.fromSettings());
	}

	// ----- private methods -----
	private static Map<String, Object> defaults(final ExtractionOptions extraction, final SplitOptions split) {

		final Map<String, Object> defaults = new LinkedHashMap<>();

		defaults.put(SPLITTER,             split.getSplitter().getValue());
		defaults.put(CHUNK_SIZE,           split.getChunkSize());
		defaults.put(CHUNK_OVERLAP,        split.getChunkOverlap());
		defaults.put(SIZE_UNIT,            split.getSizeUnit().getValue());
		defaults.put(SEPARATORS,           null);
		defaults.put(INCLUDE_HEADING_PATH, split.isIncludeHeadingPath());
		defaults.put(TOKEN_ENCODING,       split.getTokenEncoding());
		defaults.put(OCR,                  extraction.getOcrMode().getValue());
		defaults.put(OCR_LANGUAGES,        extraction.getOcrLanguages());
		defaults.put(MAX_CHARACTERS,       extraction.getMaxCharacters());
		defaults.put(INCLUDE_EMBEDDED,     extraction.isIncludeEmbedded());
		defaults.put(INCLUDE_IMAGES,       extraction.isIncludeImages());

		return defaults;
	}

	/**
	 * The nearest configuration applies completely: options it leaves empty keep their setting, they are
	 * not taken from configurations further up.
	 */
	private static void applyConfiguration(final AbstractFile file, final Map<String, EffectiveContentOptions.Value> values) {

		final ContentConfigurations.Found found = ContentConfigurations.nearest(file);
		if (found != null) {

			for (final Map.Entry<String, Object> option : found.configuration().getOptionValues().entrySet()) {

				values.put(option.getKey(), new EffectiveContentOptions.Value(option.getValue(), CONFIG + found.targetPath()));
			}
		}
	}

	private static void apply(final Map<String, EffectiveContentOptions.Value> values, final ExtractionOptions extraction, final SplitOptions split) {

		split.setSplitter(SplitterType.fromValue(string(values, SPLITTER)));
		split.setChunkSize(integer(values, CHUNK_SIZE, 1));
		split.setChunkOverlap(integer(values, CHUNK_OVERLAP, 0));
		split.setSizeUnit(SizeUnit.fromValue(string(values, SIZE_UNIT)));
		split.setSeparators(strings(values, SEPARATORS));
		split.setIncludeHeadingPath(bool(values, INCLUDE_HEADING_PATH));
		split.setTokenEncoding(string(values, TOKEN_ENCODING));

		extraction.setOcrMode(OcrMode.fromValue(string(values, OCR)));
		extraction.setOcrLanguages(string(values, OCR_LANGUAGES));
		extraction.setMaxCharacters(integer(values, MAX_CHARACTERS, 1));
		extraction.setIncludeEmbedded(bool(values, INCLUDE_EMBEDDED));
		extraction.setIncludeImages(bool(values, INCLUDE_IMAGES));
	}

	/**
	 * Values come from different places (a configuration can set the overlap while the size comes from
	 * the settings or the call), so they may not fit together. An overlap that is not smaller than the size is reduced instead of failing,
	 * since automatic extraction would otherwise fail for every file below the configuration.
	 */
	private static void clampOverlap(final Map<String, EffectiveContentOptions.Value> values, final SplitOptions split, final List<String> warnings) {

		if (split.getChunkOverlap() >= split.getChunkSize()) {

			final int reduced = split.getChunkSize() / 4;

			warnings.add("chunkOverlap " + split.getChunkOverlap() + " (" + values.get(CHUNK_OVERLAP).source() + ") is not smaller than chunkSize " + split.getChunkSize()
				+ " (" + values.get(CHUNK_SIZE).source() + "), reduced to " + reduced + ".");

			split.setChunkOverlap(reduced);
			values.put(CHUNK_OVERLAP, new EffectiveContentOptions.Value(reduced, values.get(CHUNK_OVERLAP).source() + " (reduced)"));
		}
	}

	private static String hash(final Map<String, EffectiveContentOptions.Value> values) {

		final StringBuilder canonical = new StringBuilder();

		for (final Map.Entry<String, EffectiveContentOptions.Value> entry : new TreeMap<>(values).entrySet()) {

			canonical.append(entry.getKey()).append('=').append(entry.getValue().value()).append('\n');
		}

		try {

			final byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(digest, 0, 16);

		} catch (final NoSuchAlgorithmException ex) {

			// every Java runtime is required to provide SHA-256
			throw new IllegalStateException(ex);
		}
	}

	private static String string(final Map<String, EffectiveContentOptions.Value> values, final String name) {

		final Object value = values.get(name).value();
		if (value == null || value.toString().isBlank()) {

			throw new IllegalArgumentException("Option '" + name + "' must not be empty.");
		}

		return value.toString().trim();
	}

	private static int integer(final Map<String, EffectiveContentOptions.Value> values, final String name, final int minimum) {

		final Object value = values.get(name).value();
		if (value instanceof Number number && number.doubleValue() == Math.floor(number.doubleValue()) && number.intValue() >= minimum) {

			return number.intValue();
		}

		throw new IllegalArgumentException("Option '" + name + "' must be a whole number of at least " + minimum + ", got '" + value + "'.");
	}

	private static boolean bool(final Map<String, EffectiveContentOptions.Value> values, final String name) {

		final Object value = values.get(name).value();
		if (value instanceof Boolean bool) {

			return bool;
		}

		throw new IllegalArgumentException("Option '" + name + "' must be true or false, got '" + value + "'.");
	}

	private static List<String> strings(final Map<String, EffectiveContentOptions.Value> values, final String name) {

		final Object value = values.get(name).value();
		if (value == null) {

			return null;
		}

		if (value instanceof Collection<?> collection && !collection.isEmpty() && collection.stream().allMatch(String.class::isInstance)) {

			return collection.stream().map(String.class::cast).toList();
		}

		throw new IllegalArgumentException("Option '" + name + "' must be a non-empty list of texts.");
	}
}
