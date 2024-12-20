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
package org.structr.websocket.command;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.IOException;

/**
 * Websocket command for image conversion tasks.
 */
public class ImageConverterCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ImageConverterCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String originalImageId          = webSocketData.getId();
		final Image originalImage             = (Image) getNode(originalImageId);

		final String format = webSocketData.getNodeDataStringValue("format");
		final int width     = webSocketData.getNodeDataIntegerValue("width");
		final int height    = webSocketData.getNodeDataIntegerValue("height");
		final int offsetX   = webSocketData.getNodeDataIntegerValue("offsetX");
		final int offsetY   = webSocketData.getNodeDataIntegerValue("offsetY");

		if (originalImage != null) {

			final Thumbnail thumbnailData = ImageHelper.createCroppedImage(originalImage, width, height, offsetX, offsetY, format);

			if (thumbnailData != null) {

				final Integer tnWidth  = thumbnailData.getWidth();
				final Integer tnHeight = thumbnailData.getHeight();
				byte[] data;

				try {

					data = thumbnailData.getBytes();
					final String thumbnailName = ImageHelper.getVariantName(originalImage.getName(), tnWidth, tnHeight, "_cropped_");

					// create image variant
					final NodeInterface imageVariant = ImageHelper.createImageNode(originalImage.getSecurityContext(), data, "image/" + Thumbnail.Format.png, "Image", thumbnailName, false);

					// store in same parent folder
					imageVariant.as(Image.class).setParent(originalImage.getParent());

				} catch (IOException | FrameworkException ex) {

					getWebSocket().send(MessageBuilder.status().code(400).message("Could not create converted image for " + originalImageId).build(), true);

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(400).message("Could not create converted image for " + originalImageId).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("No id of the original image given").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CONVERT_IMAGE";

	}

}
