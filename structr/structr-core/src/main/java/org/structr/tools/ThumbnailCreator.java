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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.structr.common.ImageHelper;
import org.structr.common.ImageHelper.Thumbnail;
import org.structr.common.StandaloneTestHelper;
import org.structr.core.Services;
import org.structr.core.entity.Image;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author axel
 */
public class ThumbnailCreator {

    public static void main(String[] args) {


        StandaloneTestHelper.prepareStandaloneTest("/opt/structr/t5s/db");


        // hoch 8451L
        // quer 8617L
        // quadrat 9710L

        try {
            Image image1 = (Image) Services.command(FindNodeCommand.class).execute(new SuperUser(), 8451L);
            Thumbnail tn1 = ImageHelper.createThumbnail(image1, 160, 90, false);
            FileUtils.writeByteArrayToFile(new File("/tmp/8451_scale.jpg"), tn1.getBytes());

            Image image2 = (Image) Services.command(FindNodeCommand.class).execute(new SuperUser(), 8451L);
            Thumbnail tn2 = ImageHelper.createThumbnail(image2, 160, 90, true);
            FileUtils.writeByteArrayToFile(new File("/tmp/8451_crop.jpg"), tn2.getBytes());
            
//            Image image3 = (Image) Services.command(FindNodeCommand.class).execute(new SuperUser(), 9710L);
//            Thumbnail tn3 = ImageHelper.createThumbnail(image3, 100, 100, true);
//            FileUtils.writeByteArrayToFile(new File("/tmp/9710.jpg"), tn3.getBytes());

        } catch (IOException ex) {
            Logger.getLogger(ThumbnailCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        StandaloneTestHelper.finishStandaloneTest();

    }
}
