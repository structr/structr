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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Language;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 *
 */
public class OneFunction extends CoreFunction {

	@Override
	public String getName() {
		return "one";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("number, oneValue, otherValue");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndAllElementsNotNull(sources, 3);

		final Integer value = this.parseInt(sources[0], 0);
		if (value == 1) {

			return sources[1];
		}

		return sources[2];
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${one(number, oneValue, otherValue)}. Example: ${one(this.children.size, 'child', 'children')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Checks if a number is equal to 1, returns the oneValue if yes, the otherValue if no.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}
