/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.flow.impl;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.traits.definitions.FlowScriptConditionTraitDefinition;
import org.structr.module.api.DeployableEntity;

public class FlowScriptCondition extends FlowCondition implements DeployableEntity, ThrowingElement {

	public FlowScriptCondition(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final String getScript() {
		return wrappedObject.getProperty(traits.key(FlowScriptConditionTraitDefinition.SCRIPT_PROPERTY));
	}

	public final void setScript(final String script) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowScriptConditionTraitDefinition.SCRIPT_PROPERTY), script);
	}

	public final FlowDataSource getScriptSource() {

		final NodeInterface scriptSource = wrappedObject.getProperty(traits.key(FlowScriptConditionTraitDefinition.SCRIPT_SOURCE_PROPERTY));
		if (scriptSource != null) {

			return scriptSource.as(FlowDataSource.class);
		}

		return null;
	}

	public void setScriptSource(final String scriptSource) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowScriptConditionTraitDefinition.SCRIPT_SOURCE_PROPERTY), scriptSource);
	}
}
