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
package org.structr.autocomplete;

import org.structr.common.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.ParseResult;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class JavaHintProvider extends AbstractHintProvider {

	@Override
	protected List<AbstractHint> getAllHints(final ActionContext ionContext, final GraphObject currentNode, final String editorText, final ParseResult parseResult) {
		return new ArrayList<>();
	}

	@Override
	protected String getFunctionName(final String source) {

		if (source.contains("_")) {
			return CaseHelper.toLowerCamelCase(source);
		}

		return source;
	}
}
