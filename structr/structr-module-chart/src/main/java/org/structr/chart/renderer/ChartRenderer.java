package org.structr.chart.renderer;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.structr.chart.entity.Chart;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class ChartRenderer implements NodeRenderer<Chart>
{

	public void renderNode(StructrOutputStream out, Chart currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
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

		BufferedImage image = ChartFactory.createBarChart(currentNode.getName(),
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

	public String getContentType(Chart currentNode)
	{
		return("image/png");
	}
}
