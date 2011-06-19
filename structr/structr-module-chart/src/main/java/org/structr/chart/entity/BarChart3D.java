package org.structr.chart.entity;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

/**
 *
 * @author Christian Morgner
 */
public class BarChart3D extends CategoryChart
{
	@Override
	public CategoryItemRenderer getCategoryItemRenderer()
	{
		return(new BarRenderer3D());
	}

	@Override
	public void configureCategoryAxis(CategoryAxis categoryAxis)
	{
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
		return ("/images/chart_bar.png");
	}
}
