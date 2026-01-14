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
package org.structr.web.traits.operations;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;

public abstract class OpeningTag extends FrameworkMethod<OpeningTag> {

	public abstract void openingTag(final DOMElement node, final AsyncBuffer out, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;
}
