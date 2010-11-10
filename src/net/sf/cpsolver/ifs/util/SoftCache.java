package net.sf.cpsolver.ifs.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Simple table cache (key, value) using java soft references.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 */

public class SoftCache<K, V> implements Map<K, V> {
    private static Logger sLogger = Logger.getLogger(SoftCache.class);
    private Hashtable<K, Reference<V>> iCache = new Hashtable<K, Reference<V>>();
    private ReferenceQueue<V> iQueue = new ReferenceQueue<V>();

    public SoftCache() {
        new SoftCacheCleanupThread().start();
    }

    public synchronized boolean isEmpty() {
        return iCache.isEmpty();
    }

    public synchronized void clear() {
        iCache.clear();
    }

    public synchronized boolean containsKey(Object key) {
        return iCache.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        for (Iterator<Reference<V>> i = iCache.values().iterator(); i.hasNext();) {
            Reference<V> ref = i.next();
            if (value.equals(ref.get()))
                return true;
        }
        return false;
    }

    public synchronized V get(Object key) {
        Reference<V> ref = iCache.get(key);
        return (ref == null ? null : ref.get());
    }

    public synchronized V remove(Object key) {
        Reference<V> ref = iCache.remove(key);
        return (ref == null ? null : ref.get());
    }

    public V put(K key, V value) {
        return putReference(key, new SoftReference<V>(value, iQueue));
    }

    public Object putSoft(K key, V value) {
        return putReference(key, new SoftReference<V>(value, iQueue));
    }

    public Object putWeak(K key, V value) {
        return putReference(key, new WeakReference<V>(value, iQueue));
    }

    private synchronized V putReference(K key, Reference<V> ref) {
        Reference<V> old = iCache.put(key, ref);
        return (old == null ? null : old.get());
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public synchronized int size() {
        return iCache.size();
    }

    public synchronized Set<K> keySet() {
        return iCache.keySet();
    }

    public synchronized Collection<V> values() {
        List<V> ret = new ArrayList<V>(iCache.size());
        for (Reference<V> ref : iCache.values()) {
            V value = ref.get();
            if (value != null)
                ret.add(value);
        }
        return ret;
    }

    public synchronized Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> ret = new HashSet<Map.Entry<K, V>>(iCache.size());
        for (Map.Entry<K, Reference<V>> entry : iCache.entrySet()) {
            if (entry.getValue().get() != null)
                ret.add(new Entry<K, V>(entry.getKey(), entry.getValue().get()));

        }
        return ret;
    }

    public synchronized void cleanDeallocated() {
        int nrCleaned = 0;
        for (Iterator<Map.Entry<K, Reference<V>>> i = iCache.entrySet().iterator(); i.hasNext();) {
            Map.Entry<K, Reference<V>> entry = i.next();
            if (entry.getValue().get() == null) {
                i.remove();
                nrCleaned++;
            }
        }
        sLogger.debug("cleaned " + nrCleaned + " of " + (iCache.size() + nrCleaned) + " items.");
    }

    private ReferenceQueue<V> getQueue() {
        return iQueue;
    }

    private class SoftCacheCleanupThread extends Thread {
        private SoftCacheCleanupThread() {
            setDaemon(true);
            setName("SoftCacheCleanup");
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ReferenceQueue<V> q = getQueue();
                    if (q == null)
                        break; // soft cache deallocated -- stop the thread
                    if (q.remove(10000) == null)
                        continue; // was there something deallocated?
                    while (q.poll() != null) {
                    } // pull all the deallocated references from the queue
                    cleanDeallocated(); // clean the cache
                }
                sLogger.debug("cache terminated");
            } catch (Exception e) {
                sLogger.error("cleanup thread failed, reason:" + e.getMessage(), e);
            }
        }
    }

    private static class Entry<K, V> implements Map.Entry<K, V> {
        private K iKey = null;
        private V iValue = null;

        private Entry(K key, V value) {
            iKey = key;
            iValue = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Map.Entry<?, ?>))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return (getKey() == null ? e.getKey() == null : getKey().equals(e.getKey()))
                    && (getValue() == null ? e.getValue() == null : getValue().equals(e.getValue()));
        }

        @Override
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
        }

        public K getKey() {
            return iKey;
        }

        public V getValue() {
            return iValue;
        }

        public V setValue(V value) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    public static void test() {
        for (int t = 0; t < 10; t++) {
            SoftCache<Integer, byte[]> x = new SoftCache<Integer, byte[]>();
            for (int i = 0; i < 1000000; i++)
                x.put(new Integer(i), new byte[1024]);
        }
    }
}
