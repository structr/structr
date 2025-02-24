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
package org.structr.web.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Agent;
import org.structr.agent.ReturnValue;
import org.structr.agent.Task;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Image;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ThumbnailAgent extends Agent<ThumbnailWorkObject> {

	public static final Logger logger                        = LoggerFactory.getLogger(ThumbnailAgent.class);
	public static final String TASK_NAME                     = "ThumbnailAgent";
	public static final Set queuedImageUUIDs                 = Collections.synchronizedSet(new HashSet<>());

	public ThumbnailAgent() {

		setName(TASK_NAME);
		setDaemon(true);
	}

	@Override
	public ReturnValue processTask(Task<ThumbnailWorkObject> task) throws Throwable {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		securityContext.disablePreventDuplicateRelationships();

		if (TASK_NAME.equals(task.getType())) {

			for (ThumbnailWorkObject wo : task.getWorkObjects()) {

				logger.debug("Creating thumbnail for image {} with width:{} and height:{}, cropToFit:{}", wo.getOriginalImageId(), wo.getMaxWidth(), wo.getMaxHeight(), wo.isCropToFit());
				createThumbnail(securityContext, wo.getOriginalImageId(), wo.getMaxWidth(), wo.getMaxHeight(), wo.isCropToFit());
			}

			return ReturnValue.Success;
		}

		return ReturnValue.Abort;
	}

	@Override
	public Class getSupportedTaskType() {

		return ThumbnailTask.class;
	}

	/**  Private Methods  **/
	private static void createThumbnail(final SecurityContext securityContext, final String imageUuid, final int maxWidth, final int maxHeight, final boolean cropToFit) {

		final Logger logger = LoggerFactory.getLogger(Image.class);
		final App app = StructrApp.getInstance();

		synchronized (queuedImageUUIDs) {

			if (queuedImageUUIDs.contains(imageUuid)) {

				return;
			}
		}

		try (final Tx tx = app.tx()) {

			final String thumbnailRel = "ImageTHUMBNAILImage";
			final NodeInterface node  = app.nodeQuery(StructrTraits.IMAGE).uuid(imageUuid).getFirst();
			NodeInterface thumbnail   = null;

			if (node == null) {
				return;
			}

			final Image originalImage = node.as(Image.class);

			if (originalImage.getExistingThumbnail(maxWidth, maxHeight, cropToFit) != null) {

				return;
			}

			synchronized (queuedImageUUIDs) {

				queuedImageUUIDs.add(imageUuid);
			}

			final ImageHelper.Thumbnail thumbnailData = ImageHelper.createThumbnail(originalImage, maxWidth, maxHeight, cropToFit);
			if (thumbnailData != null) {

				final Integer tnWidth  = thumbnailData.getWidth();
				final Integer tnHeight = thumbnailData.getHeight();
				byte[] data            = null;

				try {

					data = thumbnailData.getBytes();
					final String thumbnailName = ImageHelper.getThumbnailName(originalImage.getName(), tnWidth, tnHeight);

					// create thumbnail node
					thumbnail = ImageHelper.createImageNode(securityContext, data, "image/" + ImageHelper.Thumbnail.defaultFormat, StructrTraits.IMAGE, thumbnailName, true);

				} catch (IOException ex) {

					logger.warn("Could not create thumbnail image for " + originalImage.getUuid(), ex);
				}

				if (thumbnail != null && data != null) {

					// Create a thumbnail relationship
					final PropertyMap relProperties = new PropertyMap();
					relProperties.put(Traits.of(StructrTraits.IMAGE).key("width"),                  tnWidth);
					relProperties.put(Traits.of(StructrTraits.IMAGE).key("height"),                 tnHeight);
					relProperties.put(Traits.of(StructrTraits.IMAGE).key("checksum"),               originalImage.getChecksum());

					// We have to store the specs here in order to find existing thumbnails based on the specs they've been created for, not actual dimensions.
					relProperties.put(new IntProperty("maxWidth"),                           maxWidth);
					relProperties.put(new IntProperty("maxHeight"),                          maxHeight);
					relProperties.put(new BooleanProperty( "cropToFit"),                     cropToFit);

					app.create(node, thumbnail, thumbnailRel, relProperties);

					// Create thumbnail Image node
					final PropertyMap properties = new PropertyMap();
					properties.put(Traits.of(StructrTraits.IMAGE).key("width"),                               tnWidth);
					properties.put(Traits.of(StructrTraits.IMAGE).key("height"),                              tnHeight);
					properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key("hidden"),                      originalImage.isHidden());
					properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToAuthenticatedUsers"), originalImage.isVisibleToAuthenticatedUsers());
					properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key("visibleToPublicUsers"),        originalImage.isVisibleToPublicUsers());
					properties.put(Traits.of(StructrTraits.FILE).key("size"),                                 Long.valueOf(data.length));
					properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key("owner"),                       originalImage.as(AccessControllable.class).getOwnerNode());
					properties.put(Traits.of(StructrTraits.FILE).key("parent"),                               originalImage.getThumbnailParentFolder(originalImage.getParent(), securityContext));
					properties.put(Traits.of(StructrTraits.FILE).key("hasParent"),                            originalImage.getProperty(Traits.of(StructrTraits.IMAGE).key("hasParent")));

					thumbnail.unlockSystemPropertiesOnce();
					thumbnail.setProperties(securityContext, properties);
				}

			} else {

				logger.warn("Could not create thumbnail for image {} ({})", originalImage.getName(), imageUuid);

				// mark file so we don't try to create a thumbnail again
				originalImage.setProperty(Traits.of(StructrTraits.IMAGE).key("thumbnailCreationFailed"), true);
			}

			originalImage.unlockSystemPropertiesOnce();
			originalImage.setIsCreatingThumb(false);

			synchronized (queuedImageUUIDs) {

				queuedImageUUIDs.remove(imageUuid);
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();

			logger.warn("Unable to create thumbnail for " + imageUuid, t);

		}
	}
}
