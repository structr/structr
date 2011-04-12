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
	private boolean mandatory = false;
	private Object value = null;

	public abstract Class getParameterType();

	public Slot(boolean mandatory)
	{
		this.mandatory = mandatory;
	}

	public void setMandatory(boolean mandatory)
	{
		this.mandatory = mandatory;
	}

	public boolean isMandatory()
	{
		return(mandatory);
	}

	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return(value);
	}
}
