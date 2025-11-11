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
package org.structr.web.function;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarcodeFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "barcode";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("type, data [, width, height, hintKey, hintValue ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String barcodeType = sources[0].toString();
			final String barcodeData = sources[1].toString();
			final Number width      = (sources.length >= 3) ? (Number)sources[2] : 200;
			final Number height     = (sources.length >= 4) ? (Number)sources[3] : 200;

			final Map<String, Object> hints = (sources.length >= 5 && sources[4] instanceof Map) ? (Map)sources[4] : parseParametersAsMap(sources, 4);

			return BarcodeFunction.getQRCode(barcodeData, barcodeType, width, height, hints);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	public static String getQRCode(final String barcodeData, final String barcodeType, final Number width, final Number height, final Map<String, Object> hints) {

		if (barcodeData != null) {

			final MultiFormatWriter barcodeWriter = new MultiFormatWriter();

			try {

				BitMatrix bitMatrix = barcodeWriter.encode(barcodeData, BarcodeFormat.valueOf(barcodeType), width.intValue(), height.intValue(), BarcodeFunction.parseHints(hints));

				final ByteArrayOutputStream baos = new ByteArrayOutputStream();

				ImageIO.write(MatrixToImageWriter.toBufferedImage(bitMatrix), "PNG", baos);

				return baos.toString(StandardCharsets.ISO_8859_1);

			} catch(WriterException we) {

				logger.warn("barcode(): WriterException while encoding barcode: {} {}", barcodeType, barcodeData, we);

			} catch(IOException ioe) {

				logger.warn("barcode(): IOException", ioe);

			}
		}

		return "";
	}

	public Map<String, Object> parseParametersAsMap(final Object[] sources, final int startIndex) throws FrameworkException {

		final int parameter_count = sources.length - startIndex;

		if (parameter_count % 2 != 0) {

			throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". " + usage(true));
		}

		final Map<String, Object> params = new HashMap();

		for (int i = startIndex; i < sources.length; i += 2) {

			params.put(sources[i].toString(), sources[i+1]);
		}

		return params;
	}

	public static Map<EncodeHintType, Object> parseHints(final Map<String, Object> suppliedHints) {

		final Map<EncodeHintType, Object> hints = new HashMap();

		for(final Map.Entry<String, Object> hint : suppliedHints.entrySet()) {

			final Object obj = hint.getValue();

			// All hints that are of type Number are Integers (since internally everything is handled as Double, we need to convert this)
			if (obj instanceof Number) {
				hints.put(EncodeHintType.valueOf(hint.getKey()), ((Number)obj).intValue());
			} else {
				hints.put(EncodeHintType.valueOf(hint.getKey()), obj);
			}
		}

		return hints;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${ barcode(type, data[, width, height[, hintKey, hintValue]]) }"),
			Usage.javaScript("Usage: ${{ Structr.barcode(type, data[, width, height[, hintsMap]]); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates a barcode of given type with the given data.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
