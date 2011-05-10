/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.modules.agent.thumbnail;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import org.structr.core.agent.Agent;
import org.structr.core.agent.ReturnValue;
import org.structr.core.agent.Task;
import org.structr.core.entity.StructrNode;

/**
 *
 * @author cmorgner
 */
public class ThumbnailAgent extends Agent {

    @Override
    public Class getSupportedTaskType() {
        return (ThumbnailTask.class);
    }

    @Override
    public ReturnValue processTask(Task task) {
        if (task instanceof ThumbnailTask) {
            Set<StructrNode> nodes = task.getNodes();

            for (StructrNode node : nodes) {
                // FIXME: create node for thumbnail
                createThumbnail(node);
            }
        }

        return (ReturnValue.Success);
    }

    private void createThumbnail(StructrNode node) {
        // FIXME: use StructrPage.URL_KEY..

        String fileName = (String) node.getNode().getProperty("url");
        String contentType = (String) node.getNode().getProperty("mimeType");

        fileName = fileName.substring("file://".length());

        File inputFile = new File(fileName);
        File outputFile = new File(fileName.concat("_thumb"));

        try {
            // read image
            long start = System.nanoTime();

            BufferedImage source = ImageIO.read(inputFile);

            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();

            int destWidth = 80;
            int destHeight = 80;

            BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = dest.createGraphics();

            // set rendering hints..
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            AffineTransform transform = AffineTransform.getScaleInstance((double) destWidth / sourceWidth, (double) destWidth / sourceHeight);
            g.drawRenderedImage(source, transform);

            ImageIO.write(dest, contentType, outputFile);

            long end = System.nanoTime();
            long time = (end - start) / 1000000;

            System.out.println("ThumbnailAgent: reading, scaling and writing took " + time + " ms");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
