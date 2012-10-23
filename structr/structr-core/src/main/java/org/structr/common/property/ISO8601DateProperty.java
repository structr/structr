/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common.property;

import org.structr.common.property.DateProperty;

/**
 * Date converter for the ISO 8601 format.
 * 
 * @author Christian Morgner
 */
public class ISO8601DateProperty extends DateProperty {
	
	public ISO8601DateProperty(String name) {
		
		super(name, "yyyy-MM-dd'T'HH:mm:ssZ");
	}
}
