/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

/**
 *
 *
 * @author Christian Morgner
 */
public interface InteractiveNode
{
	public Class getParameterType();
	public String getName();

	/**
	 * Returns the value, or null of no value was entered or an error occurred. Note
	 * that this method must return null for an invalid value.
	 *
	 * @return the parsed value or null of an error occurred
	 */
	public Object getValue();

	public void setMappedName(String mappedName);
	public String getMappedName();

	/**
	 * This method will be called from the node that handles the
	 * request.
	 * 
	 * @param errorValue
	 */
	public void setErrorValue(Object errorValue);
}
