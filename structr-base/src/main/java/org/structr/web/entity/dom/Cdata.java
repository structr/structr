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
package org.structr.web.entity.dom;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.w3c.dom.CDATASection;

import java.net.URI;

/**
 *
 */

public interface Cdata extends Content, CDATASection, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Cdata");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Cdata"));
		type.setExtends(URI.create("#/definitions/Content"));
		type.setCategory("html");

		type.overrideMethod("render", false,
			"arg0.getBuffer().append((\"<!CDATA[\"));\n" +
			"\t\tsuper.render(arg0, arg1);\n" +
			"arg0.getBuffer().append(\"]]>\");\n"
		);
	}}

	/*
	@Override
	public void render(RenderContext renderContext, int depth) throws FrameworkException {

		renderContext.getBuffer().append(("<!CDATA["));

		super.render(renderContext, depth);

		renderContext.getBuffer().append("]]>");
	}
	*/
}
