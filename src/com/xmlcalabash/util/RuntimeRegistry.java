package com.xmlcalabash.util;

import com.xmlcalabash.core.XProcRuntime;

import java.util.HashMap;

/**
 * Created by ndw on 5/7/14.
 */
public class RuntimeRegistry {
    private static RuntimeRegistry instance = null;
    private HashMap<Object,XProcRuntime> registry = null;

    protected RuntimeRegistry() {
        registry = new HashMap<Object,XProcRuntime> ();
    }

    public synchronized static RuntimeRegistry getInstance() {
        if(instance == null) {
            instance = new RuntimeRegistry();
        }
        return instance;
    }

    public synchronized void registerRuntime(Object o, XProcRuntime runtime) {
        registry.put(o, runtime);
    }

    public synchronized void unregisterRuntime(Object o) {
        registry.remove(o);
    }

    public synchronized XProcRuntime getRuntime(Object o) {
        return registry.get(o);
    }
}
