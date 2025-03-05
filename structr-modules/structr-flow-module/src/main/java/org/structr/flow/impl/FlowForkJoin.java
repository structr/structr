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
package org.structr.flow.impl;

import org.structr.flow.api.Action;
import org.structr.flow.api.ThrowingElement;
import org.structr.module.api.DeployableEntity;

public interface FlowForkJoin extends FlowNode, Action, DeployableEntity, ThrowingElement {

	/*

	public static final Property<FlowExceptionHandler> exceptionHandler 	= new EndNode<>("exceptionHandler", FlowExceptionHandlerNodes.class);

	public static final View defaultView 									= new View(FlowAction.class, PropertyView.Public, exceptionHandler, isStartNodeOfContainer);
	public static final View uiView      									= new View(FlowAction.class, PropertyView.Ui, exceptionHandler, isStartNodeOfContainer);

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return getProperty(exceptionHandler);
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public void execute(Context context) throws FlowException {

		try {

			Queue<Future> futures = context.getForkFutures();

			while(futures.size() > 0) {
				//Poll head and invoke get to force the promise to resolve and thus waiting for thread termination
				Future f = futures.poll();
				if (f != null) {
					f.get();
				}
			}

		} catch (ExecutionException | InterruptedException ex) {

			throw new FlowException(ex, this);

		}

	}
	*/
}
