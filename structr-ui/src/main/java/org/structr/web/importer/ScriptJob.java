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
package org.structr.web.importer;

import java.util.Map;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

/**
 */
public class ScriptJob extends ImportJob {

	private Object script   = null;

	public ScriptJob(final Principal user, final Map<String, Object> configuration, final Object script) {

		super(null, user, configuration);

		this.script  = script;
	}

	@Override
	boolean runInitialChecks() throws FrameworkException {
		return true;
	}

	@Override
	Runnable getRunnable() {

		return () -> {

			try {


				final SecurityContext ctx = SecurityContext.getInstance(user, AccessMode.Backend);
				final long startTime      = System.currentTimeMillis();
				reportBegin();

				System.out.println(script);

				//Scripting.evaluate(new ActionContext(ctx, configuration), null, scriptSource, jobName);

				importFinished(startTime, 1);

			} catch (Exception e) {

				reportException(e);

			} finally {

				jobFinished();
			}

		};
	}

	@Override
	public String getImportType() {
		return "CSV";
	}

	@Override
	public String getImportStatusType() {
		return "FILE_IMPORT_STATUS";
	}

	@Override
	public String getImportExceptionMessageType() {
		return "FILE_IMPORT_EXCEPTION";
	}
}
