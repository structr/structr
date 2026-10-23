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
package org.structr.extraction.function.enterprise;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 * A function about extracted content that only Structr Enterprise provides (the AI module: embeddings and
 * semantic search). In the Community Edition it is registered under its name only to say so: a script that
 * calls it, e.g. after a change from Enterprise to the Community Edition, gets this message instead of an
 * unknown function. The AI module loads after this module (it depends on it) and registers the real
 * function under the same name, which replaces this one.
 */
public class EnterpriseOnlyFunction extends ContentExtractionFunction {

	private final String name;
	private final String signature;
	private final String description;

	public EnterpriseOnlyFunction(final String name, final String signature, final String description) {

		this.name        = name;
		this.signature   = signature;
		this.description = description;
	}

	/** The functions of the AI module in the documents namespace, see the Content Extraction article. */
	public static List<EnterpriseOnlyFunction> all() {

		return List.of(
			new EnterpriseOnlyFunction("vectorSearch",          "query [, options ]",         "Searches extracted content by meaning (semantic search), optionally together with the words."),
			new EnterpriseOnlyFunction("answer",                "question, options",          "Answers a question from extracted content with a chat model, with citations."),
			new EnterpriseOnlyFunction("embedContent",          "[ fileOrFolderOrList [, options ] ]", "Embeds the chunks of extracted files."),
			new EnterpriseOnlyFunction("linkEmbeddingProvider", "folderOrFile, config, model [, options ]", "Sets the provider and model that embed the extracted content of a folder's files."),
			new EnterpriseOnlyFunction("embeddingStatus",       "",                           "Returns the state of the embeddings of extracted content."),
			new EnterpriseOnlyFunction("resetEmbeddings",       "[ options ]",                "Removes the embeddings of extracted content and embeds it again.")
		);
	}

	@Override
	public boolean isEnterprise() {

		return true;
	}

	@Override
	public String getName() {

		return NAMESPACE + "." + name;
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages(signature);
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		throw new FrameworkException(422, getName() + "() needs Structr Enterprise with the AI module. In the Community Edition, extracted content is searched by words with "
			+ NAMESPACE + ".fulltextSearch(). See the documentation: APIs & Integrations > Content Extraction > Embeddings and Semantic Search.");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{ $." + getName() + "(" + signature + ") }} (Structr Enterprise)"),
			Usage.structrScript("Usage: ${ " + getName() + "(" + signature + ") } (Structr Enterprise)")
		);
	}

	@Override
	public String getShortDescription() {

		return description + " Structr Enterprise only.";
	}

	@Override
	public String getLongDescription() {

		return "This function is part of Structr Enterprise (AI module). In the Community Edition calling it fails with a message that says so. "
			+ "See the Content Extraction article, section \"Embeddings and Semantic Search (Enterprise Edition)\".";
	}
}
