/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class StartNodePropertyGroup implements PropertyGroup {

	private PropertyKey[] keys = null;

	public StartNodePropertyGroup(PropertyKey... keys) {
		this.keys = keys;
	}

	@Override
	public Object getGroupedProperties(GraphObject source) {

		if(source instanceof AbstractRelationship) {

			Map<String, Object> props = new LinkedHashMap<String, Object>();
			AbstractRelationship rel  = (AbstractRelationship)source;
			AbstractNode startNode    = rel.getStartNode();

			for(PropertyKey key : keys) {
				props.put(key.name(), startNode.getProperty(key));
			}

			return props;
		}

		return null;
	}

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) throws FrameworkException {
	}
}
