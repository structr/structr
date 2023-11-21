/*
 * Copyright (C) 2010-2024 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.servlet;

import org.structr.common.SecurityContext;
import org.structr.rest.serialization.RestWriter;
import org.structr.rest.serialization.StreamingWriter;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 *
 *
 */
public class StreamingFlowWriter extends StreamingWriter {

	public StreamingFlowWriter(final String propertyView, final boolean indent, final int outputNestingDepth, final boolean wrapSingleResultInArray, final boolean serializeNulls) {
		super(propertyView, indent, outputNestingDepth, wrapSingleResultInArray, serializeNulls);
	}

	@Override
	public RestWriter getRestWriter(final SecurityContext securityContext, final Writer writer) {
		return new StructrFlowHtmlWriter(securityContext, new PrintWriter(new BufferedWriter(writer)));
	}
}