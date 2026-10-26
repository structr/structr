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

import com.google.gson.Gson;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.function.Functions;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.GlobalSearchContributors;
import org.structr.core.traits.StructrTraits;
import org.structr.extraction.automation.ContentFileChangeListener;
import org.structr.extraction.configuration.ContentConfigurationDeployment;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.function.enterprise.EnterpriseOnlyFunction;
import org.structr.extraction.function.extract.DetectLanguageFunction;
import org.structr.extraction.function.extract.ExtractContentFunction;
import org.structr.extraction.function.extract.ExtractEmbeddedFunction;
import org.structr.extraction.function.extract.ExtractFiguresFunction;
import org.structr.extraction.function.extract.ExtractLinksFunction;
import org.structr.extraction.function.extract.ExtractMetadataFunction;
import org.structr.extraction.function.extract.ExtractTablesFunction;
import org.structr.extraction.function.graph.DeleteExtractedContentFunction;
import org.structr.extraction.function.graph.ExtractContentInfoFunction;
import org.structr.extraction.function.graph.GetContentContextFunction;
import org.structr.extraction.function.graph.GetFigureContextFunction;
import org.structr.extraction.function.graph.GetExtractedTextFunction;
import org.structr.extraction.function.graph.ProcessContentFunction;
import org.structr.extraction.function.graph.DocumentsFulltextSearchFunction;
import org.structr.extraction.function.text.CountTokensFunction;
import org.structr.extraction.function.text.NormalizeTextFunction;
import org.structr.extraction.function.text.SplitContentFunction;
import org.structr.extraction.function.text.StopWordsFunction;
import org.structr.extraction.function.text.TruncateTokensFunction;
import org.structr.extraction.query.ContentGlobalSearch;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.extraction.traits.definitions.DocumentFigureTraitDefinition;
import org.structr.extraction.traits.definitions.ExtractedContentNodeTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.extraction.traits.relationships.ContentExtractionConfigurationConfiguresDefinition;
import org.structr.extraction.traits.relationships.ExtractedContentNodeNextDefinition;
import org.structr.extraction.traits.relationships.FigureHasContentDefinition;
import org.structr.extraction.traits.relationships.FileHasExtractedContentDefinition;
import org.structr.extraction.traits.relationships.FileHasFigureDefinition;
import org.structr.module.StructrModule;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Extracts the content of files (Tika, with optional Tesseract OCR), splits it into chunks and stores
 * the chunks as ExtractedContentNode nodes connected to their file.
 *
 * Embeddings, vector indexes and retrieval are not part of this module; the Enterprise AI module builds
 * on its Java API for that.
 */
public class ContentExtractionModule implements StructrModule {

	public static final String MODULE_NAME = "content-extraction";

	public ContentExtractionModule() {

		// ServiceLoader instantiates the module at startup; registering the settings here gives them the
		// same lifetime as in modules that declare their settings as static fields of the module class
		ContentExtractionSettings.register();
	}

	@Override
	public void onLoad() {

		// relationship traits and types first: the relationship properties below resolve them eagerly
		StructrTraits.registerTrait(new FileHasExtractedContentDefinition());
		StructrTraits.registerTrait(new ExtractedContentNodeNextDefinition());
		StructrTraits.registerTrait(new ContentExtractionConfigurationConfiguresDefinition());
		StructrTraits.registerTrait(new FileHasFigureDefinition());
		StructrTraits.registerTrait(new FigureHasContentDefinition());

		StructrTraits.registerRelationshipType(ContentExtractionTraits.FILE_HAS_EXTRACTED_CONTENT, ContentExtractionTraits.FILE_HAS_EXTRACTED_CONTENT);
		StructrTraits.registerRelationshipType(ContentExtractionTraits.EXTRACTED_CONTENT_NEXT,     ContentExtractionTraits.EXTRACTED_CONTENT_NEXT);
		StructrTraits.registerRelationshipType(ContentExtractionTraits.CONFIGURATION_CONFIGURES,   ContentExtractionTraits.CONFIGURATION_CONFIGURES);
		StructrTraits.registerRelationshipType(ContentExtractionTraits.FILE_HAS_FIGURE,            ContentExtractionTraits.FILE_HAS_FIGURE);
		StructrTraits.registerRelationshipType(ContentExtractionTraits.FIGURE_CONTENT,             ContentExtractionTraits.FIGURE_CONTENT);

		StructrTraits.registerTrait(new ExtractedContentNodeTraitDefinition());
		StructrTraits.registerNodeType(ContentExtractionTraits.EXTRACTED_CONTENT_NODE, ContentExtractionTraits.EXTRACTED_CONTENT_NODE);

		// configuration of automatic extraction, attached to folders and files
		StructrTraits.registerTrait(new ContentExtractionConfigurationTraitDefinition());
		StructrTraits.registerNodeType(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);

		// stored figures: images with what extractFigures() found out about them, a base type for the data model
		StructrTraits.registerTrait(new DocumentFigureTraitDefinition());
		StructrTraits.registerNodeType(ContentExtractionTraits.DOCUMENT_FIGURE, StructrTraits.ABSTRACT_FILE, StructrTraits.LINKABLE, StructrTraits.FILE, StructrTraits.IMAGE, ContentExtractionTraits.DOCUMENT_FIGURE);

		// extraction state on File
		ContentFileStateProperties.register();

		// automatic extraction on new content and moves
		TransactionCommand.registerTransactionListener(new ContentFileChangeListener());

		// file content in the global search of the admin UI
		GlobalSearchContributors.register(ContentGlobalSearch.INSTANCE);

		// says once what Structr Enterprise left behind, if it is not running anymore
		Services.getInstance().registerInitializationCallback(() -> {

			if (!Services.isTesting()) {
				EnterpriseDataCheck.run();
			}
		});
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		// all functions live in the "documents" namespace: $.documents.extractContent() in JavaScript,
		// documents.extractContent() in StructrScript

		Functions.put(licenseManager, new ExtractContentFunction());
		Functions.put(licenseManager, new ExtractMetadataFunction());
		Functions.put(licenseManager, new ExtractTablesFunction());
		Functions.put(licenseManager, new ExtractLinksFunction());
		Functions.put(licenseManager, new ExtractEmbeddedFunction());
		Functions.put(licenseManager, new ExtractFiguresFunction());
		Functions.put(licenseManager, new SplitContentFunction());
		Functions.put(licenseManager, new CountTokensFunction());
		Functions.put(licenseManager, new TruncateTokensFunction());
		Functions.put(licenseManager, new NormalizeTextFunction());
		Functions.put(licenseManager, new DetectLanguageFunction());
		Functions.put(licenseManager, new StopWordsFunction());
		Functions.put(licenseManager, new ProcessContentFunction());
		Functions.put(licenseManager, new ExtractContentInfoFunction());
		Functions.put(licenseManager, new GetContentContextFunction());
		Functions.put(licenseManager, new GetExtractedTextFunction());
		Functions.put(licenseManager, new DeleteExtractedContentFunction());
		Functions.put(licenseManager, new GetFigureContextFunction());
		Functions.put(licenseManager, new DocumentsFulltextSearchFunction());

		// the functions of Structr Enterprise in this namespace, which say so when called in the Community Edition;
		// the AI module depends on this module, so it loads later and replaces them with the real ones
		for (final EnterpriseOnlyFunction function : EnterpriseOnlyFunction.all()) {
			Functions.put(licenseManager, function);
		}
	}

	@Override
	public String getName() {

		return MODULE_NAME;
	}

	@Override
	public Set<String> getDependencies() {

		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {

		return null;
	}

	@Override
	public List<String> getUIExtensionScripts() {

		return List.of("js/content-extraction-ui.js");
	}

	@Override
	public boolean hasDeploymentData() {

		return true;
	}

	@Override
	public void exportDeploymentData(final Path target, final Gson gson) throws FrameworkException {

		ContentConfigurationDeployment.exportConfigurations(target, gson);
	}

	@Override
	public void importDeploymentData(final Path source, final Gson gson) throws FrameworkException {

		ContentConfigurationDeployment.importConfigurations(source, gson);
	}
}
