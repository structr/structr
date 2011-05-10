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
package org.structr.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Image;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author axel
 */
public class SearchStressTest {

    public static void main(String[] args) {

        try {

            StandaloneTestHelper.prepareStandaloneTest("/opt/structr/t5s/db");

            for (int i = 0; i < 1; i++) {

                long t0 = System.currentTimeMillis();

                List<AbstractNode> searchResult = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(null, null, false, false, Search.andExactType("Image"));
                List<AbstractNode> searchResult2 = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(null, null, false, false, Search.andExactType("VfmLeonardoImage"));

                searchResult.addAll(searchResult2);

                long t1 = System.currentTimeMillis();

                System.out.println("[" + i + "] Found " + searchResult.size() + " images in " + (t1 - t0) + " ms");

                for (AbstractNode n : searchResult) {

                    if (n instanceof Image) {
                        Image image = (Image) n;

                        if (!image.isThumbnail()) {

                            List<Image> thumbnails = image.getThumbnails();
                            System.out.println("Image " + image.getId() + " has " + thumbnails.size() + " thumbnail(s).");

                            image.removeThumbnails();

                            System.out.println("Thumbnails removed from node " + image.getId());
                            
                            image.getScaledImage(100, 100);
                            image.getScaledImage(200, 200);
                            image.getScaledImage(300, 300);

                            System.out.println("Created some thumbnails on node " + image.getId());
                        }
                    }

                }


            }

        } catch (Exception e) {
            Logger.getLogger(SearchStressTest.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        } finally {
            StandaloneTestHelper.finishStandaloneTest();

        }
    }
}
