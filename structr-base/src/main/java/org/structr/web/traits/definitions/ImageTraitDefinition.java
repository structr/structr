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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.propertycontainer.SetProperties;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.schema.SchemaService;
import org.structr.web.agent.ThumbnailTask;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;
import org.structr.web.traits.wrappers.ImageTraitWrapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An image whose binary data will be stored on disk.
 */
public class ImageTraitDefinition extends AbstractTraitDefinition {

	private static final Property<NodeInterface> imageParentProperty          = new StartNode("imageParent", "FolderCONTAINSImage").partOfBuiltInSchema();
	private static final Property<NodeInterface> imageOfUser                  = new EndNode("imageOfUser", "ImagePICTURE_OFUser").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> thumbnailsProperty = new EndNodes("thumbnails", "ImageTHUMBNAILImage").partOfBuiltInSchema();
	private static final Property<NodeInterface> originalImageProperty        = new StartNode("originalImage", "ImageTHUMBNAILImage").partOfBuiltInSchema();
	private static final Property<NodeInterface> tnMidProperty                = new ThumbnailProperty("tnMid").format("300, 300, false").typeHint("Image").partOfBuiltInSchema().dynamic();
	private static final Property<NodeInterface> tnSmallProperty              = new ThumbnailProperty("tnSmall").format("100, 100, false").typeHint("Image").partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> isCreatingThumbProperty            = new BooleanProperty("isCreatingThumb").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> isImageProperty                    = new BooleanProperty("isImage").readOnly().transformators("org.structr.common.ConstantBooleanTrue").partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> isThumbnailProperty                = new BooleanProperty("isThumbnail").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> thumbnailCreationFailedProperty    = new BooleanProperty("thumbnailCreationFailed").partOfBuiltInSchema().dynamic();
	private static final Property<Integer> heightProperty                     = new IntProperty("height").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Integer> orientationProperty                = new IntProperty("orientation").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Integer> widthProperty                      = new IntProperty("width").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<String> exifIFD0DataProperty                = new StringProperty("exifIFD0Data").partOfBuiltInSchema().dynamic();
	private static final Property<String> exifSubIFDDataProperty              = new StringProperty("exifSubIFDData").partOfBuiltInSchema().dynamic();
	private static final Property<String> gpsDataProperty                     = new StringProperty("gpsData").partOfBuiltInSchema().dynamic();
	private static final Property<String> imageDataProperty                   = new ImageDataProperty("imageData").typeHint("String").partOfBuiltInSchema().dynamic();


	/*
	View publicView = new View(Image.class, PropertyView.Public, parentProperty);
	View uiView     = new View(Image.class, PropertyView.Ui,     parentProperty);
	*/


	public ImageTraitDefinition() {
		super("Image");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnModification.class,
			new OnModification() {
				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Image thisImage = ((NodeInterface) graphObject).as(Image.class);
					if ( !thisImage.isThumbnail() && !thisImage.isTemplate() ) {

						if (modificationQueue.isPropertyModified(graphObject, Traits.nameProperty())) {

							final String newImageName = thisImage.getName();

							for (Image tn : thisImage.getThumbnails()) {

								final String expectedThumbnailName = ImageHelper.getThumbnailName(newImageName, tn.getWidth(), tn.getHeight());
								final String currentThumbnailName  = tn.getName();

								if ( !expectedThumbnailName.equals(currentThumbnailName) ) {

									final Logger logger = LoggerFactory.getLogger(Image.class);
									logger.debug("Auto-renaming Thumbnail({}) from '{}' to '{}'", tn.getUuid(), currentThumbnailName, expectedThumbnailName);

									tn.setName(expectedThumbnailName);

								}
							}
						}
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

					final String keyName = key.jsonName();

					// Copy visibility properties and owner to all thumbnails
					if ("visibleToPublicUsers".equals(keyName) || "visibleToAuthenticatedUsers".equals(keyName) || "owner".equals(keyName)) {

						final Image thisImage = ((NodeInterface) graphObject).as(Image.class);

						for (final Image tn : thisImage.getThumbnails()) {

							if (!tn.getUuid().equals(thisImage.getUuid())) {

								tn.getWrappedNode().setProperty(key, value);
							}
						}
					}

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			},

			SetProperties.class,
			new SetProperties() {

				@Override
				public void setProperties(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {

					final Image thisImage = ((NodeInterface) graphObject).as(Image.class);

					if ( !thisImage.isThumbnail() ) {

						final PropertyMap propertiesCopiedToAllThumbnails = new PropertyMap();

						for (final PropertyKey key : properties.keySet()) {

							final String keyName = key.jsonName();

							if ("visibleToPublicUsers".equals(keyName) || "visibleToAuthenticatedUsers".equals(keyName) || "owner".equals(keyName)) {

								propertiesCopiedToAllThumbnails.put(key, properties.get(key));
							}
						}

						if ( !propertiesCopiedToAllThumbnails.isEmpty() ) {

							for (final Image tn : thisImage.getThumbnails()) {

								if (!tn.getUuid().equals(thisImage.getUuid())) {

									final NodeInterface wrappedNode = tn.getWrappedNode();
									final SecurityContext sc        = wrappedNode.getSecurityContext();

									wrappedNode.setProperties(sc, propertiesCopiedToAllThumbnails);
								}
							}
						}
					}

					getSuper().setProperties(graphObject, securityContext, properties, isCreation);
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
			Image.class, (traits, node) -> new ImageTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			imageParentProperty,
			imageOfUser,
			thumbnailsProperty,
			originalImageProperty,
			tnMidProperty,
			tnSmallProperty,
			isCreatingThumbProperty,
			isImageProperty,
			isThumbnailProperty,
			thumbnailCreationFailedProperty,
			heightProperty,
			orientationProperty,
			widthProperty,
			exifIFD0DataProperty,
			exifSubIFDDataProperty,
			gpsDataProperty,
			imageDataProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*

	// TODO: sysinternal and unvalidated properties are not possible right now
	image.overrideMethod("isImage",              false, "return getProperty(isImageProperty);");
	image.overrideMethod("isThumbnail",          false, "return getProperty(isThumbnailProperty);");
	image.overrideMethod("getOriginalImageName", false, "return " + Image.class.getName() + ".getOriginalImageName(this);");
	image.overrideMethod("setProperty",          true,  "return " + Image.class.getName() + ".setProperty(this, arg0, arg1);");
	image.overrideMethod("onModification",       true,  Image.class.getName() + ".onModification(this, arg0, arg1, arg2);");
	image.overrideMethod("setProperties",        true,  Image.class.getName() + ".setProperties(this, arg0, arg1);");
	image.overrideMethod("isGranted",            false, "if (this.isThumbnail()) { final org.structr.web.entity.Image originalImage = getOriginalImage(); if (originalImage != null) { return originalImage.isGranted(arg0, arg1); } } return super.isGranted(arg0, arg1);");

	final JsonMethod getScaledImage1 = image.addMethod("getScaledImage");
	getScaledImage1.setReturnType(Image.class.getName());
	getScaledImage1.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
	getScaledImage1.addParameter("arg0", "String");
	getScaledImage1.addParameter("arg1", "String");

	final JsonMethod getScaledImage2 = image.addMethod("getScaledImage");
	getScaledImage2.setReturnType(Image.class.getName());
	getScaledImage2.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
	getScaledImage2.addParameter("arg0", "String");
	getScaledImage2.addParameter("arg1", "String");
	getScaledImage2.addParameter("arg2", "boolean");

	final JsonMethod getScaledImage3 = image.addMethod("getScaledImage");
	getScaledImage3.setReturnType(Image.class.getName());
	getScaledImage3.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1);");
	getScaledImage3.addParameter("arg0", "int");
	getScaledImage3.addParameter("arg1", "int");

	final JsonMethod getScaledImage4 = image.addMethod("getScaledImage");
	getScaledImage4.setReturnType(Image.class.getName());
	getScaledImage4.setSource("return "+ Image.class.getName() + ".getScaledImage(this, arg0, arg1, arg2);");
	getScaledImage4.addParameter("arg0", "int");
	getScaledImage4.addParameter("arg1", "int");
	getScaledImage4.addParameter("arg2", "boolean");

	// view configuration
	image.addViewProperty(PropertyView.Public, "parent");
	 */
}
