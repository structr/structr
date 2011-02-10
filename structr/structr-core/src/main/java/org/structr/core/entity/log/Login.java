/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.log;

/**
 * To log Login events
 * 
 * @author axel
 */
public class Login extends Activity {

    private final static String ICON_SRC = "/images/door_in.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

}
