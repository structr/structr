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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.ScaleOptions;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateImageVariantFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "createImageVariant";
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
			Signature.javaScript("image [, options ]"),
			Signature.structrScript("image [, key, value, ... ]")
		);
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length < 1 || !(sources[0] instanceof NodeInterface node) || !node.is(StructrTraits.IMAGE)) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getDisplayName(false) + ": First parameter must be an Image. Found: " + (sources != null && sources.length > 0 ? sources[0] : null));
		}

		final Map<String, Object> config = getOptions(sources);
		final Image original             = node.as(Image.class);
		final ScaleOptions options       = new ScaleOptions();

		options.maxWidth  = toInteger(config.get("maxWidth"));
		options.maxHeight = toInteger(config.get("maxHeight"));
		options.scale     = toDouble(config.get("scale"));
		options.crop      = toBoolean(config.get("crop"));
		options.offsetX   = toInteger(config.get("offsetX"));
		options.offsetY   = toInteger(config.get("offsetY"));
		options.format    = config.get("format") != null ? config.get("format").toString() : null;
		options.quality   = config.get("quality") != null ? toDouble(config.get("quality")).floatValue() : null;
		options.rotate    = config.get("rotate") != null ? toInteger(config.get("rotate")) : 0;
		options.flipX     = toBoolean(config.get("flipX"));
		options.flipY     = toBoolean(config.get("flipY"));

		final Thumbnail result = ImageHelper.scaleImage(original, options, false);
		final Object parent    = config.get("parent");
		final Folder folder;

		if (parent == null) {

			folder = original.getParent();

		} else if (parent instanceof NodeInterface p && p.is(StructrTraits.FOLDER)) {

			folder = p.as(Folder.class);

		} else {

			throw new FrameworkException(422, getDisplayName(false) + ": parent must be a Folder. Found: " + parent);
		}

		final String name = config.get("name") != null ? config.get("name").toString() : getDefaultName(original, result);

		try {

			final NodeInterface variant = ImageHelper.createImageNode(ctx.getSecurityContext(), result.getBytes(), result.getContentType(), StructrTraits.IMAGE, name, false);
			final Image image           = variant.as(Image.class);

			image.setParent(folder);

			return variant;

		} catch (IOException ex) {

			throw new FrameworkException(422, getDisplayName(false) + ": Unable to store the variant of image '" + original.getName() + "': " + ex.getMessage());
		}
	}

	@Override
	public String getShortDescription() {

		return "Creates a scaled, cropped or re-encoded copy of an image as a new, standalone image.";
	}

	@Override
	public String getLongDescription() {

		return """
		The variant is created synchronously in the current transaction: it exists when the function returns, and a rollback removes it. It is an ordinary `Image` owned by the current user, not a thumbnail, so it is never recreated or deleted automatically, and its visibility flags are not copied from the original. The EXIF orientation of the original is applied, and no metadata is written to the variant.

		The size comes from `scale` or from the bounding box `maxWidth` x `maxHeight`. The variant keeps the aspect ratio and is never larger than the original. Without any size option, the image is only re-encoded.

		| Option | Description |
		| --- | --- |
		| maxWidth, maxHeight | Bounding box in pixels. One of them is enough. |
		| scale | Scale factor instead of a bounding box, e.g. `0.5`. Values above 1 do not scale up. |
		| crop | `true` scales the image until it covers the bounding box and cuts the box out. Default: `false`. |
		| offsetX, offsetY | With `crop`, the top left corner of the cut-out in pixels of the scaled image. Default: centered. With `scale: 1` and `crop: true`, this cuts a region out of the unscaled original. |
		| format | `png`, `jpeg` or `gif`. Default: the format of the original if it can be written, else `png`. Transparent areas become white in JPEG. |
		| quality | JPEG quality between `0.0` and `1.0`. Default: `0.85`. |
		| rotate | Clockwise rotation in degrees: `90`, `180` or `270` (negative values turn counter-clockwise). Applied before scaling, so the box and offsets refer to the rotated image. |
		| flipX, flipY | `true` mirrors the image left to right or top to bottom, after the rotation. |
		| name | File name of the variant. Default: the name of the original with the dimensions, e.g. `photo_1024x768.jpg`. |
		| parent | Folder of the variant. Default: the folder of the original. |

		If the image cannot be read or the variant cannot be written, the function throws an error instead of creating an empty file.

		Unlike a `Thumbnail` property, which creates its thumbnail asynchronously and manages it together with the original, this function creates the variant right away and leaves its lifecycle to the caller.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("image", "image node to create the variant of"),
			Parameter.optional("options", "map of options, see the table above (StructrScript: alternating keys and values)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
				${{
					const image = $.first($.find('Image', 'name', 'photo.jpg'));

					// downloads in three sizes, JPEG, longest edge 1024, 2048 and 3840 pixels
					for (const edge of [1024, 2048, 3840]) {
						$.createImageVariant(image, { maxWidth: edge, maxHeight: edge, format: 'jpeg', quality: 0.8 });
					}
				}}
				""", "Create downloads in reduced size"),
			Example.javaScript("""
				${{
					// a square avatar of 256 x 256 pixels, cut from the center
					$.createImageVariant($.me.avatar, { maxWidth: 256, maxHeight: 256, crop: true, name: 'avatar.png' });
				}}
				""", "Create a square, cropped variant"),
			Example.structrScript("""
				${createImageVariant(first(find('Image', 'name', 'photo.jpg')), 'scale', 0.5, 'format', 'jpeg')}
				""", "Half the size, as JPEG")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The variant is created with the type `Image`, so lifecycle methods of a custom image type (e.g. `onCreate` of a `Photo` type) do not run for it.",
			"Each call reads and decodes the original again. For many large originals, create the variants in a background job."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	// ----- private methods -----
	private Map<String, Object> getOptions(final Object[] sources) throws FrameworkException {

		if (sources.length >= 2 && sources[1] instanceof Map map) {

			return map;
		}

		final Map<String, Object> options = new HashMap<>();
		final int count                   = sources.length - 1;

		if (count % 2 != 0) {

			throw new FrameworkException(422, getDisplayName(false) + ": Options must be a map or alternating keys and values. " + usage());
		}

		for (int i = 1; i < sources.length; i += 2) {

			if (sources[i] != null) {

				options.put(sources[i].toString(), sources[i + 1]);
			}
		}

		return options;
	}

	private String getDefaultName(final Image original, final Thumbnail result) {

		return ImageHelper.getVariantFileName(original.getName(), result.getWidth(), result.getHeight(), result.getFormat());
	}

	private Integer toInteger(final Object value) throws FrameworkException {

		final Double d = toDouble(value);

		return d != null ? (int)Math.round(d) : null;
	}

	private Double toDouble(final Object value) throws FrameworkException {

		if (value == null) {
			return null;
		}

		if (value instanceof Number n) {
			return n.doubleValue();
		}

		try {

			return Double.valueOf(value.toString());

		} catch (NumberFormatException nfe) {

			throw new FrameworkException(422, getDisplayName(false) + ": Expected a number, found '" + value + "'");
		}
	}

	private boolean toBoolean(final Object value) {

		return value instanceof Boolean b ? b : (value != null && Boolean.parseBoolean(value.toString()));
	}
}
