/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

/**
 *
 * @author Christian Morgner
 */
public abstract class Slot
{
	private Object value = null;

	public abstract Class getParameterType();

	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return(value);
	}
}
