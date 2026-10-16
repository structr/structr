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

import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.structr.extraction.api.document.DetectedLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Language detection with the Optimaize detector that ships with Tika.
 *
 * Loading the language profiles takes a moment and the detector keeps per-call state, so there is one
 * detector, loaded on first use and used by one thread at a time; detection itself is fast.
 */
public final class TextLanguageDetector {

	/** Detection reads at most this many characters; more text does not change the result. */
	private static final int SAMPLE_LENGTH = 20_000;

	/** Below this many characters, the result is too unreliable to report. */
	private static final int MIN_LENGTH = 20;

	private static LanguageDetector detector = null;

	private TextLanguageDetector() {
	}

	public static DetectedLanguage detect(final String text) {

		if (text == null || text.strip().length() < MIN_LENGTH) {

			return DetectedLanguage.UNKNOWN;
		}

		final String sample = text.length() > SAMPLE_LENGTH ? text.substring(0, SAMPLE_LENGTH) : text;
		final List<LanguageResult> results;

		synchronized (TextLanguageDetector.class) {

			if (detector == null) {

				detector = new OptimaizeLangDetector().loadModels();
			}

			results = detector.detectAll(sample);
		}

		final List<DetectedLanguage.Candidate> candidates = new ArrayList<>();

		for (final LanguageResult result : results) {

			if (!result.isUnknown()) {

				candidates.add(new DetectedLanguage.Candidate(result.getLanguage(), result.getRawScore()));
			}
		}

		if (candidates.isEmpty()) {

			return DetectedLanguage.UNKNOWN;
		}

		return new DetectedLanguage(candidates.getFirst().language(), candidates.getFirst().confidence(), candidates);
	}
}
