package net.sf.cpsolver.ifs.util;

/** Simple table cache (key, value) using java soft references.
 * 
 * @version
 * IFS 1.1 (Iterative Forward Search)<br>
 * Copyright (C) 2006 Tomas Muller<br>
 * <a href="mailto:muller@ktiml.mff.cuni.cz">muller@ktiml.mff.cuni.cz</a><br>
 * Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <br><br>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <br><br>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

public class SoftCache implements Map {
	private static Logger sLogger = Logger.getLogger(SoftCache.class);
	private Hashtable iCache = new Hashtable();
	private ReferenceQueue iQueue = new ReferenceQueue();
	
	public SoftCache() {
		(new SoftCacheCleanupThread(this)).start();
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
		for (Iterator i=iCache.values().iterator();i.hasNext();) {
			Reference ref = (Reference)i.next();
			if (value.equals(ref.get())) return true;
		}
		return false;
	}
	
	public synchronized Object get(Object key) {
		Reference ref = (Reference)iCache.get(key);
		return (ref==null?null:ref.get());
	}
	public synchronized Object remove(Object key) {
		Reference ref = (Reference)iCache.remove(key);
		return (ref==null?null:ref.get());
	}
	public Object put(Object key, Object value) {
		return putReference(key, new SoftReference(value, iQueue));
	}
	public Object putSoft(Object key, Object value) {
		return putReference(key, new SoftReference(value, iQueue));
	}
	public Object putWeak(Object key, Object value) {
		return putReference(key, new WeakReference(value, iQueue));
	}
	private synchronized Object putReference(Object key, Reference ref) {
		Reference old = (Reference)iCache.put(key, ref);
		return (old==null?null:old.get());
	}
	public void putAll(Map map) {
		for (Iterator i=map.entrySet().iterator();i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next();
			put(entry.getKey(),entry.getValue());
		}
	}
	public synchronized int size() {
		return iCache.size();
	}
	public synchronized Set keySet() { 
		return iCache.keySet();
	}
	public synchronized Collection values() {
		Vector ret = new Vector(iCache.size());
		for (Iterator i=iCache.values().iterator();i.hasNext();) {
			Reference ref = (Reference)i.next();
			Object value = ref.get();
			if (value!=null) ret.addElement(value);
		}
		return ret;
	}
	public synchronized Set entrySet() {
		HashSet ret = new HashSet(iCache.size());
		for (Iterator i=iCache.entrySet().iterator();i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next();
			Reference ref = (Reference)entry.getValue();
			Object value = ref.get();
			if (value!=null) ret.add(new Entry(entry.getKey(),value));
			
		}
		return ret;
	}
	public boolean equals(Object o) {
		if (o==null || !(o instanceof Map)) return false;
		return entrySet().equals(((Map)o).entrySet());
	}
	
	public synchronized void cleanDeallocated() {
		int nrCleaned = 0;
		for (Iterator i=iCache.entrySet().iterator();i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next();
			SoftReference ref = (SoftReference)entry.getValue();
			if (ref.get()==null) {
				i.remove();
				nrCleaned++;
			}
		}
		sLogger.debug("cleaned "+nrCleaned+" of "+(iCache.size()+nrCleaned)+" items.");
	}

	private ReferenceQueue getQueue() { return iQueue; }
	
	private static class SoftCacheCleanupThread extends Thread {
		private static Logger sLogger = Logger.getLogger(SoftCacheCleanupThread.class);
		private WeakReference iSoftCache = null;
		private SoftCacheCleanupThread(SoftCache softCache) {
			setDaemon(true);
			setName("SoftCacheCleanup");
			iSoftCache = new WeakReference(softCache);
		}
		private ReferenceQueue getQueue() {
			SoftCache softCache = (SoftCache)iSoftCache.get();
			return (softCache==null?null:softCache.getQueue());
		}
		private void cleanup() {
			SoftCache softCache = (SoftCache)iSoftCache.get();
			if (softCache!=null)
				softCache.cleanDeallocated();
		}
		public void run() {
			try {
				while (true) {
					ReferenceQueue q = getQueue();
					if (q==null) break; //soft cache deallocated -- stop the thread
					if (q.remove(10000)==null) continue; //was there something deallocated?
					while (q.poll()!=null); //pull all the deallocated references from the queue
					cleanup(); //clean the cache
				}
				sLogger.debug("cache terminated");
			} catch (Exception e) {
				sLogger.error("cleanup thread failed, reason:"+e.getMessage(),e);
			}
		}
	}
	
	public static class Entry implements Map.Entry {
		private Object iKey = null;
		private Object iValue = null;
		private Entry(Object key, Object value) {
			iKey = key; iValue = value;
		}
		public boolean equals(Object o) {
			if (o==null || !(o instanceof Map.Entry)) return false;
			Map.Entry e = (Map.Entry)o;
			return (getKey()==null?e.getKey()==null:getKey().equals(e.getKey())) &&
					(getValue()==null?e.getValue()==null:getValue().equals(e.getValue()));
		}
		public int hashCode() {
			return (getKey()==null?0:getKey().hashCode()) ^ (getValue()==null?0:getValue().hashCode());
		}
		public Object getKey() {
			return iKey;
		}
		public Object getValue() {
			return iValue;
		}
		public Object setValue(Object value) throws UnsupportedOperationException{
			throw new UnsupportedOperationException();
		}
	}
	
	public static void test() {
		for (int t=0;t<10;t++) {
			SoftCache x = new SoftCache();
			for (int i=0;i<1000000;i++)
				x.put(new Integer(i),new byte[1024]);
		}
	}
}
