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

import org.structr.flow.api.DataSource;
import org.structr.flow.api.Store;
import org.structr.module.api.DeployableEntity;

public interface FlowStore extends FlowNode, Store, DataSource, DeployableEntity {

	/*

	public enum Operation {
		store,
		retrieve
	}

	private static final Logger logger 							= LoggerFactory.getLogger(FlowStore.class);

	public static final Property<DataSource> dataSource             = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<Operation> operation               = new EnumProperty<>("operation", Operation.class);
	public static final Property<String> key                        = new StringProperty("key");

	public static final View defaultView 						= new View(FlowAction.class, PropertyView.Public, key, operation, dataSource, dataTarget, isStartNodeOfContainer);
	public static final View uiView      						= new View(FlowAction.class, PropertyView.Ui,     key, operation, dataSource, dataTarget, isStartNodeOfContainer);

	@Override
	public void handleStorage(Context context) throws FlowException {

		Operation op = getProperty(operation);
		String _key = getProperty(key);
		DataSource ds = getProperty(dataSource);

		if(op != null && _key != null ) {

			switch (op) {
				case store:
					if (ds != null) {
						context.putIntoStore(_key, ds.get(context));
					}
					break;
				case retrieve:
					context.setData(getUuid(), context.retrieveFromStore(_key));
					break;
			}

		} else {

			logger.warn("Unable to handle FlowStore{}, missing operation or key.", getUuid());
		}


	}

	@Override
	public Object get(Context context) {

		Operation op = getProperty(operation);

		try {

			if (op != null && op.equals(Operation.retrieve)) {

				this.handleStorage(context);
			}
		} catch (FlowException ex) {
			
			logger.error("Exception in FlowStore get: ", ex);
		}
		return context.getData(getUuid());
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("key", this.getProperty(key));
		result.put("operation", this.getProperty(operation));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}
	*/
}
