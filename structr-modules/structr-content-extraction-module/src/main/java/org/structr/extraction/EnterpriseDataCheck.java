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
package org.structr.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.extraction.traits.definitions.ExtractedContentNodeTraitDefinition;

/**
 * Explains, once at startup, what Structr Enterprise left behind when it is not running anymore.
 *
 * The Enterprise AI module writes embeddings onto the chunks of this module and links folders to LLM
 * providers. After a change to the Community Edition all of it stays where it is: the vectors and the
 * vector index are kept and used again after a return to Enterprise, the links and the embedding state of
 * files are ignored. Nothing fails, but without this message nobody would know why the data is there or
 * why semantic search is gone.
 */
public final class EnterpriseDataCheck {

	private static final Logger logger = LoggerFactory.getLogger(EnterpriseDataCheck.class);

	private static final String AI_MODULE = "ai";

	private EnterpriseDataCheck() {
	}

	public static void run() {

		if (StructrApp.getConfiguration().getModules().containsKey(AI_MODULE)) {

			return;
		}

		final App app = StructrApp.getInstance(SecurityContext.getSuperUserInstance());

		try (final Tx tx = app.tx()) {

			final PropertyKey<Double[]> embedding = Traits.of(ContentExtractionTraits.EXTRACTED_CONTENT_NODE).key(ExtractedContentNodeTraitDefinition.EMBEDDING_PROPERTY);
			final long embedded                   = app.nodeQuery(ContentExtractionTraits.EXTRACTED_CONTENT_NODE).notBlank(embedding).count();

			tx.success();

			if (embedded > 0) {

				logger.warn("{} chunks of extracted content carry embeddings created by Structr Enterprise (AI module). They are kept, together with "
					+ "their vector index, and are used again after a return to Structr Enterprise. Semantic search, automatic embedding and the "
					+ "documents.searchContentSemantic() and documents.answerFromContent() functions need Structr Enterprise; keyword search with "
					+ "documents.searchExtractedContent() works as before. See the documentation: APIs & Integrations > Content Extraction.", embedded);
			}

		} catch (Throwable t) {

			logger.debug("Unable to check for embeddings from Structr Enterprise: {}", t.getMessage());
		}
	}
}
