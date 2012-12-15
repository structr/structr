/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.property;

/**
* A property that stores and retrieves a Date string in ISO8601 format. This property
* uses a long value internally to provide millisecond precision.
 * 
 * @author Christian Morgner
 */
public class ISO8601DateProperty extends DateProperty {
	
	public ISO8601DateProperty(String name) {
		
		super(name, "yyyy-MM-dd'T'HH:mm:ssZ");
	}
}
