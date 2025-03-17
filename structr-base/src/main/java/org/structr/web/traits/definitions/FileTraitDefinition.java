/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEvent;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.schema.action.EvaluationHints;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.property.FileDataProperty;
import org.structr.web.traits.wrappers.FileTraitWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class FileTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String FILE_PARENT_PROPERTY               = "fileParent";
	public static final String CONTENT_TYPE_PROPERTY              = "contentType";
	public static final String DONT_CACHE_PROPERTY                = "dontCache";
	public static final String INDEXED_PROPERTY                   = "indexed";
	public static final String IS_FILE_PROPERTY                   = "isFile";
	public static final String IS_TEMPLATE_PROPERTY               = "isTemplate";
	public static final String USE_AS_JAVASCRIPT_LIBRARY_PROPERTY = "useAsJavascriptLibrary";
	public static final String CACHE_FOR_SECONDS_PROPERTY         = "cacheForSeconds";
	public static final String POSITION_PROPERTY                  = "position";
	public static final String VERSION_PROPERTY                   = "version";
	public static final String MD5_PROPERTY                       = "md5";
	public static final String SHA1_PROPERTY                      = "sha1";
	public static final String SHA512_PROPERTY                    = "sha512";
	public static final String URL_PROPERTY                       = "url";
	public static final String CHECKSUM_PROPERTY                  = "checksum";
	public static final String CRC32_PROPERTY                     = "crc32";
	public static final String FILE_MODIFICATION_DATE_PROPERTY    = "fileModificationDate";
	public static final String SIZE_PROPERTY                      = "size";
	public static final String BASE64_DATA_PROPERTY               = "base64Data";

	public FileTraitDefinition() {
		super(StructrTraits.FILE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final File thisFile = graphObject.as(File.class);

					if (Settings.FilesystemEnabled.getValue() && !thisFile.getHasParent()) {

						final Folder workingOrHomeDir = thisFile.getCurrentWorkingDir();
						if (workingOrHomeDir != null && thisFile.getParent() == null) {

							thisFile.setParent(workingOrHomeDir);
						}
					}
				}
			},

			OnModification.class,
			new OnModification() {
				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final File thisFile = graphObject.as(File.class);

					synchronized (thisFile) {

						//SearchCommand.prefetch(org.structr.web.entity.File.class, thisFile.getUuid());

						// save current security context
						final SecurityContext previousSecurityContext = securityContext;

						// replace with SU context
						graphObject.setSecurityContext(SecurityContext.getSuperUserInstance());

						// update metadata and parent as superuser
						FileHelper.updateMetadata(thisFile, false);

						// restore previous security context
						graphObject.setSecurityContext(previousSecurityContext);

						// acknowledge all events for this node when it is modified
						RuntimeEventLog.acknowledgeAllEventsForId(thisFile.getUuid());
					}
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {
				@Override
				public void onNodeDeletion(NodeInterface nodeInterface, SecurityContext securityContext) throws FrameworkException {

					final File thisFile = nodeInterface.as(File.class);

					// only delete mounted files
					if (!thisFile.isExternal()) {

						StorageProviderFactory.getStorageProvider(thisFile).delete();
					}
				}
			},

			AfterCreation.class,
			new AfterCreation() {
				@Override
				public void afterCreation(GraphObject graphObject, SecurityContext securityContext) throws FrameworkException {

					final File thisFile = graphObject.as(File.class);

					try {

						FileHelper.updateMetadata(thisFile);
						thisFile.setVersion(0);

					} catch (IOException ex) {

						final Logger logger = LoggerFactory.getLogger(org.structr.web.entity.File.class);
						logger.error("Could not update metadata of {}: {}", thisFile.getPath(), ex.getMessage());
					}
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			SetProperty.class,
			new SetProperty() {
				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					final File thisFile = graphObject.as(File.class);

					FileTraitDefinition.OnSetProperty(thisFile, key, value, isCreation);

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			}

			/*

			SetProperties.class,
			new SetProperties() {

				@Override
				public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

					final File thisFile = graphObject.as(File.class);

					FileTraitDefinition.OnSetProperties(thisFile, securityContext, properties, isCreation);

					getSuper().setProperties(graphObject, securityContext, properties, isCreation);
				}
			}
			*/
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

			new JavaMethod("doCSVImport", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(File.class).doCSVImport(securityContext, arguments.toMap());
				}
			},

			new JavaMethod("doXMLImport", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(File.class).doXMLImport(securityContext, arguments.toMap());
				}
			},

			new JavaMethod("getFirstLines", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(File.class).getFirstLines(securityContext, arguments.toMap());
				}
			},

			new JavaMethod("getCSVHeaders", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(File.class).getCSVHeaders(securityContext, arguments.toMap());
				}
			},

			new JavaMethod("getXMLStructure", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(File.class).getXMLStructure(securityContext);
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			File.class, (traits, node) -> new FileTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> fileParentProperty  = new StartNode(FILE_PARENT_PROPERTY, StructrTraits.FOLDER_CONTAINS_FILE);
		final Property<String> contentTypeProperty        = new StringProperty(CONTENT_TYPE_PROPERTY);
		final Property<Boolean> dontCacheProperty         = new BooleanProperty(DONT_CACHE_PROPERTY).defaultValue(false);
		final Property<Boolean> indexedProperty           = new BooleanProperty(INDEXED_PROPERTY);
		final Property<Boolean> isFileProperty            = new ConstantBooleanProperty(IS_FILE_PROPERTY, true).readOnly();
		final Property<Boolean> isTemplateProperty        = new BooleanProperty(IS_TEMPLATE_PROPERTY);
		final Property<Boolean> useAsJavascriptLibrary    = new BooleanProperty(USE_AS_JAVASCRIPT_LIBRARY_PROPERTY).indexed();
		final Property<Integer> cacheForSecondsProperty   = new IntProperty(CACHE_FOR_SECONDS_PROPERTY);
		final Property<Integer> positionProperty          = new IntProperty(POSITION_PROPERTY).indexed();
		final Property<Integer> versionProperty           = new IntProperty(VERSION_PROPERTY).indexed();
		final Property<String> md5Property                = new StringProperty(MD5_PROPERTY);
		final Property<String> sha1Property               = new StringProperty(SHA1_PROPERTY);
		final Property<String> sha512Property             = new StringProperty(SHA512_PROPERTY);
		final Property<String> urlProperty                = new StringProperty(URL_PROPERTY);
		final Property<Long> checksumProperty             = new LongProperty(CHECKSUM_PROPERTY).indexed();
		final Property<Long> crc32Property                = new LongProperty(CRC32_PROPERTY).indexed();
		final Property<Long> fileModificationDateProperty = new LongProperty(FILE_MODIFICATION_DATE_PROPERTY);
		final Property<Long> sizeProperty                 = new LongProperty(SIZE_PROPERTY).indexed();
		final Property<String> base64DataProperty         = new FileDataProperty(BASE64_DATA_PROPERTY).typeHint("String");

		return Set.of(
			fileParentProperty,
			contentTypeProperty,
			dontCacheProperty,
			indexedProperty,
			isFileProperty,
			isTemplateProperty,
			useAsJavascriptLibrary,
			cacheForSecondsProperty,
			positionProperty,
			versionProperty,
			md5Property,
			sha1Property,
			sha512Property,
			urlProperty,
			checksumProperty,
			crc32Property,
			fileModificationDateProperty,
			sizeProperty,
			base64DataProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					URL_PROPERTY, IS_FILE_PROPERTY, IS_TEMPLATE_PROPERTY, INDEXED_PROPERTY, SIZE_PROPERTY, FILE_MODIFICATION_DATE_PROPERTY,
					DONT_CACHE_PROPERTY, AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY, NodeInterfaceTraitDefinition.OWNER_PROPERTY,
					CONTENT_TYPE_PROPERTY, AbstractFileTraitDefinition.IS_MOUNTED_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					URL_PROPERTY, IS_FILE_PROPERTY, IS_TEMPLATE_PROPERTY, INDEXED_PROPERTY, SIZE_PROPERTY, CACHE_FOR_SECONDS_PROPERTY,
					VERSION_PROPERTY, CHECKSUM_PROPERTY, MD5_PROPERTY, DONT_CACHE_PROPERTY, AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY,
					NodeInterfaceTraitDefinition.OWNER_PROPERTY, AbstractFileTraitDefinition.HAS_PARENT_PROPERTY, AbstractFileTraitDefinition.PATH_PROPERTY,
					CONTENT_TYPE_PROPERTY, USE_AS_JAVASCRIPT_LIBRARY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType(StructrTraits.FILE);

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/File"));
		type.setImplements(Linkable.class);
		type.setImplements(JavaScriptSource.class);
		type.setExtends(URI.create("#/definitions/AbstractFile"));
		type.setCategory("ui");

		// override setProperty methods, but don't call super first (we need the previous value)
		type.overrideMethod("setProperty",                 false,  org.structr.web.entity.File.class.getName() + ".OnSetProperty(this, arg0,arg1);\n\t\treturn super.setProperty(arg0, arg1, false);");
		type.overrideMethod("setProperties",               false,  org.structr.web.entity.File.class.getName() + ".OnSetProperties(this, arg0, arg1, arg2);\n\t\tsuper.setProperties(arg0, arg1, arg2);")
				// the following lines make the overridden setProperties method more explicit in regards to its parameters
				.setReturnType("void")
				.addParameter("arg0", SecurityContext.class.getName())
				.addParameter("arg1", "java.util.Map<java.lang.String, java.lang.Object>")
				.addParameter("arg2", "boolean")
				.addException(FrameworkException.class.getName());

		type.overrideMethod("onCreation",                  true,  org.structr.web.entity.File.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",              true,  org.structr.web.entity.File.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onNodeDeletion",              true,  org.structr.web.entity.File.class.getName() + ".onNodeDeletion(this);");
		type.overrideMethod("afterCreation",               true,  org.structr.web.entity.File.class.getName() + ".afterCreation(this, arg0);");

		type.overrideMethod("increaseVersion",             false, org.structr.web.entity.File.class.getName() + ".increaseVersion(this);");
		type.overrideMethod("notifyUploadCompletion",      false, org.structr.web.entity.File.class.getName() + ".notifyUploadCompletion(this);");
		type.overrideMethod("callOnUploadHandler",         false, org.structr.web.entity.File.class.getName() + ".callOnUploadHandler(this, arg0);");

		type.overrideMethod("getInputStream",              false, "return " + org.structr.web.entity.File.class.getName() + ".getInputStream(this);");
		type.overrideMethod("getSearchContext",            false, "return " + org.structr.web.entity.File.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);");
		type.overrideMethod("getJavascriptLibraryCode",    false, "return " + org.structr.web.entity.File.class.getName() + ".getJavascriptLibraryCode(this);");
		type.overrideMethod("getEnableBasicAuth",          false, "return getProperty(enableBasicAuthProperty);");

		type.overrideMethod("getCurrentWorkingDir",        false, "return " + org.structr.web.entity.File.class.getName() + ".getCurrentWorkingDir(this);");

		// overridden methods
		final JsonMethod getOutputStream1 = type.addMethod("getOutputStream");
		getOutputStream1.setSource("return " + org.structr.web.entity.File.class.getName() + ".getOutputStream(this, notifyIndexerAfterClosing, append);");
		getOutputStream1.addParameter("notifyIndexerAfterClosing", "boolean");
		getOutputStream1.addParameter("append", "boolean");
		getOutputStream1.setReturnType(OutputStream.class.getName());

		final JsonMethod getOutputStream2 = type.addMethod("getOutputStream");
		getOutputStream2.setSource("return " + org.structr.web.entity.File.class.getName() + ".getOutputStream(this, true, false);");
		getOutputStream2.setReturnType(OutputStream.class.getName());

		type.addMethod("doCSVImport")
				.setReturnType(Long.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".doCSVImport(this, parameters, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);


		type.addMethod("doXMLImport")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
				.setReturnType(Long.class.getName())
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".doXMLImport(this, parameters, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		type.addMethod("getFirstLines")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
				.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".getFirstLines(this, parameters, ctx);")
				.setDoExport(true);

		type.addMethod("getCSVHeaders")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
				.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".getCSVHeaders(this, parameters, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		type.addMethod("getXMLStructure")
				.addParameter("ctx", SecurityContext.class.getName())
				.setReturnType("java.lang.String")
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".getXMLStructure(this);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		type.addMethod("extractStructure")
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
				.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
				.setSource("return " + org.structr.web.entity.File.class.getName() + ".extractStructure(this);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

		// view configuration
		type.addViewProperty(PropertyView.Public, "includeInFrontendExport");
		type.addViewProperty(PropertyView.Public, "owner");

		type.addViewProperty(PropertyView.Ui, "hasParent");
		type.addViewProperty(PropertyView.Ui, "path");

	}}
	*/

	private static <T> void OnSetProperty(final org.structr.web.entity.File thisFile, final PropertyKey<T> key, T value, final boolean isCreation) {

		if (isCreation) {
			return;
		}

		final Traits traits                                             = Traits.of(StructrTraits.FILE);
		final PropertyKey<StorageConfiguration> storageConfigurationKey = traits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY);
		final PropertyKey<Folder> parentKey                             = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<String> parentIdKey                           = traits.key(AbstractFileTraitDefinition.PARENT_ID_PROPERTY);

		if (key.equals(storageConfigurationKey)) {

			thisFile.checkMoveBinaryContents((NodeInterface)value);

		} else if (key.equals(parentKey)) {

			thisFile.checkMoveBinaryContents(thisFile.getParent(), (NodeInterface)value);

		} else if (key.equals(parentIdKey)) {

			NodeInterface parentFolder = null;
			try {

				parentFolder = StructrApp.getInstance().nodeQuery(StructrTraits.FOLDER).uuid((String) value).getFirst();

			} catch (FrameworkException ex) {

				LoggerFactory.getLogger(org.structr.web.entity.File.class).warn("Exception while trying to lookup parent folder.", ex);
			}

			if (parentFolder != null) {

				thisFile.checkMoveBinaryContents(thisFile.getParent(), parentFolder);
			}
		}
	}

	private static void OnSetProperties(final org.structr.web.entity.File thisFile, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

		if (isCreation) {
			return;
		}

		final Traits traits                                      = Traits.of(StructrTraits.FILE);
		final PropertyKey<NodeInterface> storageConfigurationKey = traits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY);
		final PropertyKey<NodeInterface> parentKey               = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<String> parentIdKey                    = traits.key(AbstractFileTraitDefinition.PARENT_ID_PROPERTY);

		if (properties.containsKey(storageConfigurationKey)) {

			thisFile.checkMoveBinaryContents(properties.get(storageConfigurationKey));

		} else if (properties.containsKey(parentKey)) {

			thisFile.checkMoveBinaryContents(thisFile.getParent(), properties.get(parentKey));

		} else if (properties.containsKey(parentIdKey)) {

			NodeInterface parentFolder = null;
			try {

				parentFolder = StructrApp.getInstance().nodeQuery(StructrTraits.FOLDER).uuid(properties.get(parentIdKey)).getFirst();

			} catch (FrameworkException ex) {

				LoggerFactory.getLogger(org.structr.web.entity.File.class).warn("Exception while trying to lookup parent folder.", ex);
			}

			thisFile.checkMoveBinaryContents(thisFile.getParent(), parentFolder);
		}
	}
}
