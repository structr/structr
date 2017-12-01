/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.function;

import java.util.Collections;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.web.importer.DataImportManager;
import org.structr.web.importer.ScriptJob;

/**
 *
 */
public class ScheduleFunction extends UiFunction {

	public static final String ERROR_MESSAGE_SCHEDULE    = "Usage: ${schedule(script)}. Example: ${schedule('delete(find('User'))')}";
	public static final String ERROR_MESSAGE_SCHEDULE_JS = "Usage: ${{Structr.schedule(script)}}. Example: ${{Structr.schedule(function() {} )}}";

	@Override
	public String getName() {
		return "schedule()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			final ScriptJob job = new ScriptJob(ctx.getSecurityContext().getCachedUser(), Collections.EMPTY_MAP, sources[0]);

			try {

				DataImportManager.getInstance().addJob(job);

			} catch (FrameworkException ex) {
				logException(ex, ex.getMessage(), null);
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SCHEDULE_JS : ERROR_MESSAGE_SCHEDULE);
	}

	@Override
	public String shortDescription() {
		return "Schedules a script or a function to be executed in a separate thread.";
	}

}
