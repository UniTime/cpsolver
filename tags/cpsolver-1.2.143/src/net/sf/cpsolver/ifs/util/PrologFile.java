package net.sf.cpsolver.ifs.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class for reading prolog files.
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
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */
public class PrologFile implements Iterator<PrologFile.Term> {
    private BufferedReader iBufferedReader = null;
    private Term iNextTerm = null;

    public PrologFile(String file) throws java.io.IOException {
        iBufferedReader = new BufferedReader(new FileReader(file));
        iNextTerm = (iBufferedReader.ready() ? readTerm(new SpecialReader(iBufferedReader)) : null);
        if (iNextTerm == null)
            iBufferedReader.close();
        else
            iBufferedReader.readLine();
    }

    /** Reads a prolog file. It returns a set of terms */
    public static List<Term> readTermsFromStream(java.io.InputStream is, String term) throws java.io.IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        List<Term> ret = new ArrayList<Term>();
        // int x=0;
        while (br.ready()) {
            Term t = readTerm(new SpecialReader(br));
            // System.out.println(t);
            // x++;
            // if (x>10) break;
            if (t != null && t.getText() != null && t.getText().startsWith(term)) {
                ret.add(t);
            }
            br.readLine();
        }
        br.close();
        return ret;
    }

    /** Writes a set of terms. */
    public static void writeTerms(PrintWriter pw, List<Term> terms) throws java.io.IOException {
        for (Term t : terms) {
            writeTerm(pw, t);
        }
    }

    /** reads a term */
    private static Term readTerm(SpecialReader is) throws IOException {
        StringBuffer text = new StringBuffer();
        List<Term> content = null;
        int i;
        if ((i = is.read()) >= 0) {
            while ((char) i == '%' || (char) i == ':') {
                do {
                    i = is.read();
                } while (i >= 0 && !(i == 0x0d || i == 0x0a));
                i = is.read();
                if (i >= 0 && (i == 0x0d || i == 0x0a))
                    i = is.read();
            }
            if (i >= 0)
                is.flush((char) i);
        }
        char prev = (char) i;
        if (i >= 0)
            while ((i = is.read()) >= 0) {
                char ch = (char) i;
                if (ch == '\n' || ch == '\r')
                    if (prev == '.')
                        break;
                    else
                        continue;
                if (ch == '(' || ch == '[') {
                    content = new ArrayList<Term>();
                    content.add(readTerm(is));
                } else if (content == null && (ch == ',' || ch == ')' || ch == ']')) {
                    is.flush(ch);
                    break;
                } else if (ch == ',')
                    content.add(readTerm(is));
                else if (ch == ')' || ch == ']')
                    break;
                else
                    text.append(ch);
                prev = ch;
            }
        else
            return null;
        Term ret = new Term(text.toString().trim(), content);
        return ret;
    }

    /** writes a term */
    private static void writeTerm(PrintWriter pw, Term t) {
        pw.println(t.toString() + ".");
    }

    @Override
    public boolean hasNext() {
        return iNextTerm != null;
    }

    @Override
    public Term next() {
        Term ret = iNextTerm;
        try {
            iNextTerm = (iBufferedReader.ready() ? readTerm(new SpecialReader(iBufferedReader)) : null);
        } catch (java.io.IOException x) {
            iNextTerm = null;
        }
        try {
            if (iNextTerm == null)
                iBufferedReader.close();
            else
                iBufferedReader.readLine();
        } catch (java.io.IOException x) {
        }
        return ret;
    }

    @Override
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
            if (iFlushedChars.length() == 0)
                return iReader.read();
            char ret = iFlushedChars.charAt(0);
            iFlushedChars.deleteCharAt(0);
            return ret;
        }

        /** flush (return to stream) a character */
        public void flush(char ch) {
            iFlushedChars.insert(0, ch);
        }
    }

    /** Term -- it can contain a text and a content (set of terms) */
    public static class Term {
        /** text */
        private String iText = null;
        /** content */
        private List<Term> iContent = null;

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Term))
                return false;
            Term t = (Term) o;
            if (iText == null && t.iText != null)
                return false;
            if (iText != null && t.iText == null)
                return false;
            if (iText != null && !iText.equals(t.iText))
                return false;
            if (iContent == null && t.iContent != null)
                return false;
            if (iContent != null && t.iContent == null)
                return false;
            if (iContent != null && !iContent.equals(t.iContent))
                return false;
            return true;
        }

        /** constructor */
        public Term(String text) {
            iText = text;
            iContent = null;
        }

        /** constructor */
        public Term(List<Term> content) {
            iText = null;
            iContent = content;
        }

        /** constructor */
        public Term(String text, List<Term> content) {
            iText = text;
            iContent = content;
        }

        /** constructor */
        public Term(String text, Term[] content) {
            iText = text;
            if (content == null) {
                iContent = null;
            } else {
                iContent = new ArrayList<Term>();
                for (int i = 0; i < content.length; i++)
                    iContent.add(content[i]);
            }
        }

        /** constructor */
        public Term(Term[] content) {
            this(null, content);
        }

        /** return text */
        public String getText() {
            return iText;
        }

        /** return content */
        public List<Term> getContent() {
            return iContent;
        }

        /** content size */
        public int size() {
            return (iContent == null ? -1 : iContent.size());
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
            return (toInt() != 0);
        }

        /** return content as boolean array */
        public boolean[] toBooleanArray() {
            if (iContent.size() == 1 && iContent.get(0).toString().length() == 0)
                return new boolean[] {};
            boolean[] ret = new boolean[iContent.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = elementAt(i).toBoolean();
            }
            return ret;
        }

        /** return content as string array */
        public String[] toStringArray() {
            if (iContent.size() == 1 && iContent.get(0).toString().length() == 0)
                return new String[] {};
            String[] ret = new String[iContent.size()];
            for (int i = 0; i < ret.length; i++) {
                Term t = elementAt(i);
                ret[i] = (t.getText().length() > 0 ? t.toString() : t.elementAt(0).toString());
            }
            return ret;
        }

        /** return content as int array */
        public int[] toIntArray() {
            // System.err.println("ToIntArray: "+this);
            if (iContent.size() == 1 && iContent.get(0).toString().length() == 0)
                return new int[] {};
            int[] ret = new int[iContent.size()];
            for (int i = 0; i < ret.length; i++) {
                Term t = elementAt(i);
                ret[i] = (t.getText().length() > 0 ? Integer.parseInt(t.getText()) : t.elementAt(0).toInt());
                // System.err.println("  "+i+" .. "+ret[i]);
            }
            return ret;
        }

        /** idx-th element of content */
        public Term elementAt(int idx) {
            try {
                return iContent.get(idx);
            } catch (Exception e) {
                return null;
            }
        }

        /** element of content named name */
        public Term element(String name) {
            try {
                for (Term t : iContent) {
                    if (t.getText() != null && t.getText().equals(name))
                        return t;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        /** index of element of content named name */
        public int indexOf(String name) {
            try {
                int idx = 0;
                for (Term t : iContent) {
                    if (t.getText() != null && t.getText().equals(name))
                        return idx;
                    idx++;
                }
                return -1;
            } catch (Exception e) {
                return -1;
            }
        }

        /** string representation of term */
        @Override
        public String toString() {
            boolean isArray = (iText == null || iText.length() == 0);
            StringBuffer sb = new StringBuffer(isArray ? "" : iText);
            if (iContent != null) {
                sb.append(isArray ? "[" : "(");
                for (Iterator<Term> e = iContent.iterator(); e.hasNext();) {
                    sb.append(e.next().toString());
                    sb.append(e.hasNext() ? "," : "");
                }
                sb.append(isArray ? "]" : ")");
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public Object clone() {
            return new Term(iText == null ? null : new String(iText), iContent == null ? iContent
                    : new ArrayList<Term>(iContent));
        }
    }
}
