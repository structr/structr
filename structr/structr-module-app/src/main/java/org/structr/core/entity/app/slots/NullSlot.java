/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app.slots;

import org.structr.core.entity.app.Slot;

/**
 *
 * @author Christian Morgner
 */
public class NullSlot extends Slot
{
	private Class type = null;

	public NullSlot(Class type)
	{
		this.type = type;
	}

	@Override
	public Class getParameterType()
	{
		return(type);
	}
}
