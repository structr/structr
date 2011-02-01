/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author cmorgner
 */
public class PutToCacheCommand extends CacheServiceCommand {

    @Override
    public Object execute(Object... parameters) {
        ConcurrentHashMap cache = (ConcurrentHashMap) arguments.get("cache");
        
        Long key = null;
        Object obj = null;
        if (parameters != null && parameters.length == 2
                && parameters[0] instanceof Long // first parameter is node id (Long)
                && parameters[1] instanceof Object) {
            key = (Long) parameters[0];
            obj = (Object) parameters[1];
        }

        if (cache != null && key != null && obj != null) {
            return cache.put(key, obj);
        }

        return null;
    }


}
