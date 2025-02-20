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
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
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

	public FileTraitDefinition() {
		super("File");
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
						final String uuid = thisFile.getUuid();
						if (uuid != null) {

							RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
						}
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

		final Property<NodeInterface> fileParentProperty  = new StartNode("fileParent", "FolderCONTAINSFile");
		final Property<String> contentTypeProperty        = new StringProperty("contentType");
		final Property<Boolean> dontCacheProperty         = new BooleanProperty("dontCache").defaultValue(false);
		final Property<Boolean> indexedProperty           = new BooleanProperty("indexed");
		final Property<Boolean> isFileProperty            = new ConstantBooleanProperty("isFile", true).readOnly();
		final Property<Boolean> isTemplateProperty        = new BooleanProperty("isTemplate");
		final Property<Boolean> useAsJavascriptLibrary    = new BooleanProperty("useAsJavascriptLibrary").indexed();
		final Property<Integer> cacheForSecondsProperty   = new IntProperty("cacheForSeconds");
		final Property<Integer> positionProperty          = new IntProperty("position").indexed();
		final Property<Integer> versionProperty           = new IntProperty("version").indexed();
		final Property<String> md5Property                = new StringProperty("md5");
		final Property<String> sha1Property               = new StringProperty("sha1");
		final Property<String> sha512Property             = new StringProperty("sha512");
		final Property<String> urlProperty                = new StringProperty("url");
		final Property<Long> checksumProperty             = new LongProperty("checksum").indexed();
		final Property<Long> crc32Property                = new LongProperty("crc32").indexed();
		final Property<Long> fileModificationDateProperty = new LongProperty("fileModificationDate");
		final Property<Long> sizeProperty                 = new LongProperty("size").indexed();
		final Property<String> base64DataProperty         = new FileDataProperty("base64Data").typeHint("String");

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
				"includeInFrontendExport", "owner"
			),
			PropertyView.Ui,
			newSet(
				"hasParent", "path"
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
		final JsonObjectType type = schema.addType("File");

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

		final Traits traits                                             = Traits.of("File");
		final PropertyKey<StorageConfiguration> storageConfigurationKey = traits.key("storageConfiguration");
		final PropertyKey<Folder> parentKey                             = traits.key("parent");
		final PropertyKey<String> parentIdKey                           = traits.key("parentId");

		if (key.equals(storageConfigurationKey)) {

			thisFile.checkMoveBinaryContents((NodeInterface)value);

		} else if (key.equals(parentKey)) {

			thisFile.checkMoveBinaryContents(thisFile.getParent(), (NodeInterface)value);

		} else if (key.equals(parentIdKey)) {

			NodeInterface parentFolder = null;
			try {

				parentFolder = StructrApp.getInstance().nodeQuery("Folder").uuid((String) value).getFirst();

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

		final Traits traits                                      = Traits.of("File");
		final PropertyKey<NodeInterface> storageConfigurationKey = traits.key("storageConfiguration");
		final PropertyKey<NodeInterface> parentKey               = traits.key("parent");
		final PropertyKey<String> parentIdKey                    = traits.key("parentId");

		if (properties.containsKey(storageConfigurationKey)) {

			thisFile.checkMoveBinaryContents(properties.get(storageConfigurationKey));

		} else if (properties.containsKey(parentKey)) {

			thisFile.checkMoveBinaryContents(thisFile.getParent(), properties.get(parentKey));

		} else if (properties.containsKey(parentIdKey)) {

			NodeInterface parentFolder = null;
			try {

				parentFolder = StructrApp.getInstance().nodeQuery("Folder").uuid(properties.get(parentIdKey)).getFirst();

			} catch (FrameworkException ex) {

				LoggerFactory.getLogger(org.structr.web.entity.File.class).warn("Exception while trying to lookup parent folder.", ex);
			}

			thisFile.checkMoveBinaryContents(thisFile.getParent(), parentFolder);
		}
	}
}
