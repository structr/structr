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
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.GenericProperty;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.*;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.MixedCSVFileImportJob;
import org.structr.web.traits.definitions.ScratchpadTraitDefinition;

import java.io.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

public class ScratchpadTraitWrapper extends AbstractNodeTraitWrapper implements Scratchpad {

	private static final AtomicLong counter = new AtomicLong();

	public ScratchpadTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Long getScratchpadId() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.SCRATCH_ID_PROPERTY));
	}

	@Override
	public void setScratchpadId(final Long scratchpadId) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ScratchpadTraitDefinition.SCRATCH_ID_PROPERTY), scratchpadId);
	}

	@Override
	public String getLogString() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.LOG_STRING_PROPERTY));
	}

	@Override
	public void setLogString(final String logString) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ScratchpadTraitDefinition.LOG_STRING_PROPERTY), logString);
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.SOURCE_PROPERTY));
	}

	@Override
	public void setSource(final String source) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ScratchpadTraitDefinition.SOURCE_PROPERTY), source);
	}

	@Override
	public String getLanguage() {
		return wrappedObject.getProperty(traits.key(ScratchpadTraitDefinition.LANGUAGE_PROPERTY));
	}

	@Override
	public Long getNextScratchId() throws FrameworkException {
		return counter.incrementAndGet();
	}

	@Override
	public Object run(final ActionContext actionContext) throws FrameworkException {

		// always set logString when run - we do not want user-supplied strings in MDC
		final String logString = getScratchLogString(getScratchpadId());
		setLogString(logString);

		MDC.put(ScratchpadTraitDefinition.MDC_SCRATCHPAD_TAG, logString);

		final String sourceParameter = getSource();
		final String language        = getLanguage();

		final String script = switch (ScratchpadTraitDefinition.Language.valueOf(language)) {
			case js     -> "${{" + sourceParameter + "}}";
			case python -> "${python{\n" + sourceParameter + "}}";
			default     -> "${" + sourceParameter + "}";
		};

		final GraphObjectMap result = new GraphObjectMap();
		result.setProperty(new GenericProperty("scratchResult"), Scripting.evaluate(actionContext, null, script, "scratchpad"));

		return result;
	}

	@Override
	public String getScratchLogString (final long scratchId) {
		return "[scratch_" + scratchId + "]";
	}
}
