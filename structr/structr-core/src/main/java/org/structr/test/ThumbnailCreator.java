/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.test;

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
