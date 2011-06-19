package org.structr.chart.entity;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

/**
 *
 * @author Christian Morgner
 */
public class AreaChart extends CategoryChart
{
	@Override
	public CategoryItemRenderer getCategoryItemRenderer()
	{
		AreaRenderer ret = new AreaRenderer();

		return(ret);
	}

	@Override
	public void configureCategoryAxis(CategoryAxis categoryAxis)
	{
		categoryAxis.setCategoryMargin(0.0);
	}

	@Override
	public void configureValueAxis(ValueAxis valueAxis)
	{
	}

	@Override
	public void configurePlot(CategoryPlot plot)
	{
	}

	@Override
	public void configureChart(JFreeChart chart)
	{
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/chart_curve.png");
	}
}
