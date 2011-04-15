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
	private InteractiveNode source = null;
	private Object value = null;

	public abstract Class getParameterType();

	// ----- builtin methods -----
	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return(value);
	}

	public void setSource(InteractiveNode source)
	{
		this.source = source;
	}

	public InteractiveNode getSource()
	{
		return(source);
	}
}
