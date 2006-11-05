package net.sf.cpsolver.ifs.util;

import java.io.*;
import java.util.*;

/** A class for reading prolog files.
 *
 * @version
 * IFS 1.0 (Iterative Forward Search)<br>
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
public class PrologFile implements Iterator {
    private java.io.BufferedReader iBufferedReader = null;
    private Term iNextTerm = null;
    
    public PrologFile(String file) throws java.io.IOException {
        iBufferedReader = new BufferedReader(new FileReader(file));
        iNextTerm = (iBufferedReader.ready()?readTerm(new SpecialReader(iBufferedReader)):null);
        if (iNextTerm==null) iBufferedReader.close(); else iBufferedReader.readLine();
    }
    
    /** Reads a prolog file. It returns a set of terms */
    public static Vector readTermsFromStream(java.io.InputStream is, String term) throws java.io.IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Vector ret = new Vector();
        //int x=0;
        while (br.ready()) {
            Term t = readTerm(new SpecialReader(br));
            //System.out.println(t);
            //x++;
            //if (x>10) break;
            if (t!=null && t.getText()!=null && t.getText().startsWith(term)) {
                ret.addElement(t);
            }
            br.readLine();
        }
        br.close();
        return ret;
    }
    
    /** Writes a set of terms. */
    public static void writeTerms(PrintWriter pw, Vector terms) throws java.io.IOException {
        for (Enumeration e=terms.elements();e.hasMoreElements();) {
            Term t = (Term)e.nextElement();
            writeTerm(pw,t);
        }
    }
    
    /** reads a term */
    private static Term readTerm( SpecialReader is) throws IOException {
        StringBuffer text=new StringBuffer();
        Vector content = null;
        int i;
        if ((i = is.read())>=0) {
            while ((char)i=='%' || (char)i==':') {
                do {
                    i=is.read();
                } while (i>=0 && !( i==0x0d || i==0x0a ));
                i=is.read();
                if (i>=0 && (i==0x0d || i==0x0a )) i=is.read();
            }
            if (i>=0) is.flush((char)i);
        }
        char prev = (char)i;
        if (i>=0) while ((i = is.read())>=0) {
            char ch = (char)i;
            if (ch=='\n' || ch=='\r')
                if (prev=='.') break; else continue;
            if (ch=='(' || ch=='[') {
                content = new Vector();
                content.addElement(readTerm(is));
            } else if (content==null && (ch==',' || ch==')' || ch==']')) {
                is.flush(ch);
                break;
            } else if (ch==',') content.addElement(readTerm(is));
            else if (ch==')' || ch==']') break;
            else text.append(ch);
            prev = ch;
        } else return null;
        Term ret =  new Term(text.toString().trim(),content);
        return ret;
    }
    
    /** writes a term */
    private static void writeTerm( PrintWriter pw, Term t) {
        pw.println(t.toString()+".");
    }
    
    
    public boolean hasNext() {
        return iNextTerm!=null;
    }
    
    public Object next() {
        Term ret = iNextTerm;
        try {
            iNextTerm = (iBufferedReader.ready()?readTerm(new SpecialReader(iBufferedReader)):null);
        } catch (java.io.IOException x) {
            iNextTerm = null;
        }
        try {
            if (iNextTerm==null) iBufferedReader.close(); else iBufferedReader.readLine();
        } catch (java.io.IOException x) {}
        return ret;
    }
    
    public void remove() {
    }
    
    /** Flushable reader -- extension of java.io.Reader */
    private static class SpecialReader {
        /** reader */
        private Reader iReader = null;
        /** flushed characters */
        private StringBuffer iFlushedChars = new StringBuffer();
        
        /** constructor */
        public SpecialReader(Reader r) {
            iReader = r;
        }
        
        /** reads a byte */
        public int read() throws java.io.IOException {
            if (iFlushedChars.length()==0) return iReader.read();
            char ret=iFlushedChars.charAt(0);
            iFlushedChars.deleteCharAt(0);
            return ret;
        }
        
        /** flush (return to stream) a character */
        public void flush(char ch) {
            iFlushedChars.insert(0,ch);
        }
    }
    
    /** Term -- it can contain a text and a content (set of terms) */
    public static class Term {
        /** text */
        private String iText = null;
        /** content */
        private Vector iContent = null;
        
        public boolean equals(Object o) {
            if (o==null || !(o instanceof Term)) return false;
            Term t = (Term)o;
            if (iText==null && t.iText!=null) return false;
            if (iText!=null && t.iText==null) return false;
            if (iText!=null && !iText.equals(t.iText)) return false;
            if (iContent==null && t.iContent!=null) return false;
            if (iContent!=null && t.iContent==null) return false;
            if (iContent!=null && !iContent.equals(t.iContent)) return false;
            return true;
        }
        
        /** constructor */
        public Term(String text) {
            iText = text;
            iContent = null;
        }
        
        /** constructor */
        public Term(Vector content) {
            iText = null;
            iContent = content;
        }
        
        /** constructor */
        public Term(String text, Vector content) {
            iText = text;
            iContent = content;
        }
        
        /** constructor */
        public Term(String text, Term[] content) {
            iText = text;
            if (content==null) {
                iContent = null;
            } else {
                iContent = new Vector();
                for (int i=0;i<content.length;i++)
                    iContent.addElement(content[i]);
            }
        }
        
        /** constructor */
        public Term(Term[] content) {
            this(null,content);
        }
        
        /** return text */
        public String getText() {
            return iText;
        }
        
        /** return content */
        public Vector getContent() {
            return iContent;
        }
        
        /** content size */
        public int size() {
            return (iContent==null?-1:iContent.size());
        }
        
        /** return text as int */
        public int toInt() {
            return Integer.parseInt(iText);
        }
        
        /** return text as long */
        public long toLong() {
            return Long.parseLong(iText);
        }
        
        /** return text as fouble */
        public double toDouble() {
            return Double.parseDouble(iText);
        }
        
        /** return text as boolean */
        public boolean toBoolean() {
            return (toInt()!=0);
        }
        
        /** return content as boolean array */
        public boolean[] toBooleanArray() {
            if (iContent.size()==1 && iContent.elementAt(0).toString().length()==0) return new boolean[] {};
            boolean[] ret = new boolean[iContent.size()];
            for (int i=0;i<ret.length;i++) {
                ret[i]=elementAt(i).toBoolean();
            }
            return ret;
        }
        
        /** return content as string array */
        public String[] toStringArray() {
            if (iContent.size()==1 && iContent.elementAt(0).toString().length()==0) return new String[] {};
            String[] ret = new String[iContent.size()];
            for (int i=0;i<ret.length;i++) {
                Term t = elementAt(i);
                ret[i]=(t.getText().length()>0?t.toString():t.elementAt(0).toString());
            }
            return ret;
        }
        
        /** return content as int array */
        public int[] toIntArray() {
            //System.err.println("ToIntArray: "+this);
            if (iContent.size()==1 && iContent.elementAt(0).toString().length()==0) return new int[] {};
            int[] ret = new int[iContent.size()];
            for (int i=0;i<ret.length;i++) {
                Term t = elementAt(i);
                ret[i]=(t.getText().length()>0?Integer.parseInt(t.getText()):t.elementAt(0).toInt());
                //System.err.println("  "+i+" .. "+ret[i]);
            }
            return ret;
        }
        
        /** idx-th element of content */
        public Term elementAt(int idx) {
            try {
                return (Term)iContent.elementAt(idx);
            } catch (Exception e) {
                return null;
            }
        }
        
        /** element of content named name*/
        public Term element(String name) {
            try {
                for (Enumeration i=iContent.elements();i.hasMoreElements();) {
                    Term t = (Term)i.nextElement();
                    if (t.getText()!=null && t.getText().equals(name)) return t;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        
        /** index of element of content named name*/
        public int indexOf(String name) {
            try {
                int idx=0;
                for (Enumeration i=iContent.elements();i.hasMoreElements();) {
                    Term t = (Term)i.nextElement();
                    if (t.getText()!=null && t.getText().equals(name)) return idx;
                    idx++;
                }
                return -1;
            } catch (Exception e) {
                return -1;
            }
        }
        
        /** string representation of term */
        public String toString() {
            boolean isArray = (iText==null || iText.length()==0);
            StringBuffer sb = new StringBuffer(isArray?"":iText);
            if (iContent!=null) {
                sb.append(isArray?"[":"(");
                for (Enumeration e=iContent.elements();e.hasMoreElements();) {
                    sb.append(e.nextElement().toString());
                    sb.append(e.hasMoreElements()?",":"");
                }
                sb.append(isArray?"]":")");
            }
            return sb.toString();
        }
        
        public Object clone() {
            return new Term(iText==null?null:new String(iText),iContent==null?iContent:(Vector)iContent.clone());
        }
    }
}
