/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.accesscontrollable.IsGranted;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.propertycontainer.SetProperties;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Image;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.ThumbnailProperty;
import org.structr.web.traits.wrappers.ImageTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * An image whose binary data will be stored on disk.
 */
public class ImageTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String IMAGE_PARENT_PROPERTY              = "imageParent";
	public static final String IMAGE_OF_USER_PROPERTY             = "imageOfUser";
	public static final String THUMBNAILS_PROPERTY                = "thumbnails";
	public static final String ORIGINAL_IMAGE_PROPERTY            = "originalImage";
	public static final String TN_MID_PROPERTY                    = "tnMid";
	public static final String TN_SMALL_PROPERTY                  = "tnSmall";
	public static final String IS_CREATING_THUMB_PROPERTY         = "isCreatingThumb";
	public static final String IS_IMAGE_PROPERTY                  = "isImage";
	public static final String IS_THUMBNAIL_PROPERTY              = "isThumbnail";
	public static final String THUMBNAIL_CREATION_FAILED_PROPERTY = "thumbnailCreationFailed";
	public static final String HEIGHT_PROPERTY                    = "height";
	public static final String ORIENTATION_PROPERTY               = "orientation";
	public static final String WIDTH_PROPERTY                     = "width";
	public static final String EXIF_IFD0_DATA_PROPERTY            = "exifIFD0Data";
	public static final String EXIF_SUB_IFD_DATA_PROPERTY         = "exifSubIFDData";
	public static final String GPS_DATA_PROPERTY                  = "gpsData";
	public static final String IMAGE_DATA_PROPERTY                = "imageData";

	public ImageTraitDefinition() {
		super(StructrTraits.IMAGE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Image thisImage = graphObject.as(Image.class);
					if ( !thisImage.isThumbnail() && !thisImage.isTemplate() ) {

						if (modificationQueue.isPropertyModified(graphObject, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))) {

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
					if (GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY.equals(keyName) || GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY.equals(keyName) || NodeInterfaceTraitDefinition.OWNER_PROPERTY.equals(keyName)) {

						final Image thisImage = graphObject.as(Image.class);

						for (final Image tn : thisImage.getThumbnails()) {

							if (!tn.getUuid().equals(thisImage.getUuid())) {

								tn.setProperty(key, value);
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

					final Image thisImage = graphObject.as(Image.class);

					if (!thisImage.isThumbnail()) {

						final PropertyMap propertiesCopiedToAllThumbnails = new PropertyMap();

						for (final PropertyKey key : properties.keySet()) {

							final String keyName = key.jsonName();

							if (GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY.equals(keyName) || GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY.equals(keyName) || NodeInterfaceTraitDefinition.OWNER_PROPERTY.equals(keyName)) {

								propertiesCopiedToAllThumbnails.put(key, properties.get(key));
							}
						}

						if (!propertiesCopiedToAllThumbnails.isEmpty()) {

							for (final Image tn : thisImage.getThumbnails()) {

								if (!tn.getUuid().equals(thisImage.getUuid())) {

									final NodeInterface wrappedNode = tn;
									final SecurityContext sc = wrappedNode.getSecurityContext();

									wrappedNode.setProperties(sc, propertiesCopiedToAllThumbnails);
								}
							}
						}
					}

					getSuper().setProperties(graphObject, securityContext, properties, isCreation);
				}
			},

			IsGranted.class,
			new IsGranted() {

				@Override
				public boolean isGranted(final NodeInterface graphObject, final Permission permission, final SecurityContext securityContext, final boolean isCreation) {

					final Image thisImage = graphObject.as(Image.class);

					if (thisImage.isThumbnail()) {

						final org.structr.web.entity.Image originalImage = thisImage.getOriginalImage();
						if (originalImage != null) {

							return originalImage.isGranted(permission, securityContext, isCreation);
						}
					}

					return getSuper().isGranted(graphObject, permission, securityContext, isCreation);
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> imageParentProperty          = new StartNode(traitsInstance, IMAGE_PARENT_PROPERTY, StructrTraits.FOLDER_CONTAINS_IMAGE);
		final Property<NodeInterface> imageOfUser                  = new EndNode(traitsInstance, IMAGE_OF_USER_PROPERTY, StructrTraits.IMAGE_PICTURE_OF_USER);
		final Property<Iterable<NodeInterface>> thumbnailsProperty = new EndNodes(traitsInstance, THUMBNAILS_PROPERTY, StructrTraits.IMAGE_THUMBNAIL_IMAGE);
		final Property<NodeInterface> originalImageProperty        = new StartNode(traitsInstance, ORIGINAL_IMAGE_PROPERTY, StructrTraits.IMAGE_THUMBNAIL_IMAGE);
		final Property<NodeInterface> tnMidProperty                = new ThumbnailProperty(TN_MID_PROPERTY).format("300, 300, false").typeHint(StructrTraits.IMAGE);
		final Property<NodeInterface> tnSmallProperty              = new ThumbnailProperty(TN_SMALL_PROPERTY).format("100, 100, false").typeHint(StructrTraits.IMAGE);
		final Property<Boolean> isCreatingThumbProperty            = new BooleanProperty(IS_CREATING_THUMB_PROPERTY).indexed();
		final Property<Boolean> isImageProperty                    = new ConstantBooleanProperty(IS_IMAGE_PROPERTY, true).readOnly();
		final Property<Boolean> isThumbnailProperty                = new BooleanProperty(IS_THUMBNAIL_PROPERTY).indexed();
		final Property<Boolean> thumbnailCreationFailedProperty    = new BooleanProperty(THUMBNAIL_CREATION_FAILED_PROPERTY);
		final Property<Integer> heightProperty                     = new IntProperty(HEIGHT_PROPERTY).indexed();
		final Property<Integer> orientationProperty                = new IntProperty(ORIENTATION_PROPERTY).indexed();
		final Property<Integer> widthProperty                      = new IntProperty(WIDTH_PROPERTY).indexed();
		final Property<String> exifIFD0DataProperty                = new StringProperty(EXIF_IFD0_DATA_PROPERTY);
		final Property<String> exifSubIFDDataProperty              = new StringProperty(EXIF_SUB_IFD_DATA_PROPERTY);
		final Property<String> gpsDataProperty                     = new StringProperty(GPS_DATA_PROPERTY);
		final Property<String> imageDataProperty                   = new ImageDataProperty(IMAGE_DATA_PROPERTY).typeHint("String").disableSerialization(true);

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
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					EXIF_IFD0_DATA_PROPERTY, EXIF_SUB_IFD_DATA_PROPERTY, GPS_DATA_PROPERTY, HEIGHT_PROPERTY, WIDTH_PROPERTY,
					IS_IMAGE_PROPERTY, IS_THUMBNAIL_PROPERTY, ORIENTATION_PROPERTY, TN_SMALL_PROPERTY, TN_MID_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					EXIF_IFD0_DATA_PROPERTY, EXIF_SUB_IFD_DATA_PROPERTY, GPS_DATA_PROPERTY, HEIGHT_PROPERTY, WIDTH_PROPERTY,
					IS_IMAGE_PROPERTY, IS_THUMBNAIL_PROPERTY, ORIENTATION_PROPERTY, TN_SMALL_PROPERTY, TN_MID_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
