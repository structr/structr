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
package org.structr.extraction.api.options;

import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;

/**
 * Options for a single extraction. Start from {@link #fromSettings()} and override what the call needs.
 *
 * The location of the tesseract binary and its language data are deliberately not options: they are
 * server configuration, and a caller that could choose them could choose which program runs.
 */
public final class ExtractionOptions {

	private OcrMode ocrMode          = OcrMode.AUTO;
	private String ocrLanguages      = "eng";
	private int ocrMaxPages          = 200;
	private int ocrTimeoutSeconds    = 120;
	private int ocrDpi               = 300;
	private int maxCharacters        = 10_000_000;
	private boolean includeEmbedded  = false;
	private boolean includeImages    = false;
	private boolean extractFigures   = false;
	private String figureType        = ContentExtractionTraits.DOCUMENT_FIGURE;
	private int maxEmbeddedDepth     = 10;

	/**
	 * Options initialized from the module settings.
	 */
	public static ExtractionOptions fromSettings() {

		final ExtractionOptions options = new ExtractionOptions();

		options.ocrMode           = OcrMode.fromValue(ContentExtractionSettings.OcrMode.getValue());
		options.ocrLanguages      = ContentExtractionSettings.OcrLanguages.getValue();
		options.ocrMaxPages       = ContentExtractionSettings.OcrMaxPages.getValue();
		options.ocrTimeoutSeconds = ContentExtractionSettings.OcrTimeout.getValue();
		options.ocrDpi            = ContentExtractionSettings.OcrDpi.getValue();
		options.maxCharacters     = ContentExtractionSettings.MaxCharacters.getValue();
		options.includeEmbedded   = ContentExtractionSettings.IncludeEmbedded.getValue();
		options.includeImages     = ContentExtractionSettings.IncludeImages.getValue();
		options.extractFigures    = ContentExtractionSettings.ExtractFigures.getValue();

		return options;
	}

	public OcrMode getOcrMode() {

		return ocrMode;
	}

	public ExtractionOptions setOcrMode(final OcrMode ocrMode) {

		this.ocrMode = ocrMode;

		return this;
	}

	public String getOcrLanguages() {

		return ocrLanguages;
	}

	public ExtractionOptions setOcrLanguages(final String ocrLanguages) {

		this.ocrLanguages = ocrLanguages;

		return this;
	}

	public int getOcrMaxPages() {

		return ocrMaxPages;
	}

	public ExtractionOptions setOcrMaxPages(final int ocrMaxPages) {

		this.ocrMaxPages = ocrMaxPages;

		return this;
	}

	public int getOcrTimeoutSeconds() {

		return ocrTimeoutSeconds;
	}

	public ExtractionOptions setOcrTimeoutSeconds(final int ocrTimeoutSeconds) {

		this.ocrTimeoutSeconds = ocrTimeoutSeconds;

		return this;
	}

	public int getOcrDpi() {

		return ocrDpi;
	}

	public ExtractionOptions setOcrDpi(final int ocrDpi) {

		this.ocrDpi = ocrDpi;

		return this;
	}

	public int getMaxCharacters() {

		return maxCharacters;
	}

	public ExtractionOptions setMaxCharacters(final int maxCharacters) {

		this.maxCharacters = maxCharacters;

		return this;
	}

	public boolean isIncludeEmbedded() {

		return includeEmbedded;
	}

	public ExtractionOptions setIncludeEmbedded(final boolean includeEmbedded) {

		this.includeEmbedded = includeEmbedded;

		return this;
	}

	/**
	 * Whether images inside documents (inline images of PDFs, pictures in office documents and e-mails)
	 * are read with OCR. Only applies if OCR runs.
	 */
	public boolean isIncludeImages() {

		return includeImages;
	}

	public ExtractionOptions setIncludeImages(final boolean includeImages) {

		this.includeImages = includeImages;

		return this;
	}

	/**
	 * Whether processing a file also stores its figures as DocumentFigures. Not used by the extraction of
	 * the text itself.
	 */
	public boolean isExtractFigures() {

		return extractFigures;
	}

	public ExtractionOptions setExtractFigures(final boolean extractFigures) {

		this.extractFigures = extractFigures;

		return this;
	}

	/**
	 * The type stored figures are created as: DocumentFigure, or a type of the data model that inherits it.
	 */
	public String getFigureType() {

		return figureType;
	}

	public ExtractionOptions setFigureType(final String figureType) {

		this.figureType = figureType;

		return this;
	}

	public int getMaxEmbeddedDepth() {

		return maxEmbeddedDepth;
	}

	public ExtractionOptions setMaxEmbeddedDepth(final int maxEmbeddedDepth) {

		this.maxEmbeddedDepth = maxEmbeddedDepth;

		return this;
	}
}
