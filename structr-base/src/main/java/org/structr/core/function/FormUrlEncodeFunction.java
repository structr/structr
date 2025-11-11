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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FormUrlEncodeFunction extends CoreFunction {

	@Override
	public String getName() {
		return "formurlencode";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("object");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final StringBuilder buf = new StringBuilder();
			final Object obj        = sources[0];

			try {
				recursivelyEncodeObject(buf, null, obj, 0);

			} catch (UnsupportedEncodingException uex) {

				logger.warn("Unsupported encoding exception: {}", uex.getMessage());
			}

			return buf.toString();

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{formurlencode(object)}}. Example: ${{formurlencode(data)}}"),
			Usage.structrScript("Usage: ${formurlencode(object)}. Example: ${formurlencode(data)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Encodes the given object to an application/x-www-form-urlencoded string";
	}

	// ----- private methods -----
	private void recursivelyEncodeObject(final StringBuilder buf, final String key, final Object root, final int level) throws UnsupportedEncodingException {

		if (root != null) {

			if (root instanceof Map) {

				for (final Entry<String, Object> entry : ((Map<String, Object>)root).entrySet()) {

					final String mapKey      = entry.getKey();
					final String compoundKey = level > 0 ? key + '[' + mapKey + ']' : mapKey;

					recursivelyEncodeObject(buf, compoundKey, entry.getValue(), level + 1);
				}

			} else if (root instanceof Iterable) {

				int index = 0;

				for (final Object o : (Iterable)root) {

					recursivelyEncodeObject(buf, key + "[" + index++ + "]", o, level + 1);
				}

			} else {

				// append separator
				buf.append(buf.length() == 0 ? "" : "&");

				buf.append(key);
				buf.append("=");

				// special handling for numbers
				if (root instanceof Number) {

					final Number num = (Number)root;

					// serialize integers without decimals
					if ((num.doubleValue() % 1) == 0) {

						buf.append(num.longValue());

					} else {

						buf.append(num.doubleValue());
					}

				} else {

					buf.append(URLEncoder.encode(root.toString(), "utf-8"));
				}
			}
		}
	}
}
