package org.structr.chart.entity;

import org.structr.common.PropertyKey;
import org.structr.core.entity.ArbitraryNode;

/**
 *
 * @author Christian Morgner
 */
public abstract class Chart extends ArbitraryNode
{
	public enum Key implements PropertyKey
	{
		width, height;
	}

	public int getWidth()
	{
		return(getIntProperty(Key.width));
	}

	public int getHeight()
	{
		return(getIntProperty(Key.height));
	}

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
