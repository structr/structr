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

import org.structr.common.error.ArgumentTypeException;
import org.structr.core.GraphObject;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;

/**
 * The request body of an outbound HTTP call, which may be text or binary.
 *
 * A File is sent as its content rather than as its string representation, so an upload does not have to
 * go through base64 or a multipart envelope. Anything else keeps the previous behaviour of being sent as
 * text, so no existing call changes meaning.
 */
public class HttpBody {

	/**
	 * The value to hand to HttpHelper: a stream for a File, the bytes for a byte[], text otherwise.
	 *
	 * Note that a File becomes a NON-REPEATABLE stream. HttpClient sends basic credentials only after a
	 * 401 challenge, and it cannot replay a stream to answer one, so a File body with username/password
	 * needs `preemptive: true`. The same applies to following a redirect.
	 */
	public static Object of(final Object source) {

		if (source == null) {

			return null;
		}

		if (source instanceof byte[]) {

			return source;
		}

		if (source instanceof GraphObject obj && obj.is(File.class.getSimpleName())) {

			return StorageProviderFactory.getStorageProvider(obj.as(AbstractFile.class)).getInputStream();
		}

		return source.toString();
	}

	/** Whether this body cannot be sent twice, so a 401 challenge or a redirect would lose it. */
	public static boolean isStream(final Object body) {

		return body instanceof java.io.InputStream;
	}

	/**
	 * Refuses a combination that cannot work: a streamed body with credentials that are not preemptive.
	 *
	 * HttpClient sends basic credentials only after the server has answered 401, which means sending the
	 * request twice. A stream cannot be replayed, so the second attempt would send an empty body and the
	 * upload would fail in a way that looks like a server problem. Failing here names the fix instead.
	 *
	 * Not solved by turning preemptive on automatically: that would send the credentials to a server that
	 * never asked for them, which is the caller's decision to make and not ours.
	 */
	public static void checkRepeatable(final String functionName, final Object body, final HttpOptions options) {

		if (isStream(body) && options.getString(HttpOptions.USERNAME) != null && !options.getBoolean(HttpOptions.PREEMPTIVE, false)) {

			throw new ArgumentTypeException(functionName + "(): a File body cannot be sent with username/password unless "
				+ "{ preemptive: true } is set. The credentials are otherwise sent only after a 401, which requires "
				+ "sending the body a second time, and a stream cannot be read twice.");
		}
	}
}
