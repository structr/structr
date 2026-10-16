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
package org.structr.extraction.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.configuration.ContentConfigurationDialog;
import org.structr.extraction.configuration.ContentConfigurations;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.extraction.traits.properties.NullableBooleanProperty;
import org.structr.extraction.traits.wrappers.ContentExtractionConfigurationTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.AbstractFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of automatic content extraction for a Folder subtree or a single File.
 *
 * Only admins can create and change configurations, since they drive CPU and storage cost. Every option
 * is optional: an empty option uses the contentextraction.* setting.
 */
public class ContentExtractionConfigurationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TARGET_PROPERTY  = "target";
	public static final String ENABLED_PROPERTY = "enabled";

	/** The options a configuration can set; the property names are the option names. */
	public static final List<String> OPTION_PROPERTIES = List.of(
		ContentOptionsResolver.SPLITTER,
		ContentOptionsResolver.CHUNK_SIZE,
		ContentOptionsResolver.CHUNK_OVERLAP,
		ContentOptionsResolver.SIZE_UNIT,
		ContentOptionsResolver.SEPARATORS,
		ContentOptionsResolver.INCLUDE_HEADING_PATH,
		ContentOptionsResolver.TOKEN_ENCODING,
		ContentOptionsResolver.OCR,
		ContentOptionsResolver.OCR_LANGUAGES,
		ContentOptionsResolver.MAX_CHARACTERS,
		ContentOptionsResolver.INCLUDE_EMBEDDED
	);

	public static final Set<String> SPLITTERS       = Set.of("recursive", "structure", "sentence", "token", "page");
	public static final Set<String> SIZE_UNITS      = Set.of("tokens", "characters");
	public static final Set<String> TOKEN_ENCODINGS = Set.of("cl100k_base", "o200k_base", "p50k_base", "r50k_base");
	public static final Set<String> OCR_MODES       = Set.of("auto", "always", "off");

	public static final int MIN_CHUNK_SIZE = 32;
	public static final int MAX_CHUNK_SIZE = 32768;

	private static final String OCR_LANGUAGES_PATTERN = "^[A-Za-z_]+(\\+[A-Za-z_]+)*$";

	public ContentExtractionConfigurationTraitDefinition() {

		super(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class, new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					return validate(obj, errorBuffer);
				}
			},

			OnCreation.class, new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					requireAdmin(securityContext);
					initializeName(graphObject);
				}
			},

			OnModification.class, new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					requireAdmin(securityContext);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(ContentExtractionConfiguration.class, (traits, node) -> new ContentExtractionConfigurationTraitWrapper(traits, node));
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> target         = new EndNode(traitsInstance, TARGET_PROPERTY, ContentExtractionTraits.CONFIGURATION_CONFIGURES).description("The File or Folder this configuration applies to.");
		final Property<Boolean> enabled              = new BooleanProperty(ENABLED_PROPERTY).defaultValue(true).description("Whether files below the target are extracted automatically.");
		final Property<String> splitter              = new EnumProperty(ContentOptionsResolver.SPLITTER, SPLITTERS).description("Splitter; empty uses contentextraction.split.splitter.");
		final Property<Integer> chunkSize            = new IntProperty(ContentOptionsResolver.CHUNK_SIZE).description("Chunk size, " + MIN_CHUNK_SIZE + " to " + MAX_CHUNK_SIZE + "; empty uses contentextraction.split.chunksize.");
		final Property<Integer> chunkOverlap         = new IntProperty(ContentOptionsResolver.CHUNK_OVERLAP).description("Chunk overlap; empty uses contentextraction.split.chunkoverlap.");
		final Property<String> sizeUnit              = new EnumProperty(ContentOptionsResolver.SIZE_UNIT, SIZE_UNITS).description("Unit of chunk size and overlap; empty uses contentextraction.split.sizeunit.");
		final Property<String[]> separators          = new ArrayProperty<>(ContentOptionsResolver.SEPARATORS, String.class).description("Separators of the recursive splitter; empty uses the default separators.");
		final Property<Boolean> includeHeadingPath   = new NullableBooleanProperty(ContentOptionsResolver.INCLUDE_HEADING_PATH).description("Whether chunks start with their section path; empty means no.");
		final Property<String> tokenEncoding         = new EnumProperty(ContentOptionsResolver.TOKEN_ENCODING, TOKEN_ENCODINGS).description("Tokenizer encoding; empty uses contentextraction.split.tokenencoding.");
		final Property<String> ocr                   = new EnumProperty(ContentOptionsResolver.OCR, OCR_MODES).description("OCR mode; empty uses contentextraction.ocr.mode.");
		final Property<String> ocrLanguages          = new StringProperty(ContentOptionsResolver.OCR_LANGUAGES).description("Tesseract languages joined with +; empty uses contentextraction.ocr.languages.");
		final Property<Integer> maxCharacters        = new IntProperty(ContentOptionsResolver.MAX_CHARACTERS).description("Maximum characters per document; empty uses contentextraction.extraction.maxcharacters.");
		final Property<Boolean> includeEmbedded      = new NullableBooleanProperty(ContentOptionsResolver.INCLUDE_EMBEDDED).description("Whether embedded documents are extracted; empty uses contentextraction.extraction.includeembedded.");

		return newSet(target, enabled, splitter, chunkSize, chunkOverlap, sizeUnit, separators, includeHeadingPath, tokenEncoding, ocr, ocrLanguages, maxCharacters, includeEmbedded);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("describe", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					return ContentConfigurationDialog.describe(actionContext.getSecurityContext(), arguments.get(TARGET_PROPERTY));
				}

				@Override
				public String getDescription() {

					return "Returns the configuration of a File or Folder, the one it inherits, the effective options and the defaults.";
				}
			},

			new JavaMethod("save", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					return ContentConfigurationDialog.save(actionContext.getSecurityContext(), arguments.get(TARGET_PROPERTY), arguments.get("values"), Boolean.TRUE.equals(arguments.get("process")));
				}

				@Override
				public String getDescription() {

					return "Creates or updates the configuration of a File or Folder, optionally processing the files it covers.";
				}
			},

			new JavaMethod("remove", false, true) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					return ContentConfigurationDialog.remove(actionContext.getSecurityContext(), arguments.get(TARGET_PROPERTY));
				}

				@Override
				public String getDescription() {

					return "Deletes the configuration of a File or Folder.";
				}
			}
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		final Set<String> view = newSet(NodeInterfaceTraitDefinition.NAME_PROPERTY, TARGET_PROPERTY, ENABLED_PROPERTY);

		view.addAll(OPTION_PROPERTIES);

		return Map.of(PropertyView.Public, view, PropertyView.Ui, view);
	}

	@Override
	public Relation getRelation() {

		return null;
	}

	// ----- public static methods -----
	public static void requireAdmin(final SecurityContext securityContext) throws FrameworkException {

		// isSuperUser() is true for the superuser context and for users with isAdmin = true
		if (securityContext != null && !securityContext.isSuperUser()) {

			throw new FrameworkException(403, "Only admins can change content extraction configurations.");
		}
	}

	// ----- private methods -----
	private static boolean validate(final GraphObject obj, final ErrorBuffer errorBuffer) {

		final Traits traits                        = obj.getTraits();
		final PropertyKey<Integer> chunkSize       = traits.key(ContentOptionsResolver.CHUNK_SIZE);
		final PropertyKey<Integer> chunkOverlap    = traits.key(ContentOptionsResolver.CHUNK_OVERLAP);
		final PropertyKey<Integer> maxCharacters   = traits.key(ContentOptionsResolver.MAX_CHARACTERS);
		final PropertyKey<String> ocrLanguages     = traits.key(ContentOptionsResolver.OCR_LANGUAGES);
		final PropertyKey<String[]> separators     = traits.key(ContentOptionsResolver.SEPARATORS);
		boolean valid                              = true;

		valid &= ValidationHelper.isValidPropertyNotNull(obj, traits.key(TARGET_PROPERTY), errorBuffer);
		valid &= isOnlyConfigurationOfTarget(obj, errorBuffer);
		valid &= ValidationHelper.isValidIntegerInRange(obj, chunkSize, "[" + MIN_CHUNK_SIZE + "," + MAX_CHUNK_SIZE + "]", errorBuffer);
		valid &= ValidationHelper.isValidIntegerInRange(obj, chunkOverlap, "[0," + MAX_CHUNK_SIZE + "]", errorBuffer);
		valid &= ValidationHelper.isValidIntegerInRange(obj, maxCharacters, "[1," + Integer.MAX_VALUE + "]", errorBuffer);

		if (obj.getProperty(ocrLanguages) != null) {

			valid &= ValidationHelper.isValidStringMatchingRegex(obj, ocrLanguages, OCR_LANGUAGES_PATTERN, "languages joined with +, e.g. eng+deu", errorBuffer);
		}

		final Integer size    = obj.getProperty(chunkSize);
		final Integer overlap = obj.getProperty(chunkOverlap);

		if (size != null && overlap != null && overlap >= size) {

			errorBuffer.add(new SemanticErrorToken(obj.getType(), ContentOptionsResolver.CHUNK_OVERLAP, "must_be_smaller_than_chunk_size").withValue(overlap).withDetail(size));
			valid = false;
		}

		final String[] separatorValues = obj.getProperty(separators);
		if (separatorValues != null && separatorValues.length == 0) {

			errorBuffer.add(new SemanticErrorToken(obj.getType(), ContentOptionsResolver.SEPARATORS, "must_not_be_empty"));
			valid = false;
		}

		return valid;
	}

	private static boolean isOnlyConfigurationOfTarget(final GraphObject obj, final ErrorBuffer errorBuffer) {

		final AbstractFile target = obj.as(ContentExtractionConfiguration.class).getTarget();
		if (target != null && ContentConfigurations.attached(target).size() > 1) {

			errorBuffer.add(new SemanticErrorToken(obj.getType(), TARGET_PROPERTY, "already_configured").withValue(target.getUuid()).withDetail(target.getPath() + " already has a content extraction configuration."));

			return false;
		}

		return true;
	}

	private static void initializeName(final GraphObject graphObject) throws FrameworkException {

		final PropertyKey<String> name = graphObject.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final AbstractFile target      = graphObject.as(ContentExtractionConfiguration.class).getTarget();

		if (graphObject.getProperty(name) == null && target != null) {

			graphObject.setProperty(name, "Content extraction for " + target.getPath());
		}
	}
}
