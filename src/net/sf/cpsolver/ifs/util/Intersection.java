package net.sf.cpsolver.ifs.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

public class Intersection<E extends Comparable<E>> implements Enumeration<E>, Iterable<E> {
    Iterator<E> i1, i2 = null;
    Set<E> s2 = null;
    E next = null;
    
    public Intersection(Collection<E> c1, Collection<E> c2) {
        setup(c1, c2);
    }
    
    private void setup(Collection<E> c1, Collection<E> c2) {
        if (c1.size() < c2.size()) {
            setup(c2, c1);
        } else {
            if (c2 instanceof HashSet<?>) {
                s2 = (Set<E>)c2;
                i1 = c1.iterator();
            } else {
                if (c1 instanceof TreeSet<?>)
                    i1 = c1.iterator();
                else
                    i1 = new TreeSet<E>(c1).iterator();
                if (c2 instanceof TreeSet<?>)
                    i2 = c2.iterator();
                else
                    i2 = new TreeSet<E>(c2).iterator();
            }
            next();
        }
    }
    
    private void next() {
        next = null;
        if (s2 != null) {
            while (i1.hasNext()) {
                next = i1.next();
                if (s2.contains(next)) return;
            }
            next = null;
        } else {
            try {
                E e1 = i1.next(), e2 = i2.next();
                for(;;) {
                    int cmp = e1.compareTo(e2);
                    if (cmp < 0) { 
                        e1 = i1.next();
                    } else if (cmp > 0) {
                        e2 = i2.next();
                    } else {
                        next = e1; break;
                    }
                }
            } catch (NoSuchElementException e) {}
        }
    }

    @Override
    public boolean hasMoreElements() {
        return next != null;
    }

    @Override
    public E nextElement() {
        E ret = next;
        next();
        return ret;
    }
    
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return hasMoreElements();
            }

            @Override
            public E next() {
                return nextElement();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    
    public static void main(String[] args) {
        Collection<Long> c1 = new ArrayList<Long>();
        Collection<Long> c2 = new TreeSet<Long>();
        for (int i = 1; i < 100000; i++) {
            c1.add((long)ToolBox.random(10000000));
            c2.add((long)ToolBox.random(10000000));
        }
        DecimalFormat df = new DecimalFormat("000");
        long t0 = System.currentTimeMillis();
        Intersection<Long> i = new Intersection<Long>(c1, c2);
        List<Long> c5 = Collections.list(i);
        long t1 = System.currentTimeMillis();
        System.out.println("Intersection [" + df.format(t1 - t0) + " ms]: " + c5);
        long t2 = System.currentTimeMillis();
        Set<Long> c3 = new HashSet<Long>(c2); c3.retainAll(new HashSet<Long>(c1));
        long t3 = System.currentTimeMillis();
        System.out.println("Retain all   [" + df.format(t3 - t2) + " ms]: " + new TreeSet<Long>(c3));
        
        long t4 = System.currentTimeMillis();
        List<Long> c4 = new ArrayList<Long>();
        for (Long x: c1)
            if (c2.contains(x)) c4.add(x);
        long t5 = System.currentTimeMillis();
        System.out.println("Iteration    [" + df.format(t5 - t4) + " ms]: " + new TreeSet<Long>(c4));
    }

}
