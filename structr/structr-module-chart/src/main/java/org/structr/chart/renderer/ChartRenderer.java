package org.structr.chart.renderer;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Christian Morgner
 */
class ChartRenderer
{
	Logger logger = Logger.getLogger(ChartRenderer.class.getName());

	protected void writeImage(OutputStream out, BufferedImage image)
	{
		try
		{
			ImageIO.write(image, "PNG", out);

		} catch(Throwable t)
		{
			logger.log(Level.WARNING, "Error writing image to stream: {0}", t.getMessage());
		}
	}
}
