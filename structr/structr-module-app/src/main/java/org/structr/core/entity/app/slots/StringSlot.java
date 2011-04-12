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
public class StringSlot extends Slot
{
	public StringSlot(boolean mandatory)
	{
		super(mandatory);
	}

	@Override
	public Class getParameterType()
	{
		return(String.class);
	}
}
