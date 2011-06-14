package org.structr.chart.entity;

import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public abstract class Chart extends AbstractNode
{
	@Override
	public String getContentType()
	{
		return("image/png");
	}

	@Override
	public void onNodeCreation()
	{
		//
	}

	@Override
	public void onNodeInstantiation()
	{
	}
}
