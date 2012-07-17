/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

import org.structr.common.PropertyKey;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;

/**
 * A property group that combines two or more property values
 * into a single string value separated by a given string.
 *
 * @author Christian Morgner
 */
public class CombinedPropertyGroup implements PropertyGroup {

	private PropertyKey[] propertyKeys = null;
	private String separator = null;

	public CombinedPropertyGroup(String separator, PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
		this.separator = separator;
	}

	@Override
	public Object getGroupedProperties(GraphObject source) {

		StringBuilder combinedPropertyValue = new StringBuilder();
		int len = propertyKeys.length;

		for(int i=0; i<len; i++) {
			combinedPropertyValue.append(source.getProperty(propertyKeys[i].name()));
			if(i < len-1) {
				combinedPropertyValue.append(separator);
			}
		}

		return combinedPropertyValue.toString();
	}

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) {
	}
}
