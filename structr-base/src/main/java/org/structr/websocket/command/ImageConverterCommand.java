/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.ScaleOptions;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.Image;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Websocket command of the image editor: rotates, flips, crops, resizes and converts an image on the server.
 *
 * Data keys: rotate (clockwise, a multiple of 90), flipX, flipY, offsetX, offsetY, width and height (the crop region in
 * pixels of the rotated and flipped image), maxWidth and maxHeight (a bounding box for the result), format, quality,
 * replace (write the result into the original instead of a new image next to it) and preview with previewWidth and
 * previewHeight (return the rotated, flipped and cropped image as a data URL, scaled into that box, without storing anything).
 */
public class ImageConverterCommand extends AbstractCommand {

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final Image original = getNodeAs(webSocketData.getId(), Image.class, StructrTraits.IMAGE);
		if (original == null) {

			getWebSocket().send(MessageBuilder.status().code(400).message("No image with id " + webSocketData.getId()).build(), true);
			return;
		}

		final Map<String, Object> data = webSocketData.getNodeData();
		final String label             = "'" + original.getName() + "' (" + original.getUuid() + ")";
		final boolean preview          = isTrue(data.get("preview"));
		final boolean replace          = isTrue(data.get("replace"));
		final ScaleOptions options     = new ScaleOptions();

		options.rotate  = toInteger(data.get("rotate"), 0);
		options.flipX   = isTrue(data.get("flipX"));
		options.flipY   = isTrue(data.get("flipY"));
		options.quality = data.get("quality") instanceof Number n ? n.floatValue() : null;

		options.regionX      = toInteger(data.get("offsetX"), null);
		options.regionY      = toInteger(data.get("offsetY"), null);
		options.regionWidth  = toInteger(data.get("width"), null);
		options.regionHeight = toInteger(data.get("height"), null);

		final BufferedImage upright = ImageHelper.readUprightImage(original);

		if (preview) {

			// rotated, flipped and cropped; fullWidth and fullHeight are those of the whole rotated image
			options.maxWidth  = toInteger(data.get("previewWidth"), null);
			options.maxHeight = toInteger(data.get("previewHeight"), null);

			final Thumbnail tn      = ImageHelper.scaleImage(upright, options, upright.getColorModel().hasAlpha() ? "png" : "jpeg", label);
			final boolean swap      = Math.abs(options.rotate) % 180 == 90;
			final Map<String, Object> result = new LinkedHashMap<>();

			result.put("preview",       "data:" + tn.getContentType() + ";base64," + Base64.getEncoder().encodeToString(tn.getBytes()));
			result.put("previewWidth",  tn.getWidth());
			result.put("previewHeight", tn.getHeight());
			result.put("fullWidth",     swap ? upright.getHeight() : upright.getWidth());
			result.put("fullHeight",    swap ? upright.getWidth() : upright.getHeight());

			getWebSocket().send(MessageBuilder.finished().callback(callback).data(result).build(), true);
			return;
		}

		options.maxWidth     = toInteger(data.get("maxWidth"), null);
		options.maxHeight    = toInteger(data.get("maxHeight"), null);

		final String format;

		if (replace) {

			// the original keeps its name, so it keeps its format too
			format = ImageHelper.normalizeFormat(ImageHelper.getImageFormatString(original));
			if (format == null) {

				throw new FrameworkException(422, "Unable to replace image " + label + ": its format cannot be written, save the result as a copy instead");
			}

		} else {

			format = ImageHelper.resolveFormat(data.get("format") != null ? data.get("format").toString() : null, original, label);
		}

		final Thumbnail tn               = ImageHelper.scaleImage(upright, options, format, label);
		final Map<String, Object> result = new LinkedHashMap<>();

		try {

			if (replace) {

				ImageHelper.setImageData(original, tn.getBytes(), tn.getContentType());

				final Traits traits          = Traits.of(StructrTraits.IMAGE);
				final PropertyMap properties = new PropertyMap();

				// the new content is upright and carries no EXIF orientation
				properties.put(traits.key(ImageTraitDefinition.WIDTH_PROPERTY),       tn.getWidth());
				properties.put(traits.key(ImageTraitDefinition.HEIGHT_PROPERTY),      tn.getHeight());
				properties.put(traits.key(ImageTraitDefinition.ORIENTATION_PROPERTY), 1);

				original.setProperties(original.getSecurityContext(), properties);

				result.put("id",   original.getUuid());
				result.put("name", original.getName());

			} else {

				final String name           = ImageHelper.getVariantFileName(original.getName(), tn.getWidth(), tn.getHeight(), tn.getFormat());
				final NodeInterface variant = ImageHelper.createImageNode(original.getSecurityContext(), tn.getBytes(), tn.getContentType(), StructrTraits.IMAGE, name, false);

				variant.as(Image.class).setParent(original.getParent());

				result.put("id",   variant.getUuid());
				result.put("name", name);
			}

		} catch (IOException ex) {

			throw new FrameworkException(422, "Unable to store the edited image " + label + ": " + ex.getMessage());
		}

		result.put("width",  tn.getWidth());
		result.put("height", tn.getHeight());

		getWebSocket().send(MessageBuilder.finished().callback(callback).data(result).build(), true);
	}

	@Override
	public String getCommand() {

		return "CONVERT_IMAGE";
	}

	// ----- private methods -----
	private static boolean isTrue(final Object value) {

		return value instanceof Boolean b ? b : (value != null && Boolean.parseBoolean(value.toString()));
	}

	private static Integer toInteger(final Object value, final Integer defaultValue) {

		if (value instanceof Number n) {
			return (int)Math.round(n.doubleValue());
		}

		if (value != null) {

			try {
				return (int)Math.round(Double.parseDouble(value.toString()));
			} catch (NumberFormatException ignore) {}
		}

		return defaultValue;
	}
}
