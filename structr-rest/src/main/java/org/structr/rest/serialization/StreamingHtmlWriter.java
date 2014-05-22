/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.serialization;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.structr.common.SecurityContext;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class StreamingHtmlWriter extends StreamingWriter {

	public StreamingHtmlWriter(final Value<String> propertyView, final boolean indent, final int outputNestingDepth) {
		super(propertyView, indent, outputNestingDepth);
	}

	@Override
	public RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer) {
		return new StructrJsonHtmlWriter(securityContext, new PrintWriter(new BufferedWriter(writer)));
	}
}