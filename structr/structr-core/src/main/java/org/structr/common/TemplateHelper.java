/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common;

import java.util.Random;

/**
 *
 * @author axel
 */
public class TemplateHelper {

    /**
     * Return a random int between 0 and max
     * 
     * @param max
     * @return
     */
    public static int getRandomInt(final int max) {
        Random rand = new Random();
        rand.setSeed(System.nanoTime());
        return rand.nextInt(max);
    }

}
