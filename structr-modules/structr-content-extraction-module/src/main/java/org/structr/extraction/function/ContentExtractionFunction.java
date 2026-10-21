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
package org.structr.extraction.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.extraction.ContentExtractionModule;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.api.options.SizeUnit;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.api.options.SplitterType;
import org.structr.schema.action.Function;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Common argument handling of the content extraction functions.
 *
 * The functions are registered with dotted names in the {@value #NAMESPACE} namespace, e.g.
 * {@code documents.extractContent}, and called as {@code $.documents.extractContent()} in JavaScript
 * and {@code documents.extractContent()} in StructrScript.
 *
 * Options are validated strictly: an option that is accepted and then ignored (a typo, or an option of
 * another function) cannot be noticed from a script, so unknown keys are errors.
 */
public abstract class ContentExtractionFunction extends Function<Object, Object> {

	public static final String NAMESPACE = "documents";

	public static final String OPTION_OCR              = "ocr";
	public static final String OPTION_OCR_LANGUAGES    = "ocrLanguages";
	public static final String OPTION_MAX_CHARACTERS   = "maxCharacters";
	public static final String OPTION_INCLUDE_EMBEDDED = "includeEmbedded";
	public static final String OPTION_INCLUDE_IMAGES   = "includeImages";
	public static final String OPTION_MAX_DEPTH        = "maxDepth";

	public static final String OPTION_SPLITTER             = "splitter";
	public static final String OPTION_CHUNK_SIZE           = "chunkSize";
	public static final String OPTION_CHUNK_OVERLAP        = "chunkOverlap";
	public static final String OPTION_SIZE_UNIT            = "sizeUnit";
	public static final String OPTION_SEPARATORS           = "separators";
	public static final String OPTION_INCLUDE_HEADING_PATH = "includeHeadingPath";
	public static final String OPTION_TOKEN_ENCODING       = "tokenEncoding";

	/** Options that apply to extraction, i.e. only when the input is a file. */
	public static final Set<String> EXTRACTION_OPTIONS = Set.of(OPTION_OCR, OPTION_OCR_LANGUAGES, OPTION_MAX_CHARACTERS, OPTION_INCLUDE_EMBEDDED, OPTION_INCLUDE_IMAGES);

	/** Options that apply to splitting. */
	public static final Set<String> SPLIT_OPTIONS = Set.of(OPTION_SPLITTER, OPTION_CHUNK_SIZE, OPTION_CHUNK_OVERLAP, OPTION_SIZE_UNIT, OPTION_SEPARATORS, OPTION_INCLUDE_HEADING_PATH, OPTION_TOKEN_ENCODING);

	@Override
	public String getRequiredModule() {

		return ContentExtractionModule.MODULE_NAME;
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	/**
	 * The File at the given argument position.
	 */
	protected File fileArgument(final Object[] sources, final int index) throws FrameworkException {

		if (sources == null || sources.length <= index || sources[index] == null) {

			throw new FrameworkException(422, getName() + "(): expected a File as argument " + (index + 1) + ".");
		}

		if (sources[index] instanceof NodeInterface node && node.is(StructrTraits.FILE)) {

			return node.as(File.class);
		}

		throw new FrameworkException(422, getName() + "(): expected a File as argument " + (index + 1) + ", got " + describe(sources[index]) + ".");
	}

	/**
	 * The options object at the given argument position, validated against the accepted keys; an empty
	 * map if the argument is absent.
	 */
	protected Map<String, Object> optionsArgument(final Object[] sources, final int index, final Set<String> accepted) throws FrameworkException {

		if (sources == null || sources.length <= index || sources[index] == null) {

			return Map.of();
		}

		if (!(sources[index] instanceof Map<?, ?> map)) {

			throw new FrameworkException(422, getName() + "(): expected an options object as argument " + (index + 1) + ", got " + describe(sources[index]) + ".");
		}

		for (final Object key : map.keySet()) {

			if (!accepted.contains(String.valueOf(key))) {

				throw new FrameworkException(422, getName() + "(): unknown option '" + key + "'. " + getName() + " accepts " + String.join(", ", new TreeSet<>(accepted)) + ".");
			}
		}

		return (Map<String, Object>) map;
	}

	/**
	 * Extraction options from the module settings, overridden by the given function options.
	 */
	protected ExtractionOptions extractionOptions(final Map<String, Object> options) throws FrameworkException {

		final ExtractionOptions result = ExtractionOptions.fromSettings();

		if (options.containsKey(OPTION_OCR)) {

			try {

				result.setOcrMode(OcrMode.fromValue(stringOption(options, OPTION_OCR)));

			} catch (final IllegalArgumentException ex) {

				throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
			}
		}

		if (options.containsKey(OPTION_OCR_LANGUAGES)) {

			result.setOcrLanguages(stringOption(options, OPTION_OCR_LANGUAGES));
		}

		if (options.containsKey(OPTION_MAX_CHARACTERS)) {

			result.setMaxCharacters(positiveIntOption(options, OPTION_MAX_CHARACTERS));
		}

		if (options.containsKey(OPTION_INCLUDE_EMBEDDED)) {

			result.setIncludeEmbedded(booleanOption(options, OPTION_INCLUDE_EMBEDDED, false));
		}

		if (options.containsKey(OPTION_INCLUDE_IMAGES)) {

			result.setIncludeImages(booleanOption(options, OPTION_INCLUDE_IMAGES, false));
		}

		if (options.containsKey(OPTION_MAX_DEPTH)) {

			result.setMaxEmbeddedDepth(positiveIntOption(options, OPTION_MAX_DEPTH));
		}

		return result;
	}

	/**
	 * Split options from the module settings, overridden by the given function options.
	 */
	protected SplitOptions splitOptions(final Map<String, Object> options) throws FrameworkException {

		final SplitOptions result = SplitOptions.fromSettings();

		try {

			if (options.containsKey(OPTION_SPLITTER)) {

				result.setSplitter(SplitterType.fromValue(stringOption(options, OPTION_SPLITTER)));
			}

			if (options.containsKey(OPTION_SIZE_UNIT)) {

				result.setSizeUnit(SizeUnit.fromValue(stringOption(options, OPTION_SIZE_UNIT)));
			}

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
		}

		if (options.containsKey(OPTION_CHUNK_SIZE)) {

			result.setChunkSize(positiveIntOption(options, OPTION_CHUNK_SIZE));
		}

		if (options.containsKey(OPTION_CHUNK_OVERLAP)) {

			result.setChunkOverlap(nonNegativeIntOption(options, OPTION_CHUNK_OVERLAP));
		}

		if (options.containsKey(OPTION_SEPARATORS)) {

			result.setSeparators(stringListOption(options, OPTION_SEPARATORS));
		}

		if (options.containsKey(OPTION_INCLUDE_HEADING_PATH)) {

			result.setIncludeHeadingPath(booleanOption(options, OPTION_INCLUDE_HEADING_PATH, false));
		}

		if (options.containsKey(OPTION_TOKEN_ENCODING)) {

			result.setTokenEncoding(stringOption(options, OPTION_TOKEN_ENCODING));
		}

		try {

			return result.validate();

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
		}
	}

	/**
	 * The text at the given argument position.
	 */
	protected String textArgument(final Object[] sources, final int index) throws FrameworkException {

		if (sources == null || sources.length <= index || sources[index] == null) {

			throw new FrameworkException(422, getName() + "(): expected a text as argument " + (index + 1) + ".");
		}

		if (sources[index] instanceof String text) {

			return text;
		}

		throw new FrameworkException(422, getName() + "(): expected a text as argument " + (index + 1) + ", got " + describe(sources[index]) + ".");
	}

	protected int nonNegativeIntOption(final Map<String, Object> options, final String key) throws FrameworkException {

		final Object value = options.get(key);
		if (value instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() == Math.floor(number.doubleValue())) {

			return number.intValue();
		}

		throw new FrameworkException(422, getName() + "(): option '" + key + "' must be a whole number of 0 or more, got " + describe(value) + ".");
	}

	protected List<String> stringListOption(final Map<String, Object> options, final String key) throws FrameworkException {

		final Object value = options.get(key);
		if (value instanceof Collection<?> collection && !collection.isEmpty() && collection.stream().allMatch(String.class::isInstance)) {

			return collection.stream().map(String.class::cast).toList();
		}

		throw new FrameworkException(422, getName() + "(): option '" + key + "' must be a non-empty list of texts, got " + describe(value) + ".");
	}

	protected String stringOption(final Map<String, Object> options, final String key) throws FrameworkException {

		final Object value = options.get(key);
		if (value == null || value.toString().isBlank()) {

			throw new FrameworkException(422, getName() + "(): option '" + key + "' must not be empty.");
		}

		return value.toString().trim();
	}

	protected int positiveIntOption(final Map<String, Object> options, final String key) throws FrameworkException {

		final Object value = options.get(key);
		if (value instanceof Number number && number.doubleValue() >= 1 && number.doubleValue() == Math.floor(number.doubleValue())) {

			return number.intValue();
		}

		throw new FrameworkException(422, getName() + "(): option '" + key + "' must be a positive whole number, got " + describe(value) + ".");
	}

	protected boolean booleanOption(final Map<String, Object> options, final String key, final boolean defaultValue) throws FrameworkException {

		final Object value = options.get(key);
		if (value == null) {

			return defaultValue;
		}

		if (value instanceof Boolean bool) {

			return bool;
		}

		throw new FrameworkException(422, getName() + "(): option '" + key + "' must be true or false, got " + describe(value) + ".");
	}

	protected FrameworkException extractionFailed(final ContentExtractionException ex) {

		return new FrameworkException(422, getName() + "(): " + ex.getMessage());
	}

	protected static List<GraphObjectMap> toGraphObjectMaps(final Collection<Map<String, Object>> maps) {

		final List<GraphObjectMap> result = new ArrayList<>();

		for (final Map<String, Object> map : maps) {

			result.add(ResultMaps.toGraphObjectMap(map));
		}

		return result;
	}

	// ----- private methods -----
	private static String describe(final Object value) {

		if (value == null) {

			return "null";
		}

		if (value instanceof NodeInterface node) {

			return "a " + node.getType();
		}

		return value.getClass().getSimpleName() + " '" + value + "'";
	}
}
