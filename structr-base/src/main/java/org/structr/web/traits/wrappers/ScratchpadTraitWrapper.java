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
package org.structr.web.traits.wrappers;

import org.slf4j.MDC;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.*;
import org.structr.web.traits.definitions.ScratchpadTraitDefinition;

import java.util.Date;

public class ScratchpadTraitWrapper extends AbstractNodeTraitWrapper implements Scratchpad {

	public static final String MDC_SCRATCHPAD_TAG = "structrScratchMDC";

	public ScratchpadTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.SOURCE_PROPERTY));
	}

	@Override
	public String getResult() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.RESULT_PROPERTY));
	}

	@Override
	public String getLog() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.LOG_PROPERTY));
	}

	@Override
	public Long getLastRunTimestamp() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.LAST_RUN_TIMESTAMP_PROPERTY));
	}

	@Override
	public boolean getCollapsed() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.COLLAPSED_PROPERTY));
	}

	@Override
	public void setLastRunTimestamp(final Long timestamp) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ScratchpadTraitDefinition.LAST_RUN_TIMESTAMP_PROPERTY), timestamp);
	}

	@Override
	public Object run(final ActionContext actionContext) throws FrameworkException {

		MDC.put(MDC_SCRATCHPAD_TAG, getScratchpadLogString());

		// wrap in graphobject so we can yield null values (would otherwise return as [])
		final GraphObjectMap result = new GraphObjectMap();
		result.setProperty(new GenericProperty("scratchResult"), Scripting.evaluate(actionContext, null, "${" + getSource().trim() + "}", "scratchpad"));

		return result;
	}

	public String getServerLog() {

		final Long lastRunTimestamp = getLastRunTimestamp();
		if (lastRunTimestamp == null) {
			return "";
		}

		final String logFilter = getScratchpadLogString();

		return ServerLogFunction.getServerLog(100_000, -1, null, logFilter);
	}

	@Override
	public String getScratchpadLogString() {

		final String lastRunDate_base36 = Long.toString(getLastRunTimestamp(), 36);

		return "[scratch_" + lastRunDate_base36 + "]";
	}
}
