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
public class GetFromCacheCommand extends CacheServiceCommand {

    @Override
    public Object execute(Object... parameters) {
        ConcurrentHashMap cache = (ConcurrentHashMap) arguments.get("cache");
        
        Long key = null;
        if (parameters != null && parameters.length == 1 && parameters[0] instanceof Long) {
            key = (Long) parameters[0];
        }

        if (cache != null && key != null) {
            return cache.get(key);
        }

        return null;
    }


}
