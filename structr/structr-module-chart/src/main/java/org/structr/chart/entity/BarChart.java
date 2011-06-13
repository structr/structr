package org.structr.chart.entity;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class BarChart extends AbstractNode
{
	@Override
	public void renderDirect(OutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
		String categoryAxisLabel = "";
		String valueAxisLabel = "";
		int width = 600;
		int height = 400;

		dataSet.addValue(1, "One", "Test 1");
		dataSet.addValue(2, "Two", "Test 1");
		dataSet.addValue(3, "Three", "Test 1");
		dataSet.addValue(4, "Four", "Test 1");

		dataSet.addValue(1, "One", "Test 2");
		dataSet.addValue(2, "Two", "Test 2");
		dataSet.addValue(3, "Three", "Test 2");
		dataSet.addValue(4, "Four", "Test 2");

		BufferedImage image = ChartFactory.createBarChart(getName(),
			categoryAxisLabel,
			valueAxisLabel,
			dataSet,
			PlotOrientation.HORIZONTAL,
			true, true, true

		).createBufferedImage(width, height);

		try
		{
			ImageIO.write(image, "PNG", out);

		} catch(Throwable ignore)
		{
		}
	}

	@Override
	public String getContentType()
	{
		return("image/png");
	}

	public String getIconSrc()
	{
		return("/images/chart_bar.png");
	}

	public void onNodeCreation()
	{
		//
	}

	public void onNodeInstantiation()
	{
	}

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId)
	{
		// nothing to do here
	}
}
