package org.structr.chart.entity;

import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;

/**
 *
 * @author Christian Morgner
 */
public class PieChart extends AbstractPieChart
{
	@Override
	public PiePlot getPiePlot(PieDataset dataSet)
	{
		return(new PiePlot(dataSet));
	}
}
