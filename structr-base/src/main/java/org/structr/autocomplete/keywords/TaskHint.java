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
package org.structr.autocomplete.keywords;

import org.structr.autocomplete.PageKeywordHint;

public class TaskHint extends PageKeywordHint {

	@Override
	public String getName() {

		return "task";
	}

	@Override
	public String getShortDescription() {

		return "Refers to the task the enclosing partial acts on.";
	}

	@Override
	public String getLongDescription() {

		return "The `task` keyword returns the TaskInstance this part of the page is about, resolved through the "
			+ "closest VisibilityMapping's bound step: in a task list it is the row's own task, on a process "
			+ "instance page it is the task at that step. Use it for status and assignee (`$.task.status`, "
			+ "`$.task.assignee.name`) and as the target of task actions (`$.task.id`). Returns null outside a "
			+ "process context.";
	}
}
