package org.structr.chart.entity;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.PieDataset;

/**
 *
 * @author Christian Morgner
 */
public class PieChart3D extends AbstractPieChart
{
	@Override
	public PiePlot getPiePlot(PieDataset dataSet)
	{
		return(new PiePlot3D(dataSet));
	}

}
