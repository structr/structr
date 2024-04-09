/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.flow.engine;

import org.structr.flow.api.Filter;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowHandler;

/**
 *
 */
public class FilterHandler implements FlowHandler<Filter> {

	@Override
	public FlowElement handle(final Context context, final Filter flowElement) throws FlowException {

		flowElement.filter(context);

		return flowElement.next();
	}
}
