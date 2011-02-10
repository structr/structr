/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.log;

/**
 * To log Logout events
 * 
 * @author axel
 */
public class Logout extends Activity {

    private final static String ICON_SRC = "/images/door_out.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

}
