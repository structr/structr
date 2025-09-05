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

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class DisablePreventDuplicateRelationshipsFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS    = "Usage: ${disable_prevent_duplicate_relationships()}";
	public static final String ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS_JS = "Usage: ${Structr.disablePreventDuplicateRelationships()}";

	@Override
	public String getName() {
		return "disable_prevent_duplicate_relationships";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().disablePreventDuplicateRelationships();

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS_JS : ERROR_MESSAGE_DISABLE_PREVENT_DUPLICATE_RELATIONSHIPS);
	}

	@Override
	public String shortDescription() {
		return "Disables prevention of duplicate relationships in many-to-many rels in the Structr Backend for the current transaction - USE AT YOUR OWN RISK!";
	}

	@Override
	public boolean isHidden() {
		return true;
	}
}
