package org.cpsolver.ifs.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A representation of a boolean query. Besides of AND, OR, and NOT, the query
 * may also contain terms (such as major:M1) that are evaluated by the {@link TermMatcher}. 
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2024 Tomas Muller<br>
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
public class Query implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Term iQuery = null;
    
    public Query(String query) {
            iQuery = parse(query == null ? "" : query.trim());
    }
    
    public Query(Term query) {
            iQuery = query;
    }
    
    public Term getQuery() { return iQuery; }
    
    /**
     * Evaluate if the query using the provided term matcher.
     */
    public boolean match(TermMatcher m) {
            return iQuery.match(m);
    }

    /**
     * Evaluate if the query using the provided term matcher.
     */

    public boolean match(AmbigousTermMatcher m) {
            Boolean ret = iQuery.match(m);
            if (ret == null) return true;
            return ret;
    }
    
    @Override
    public String toString() {
            return iQuery.toString();
    }
    
    public String toString(QueryFormatter f) {
            return iQuery.toString(f);
    }
    
    public boolean hasAttribute(String... attr) {
            for (String a: attr)
                    if (iQuery.hasAttribute(a)) return true;
            return false;
    }
    
    public boolean hasAttribute(Collection<String> attr) {
            for (String a: attr)
                    if (iQuery.hasAttribute(a)) return true;
            return false;
    }
    
    private static List<String> split(String query, String... splits) {
            List<String> ret = new ArrayList<String>();
            int bracket = 0;
            boolean quot = false;
            int last = 0;
            boolean white = false;
            loop: for (int i = 0; i < query.length(); i++) {
                    if (query.charAt(i) == '"') {
                            quot = !quot;
                            white = !quot;
                            continue;
                    }
                    if (!quot && query.charAt(i) == '(') { bracket ++; white = false; continue; }
                    if (!quot && query.charAt(i) == ')') { bracket --; white = true; continue; }
                    if (quot || bracket > 0 || (!white && query.charAt(i) != ' ')) {
                            white = (query.charAt(i) == ' ');
                            continue;
                    }
                    white = (query.charAt(i) == ' ');
                    String q = query.substring(i).toLowerCase();
                    for (String split: splits) {
                            if (split.isEmpty() || q.startsWith(split + " ") || q.startsWith(split + "\"") || q.startsWith(split + "(")) {
                                    String x = query.substring(last, i).trim();
                                    if (split.isEmpty() && x.endsWith(":")) continue;
                                    if (!x.isEmpty()) ret.add(x);
                                    last = i + split.length();
                                    if (!split.isEmpty())
                                            i += split.length() - 1;
                                    continue loop;
                            }
                    }
            }
            String x = query.substring(last).trim();
            if (!x.isEmpty()) ret.add(x);
            return ret;
    }

    private static Term parse(String query) {
            List<String> splits;
            splits = split(query, "and", "&&", "&");
            if (splits.size() > 1) {
                    CompositeTerm t = new AndTerm();
                    for (String q: splits)
                            t.add(parse(q));
                    return t;
            }
            splits = split(query, "or", "||", "|");
            if (splits.size() > 1) {
                    CompositeTerm t = new OrTerm();
                    for (String q: splits)
                            t.add(parse(q));
                    return t;
            }
            splits = split(query, "");
            if (splits.size() > 1) {
                    CompositeTerm and = new AndTerm();
                    boolean not = false;
                    splits: for (String q: splits) {
                            if (q.equalsIgnoreCase("not") || q.equals("!")) { not = true; continue; }
                            if (q.startsWith("!(")) {
                                    q = q.substring(1); not = true;
                            } else if (q.toLowerCase().startsWith("not(")) {
                                    q = q.substring(3); not = true;
                            }
                            if (not) {
                                    and.add(new NotTerm(parse(q)));
                                    not = false;
                            } else {
                                    Term t = parse(q);
                                    if (t instanceof AtomTerm) {
                                            AtomTerm a = (AtomTerm)t;
                                            for (Term x: and.terms()) {
                                                    if (x instanceof AtomTerm && ((AtomTerm)x).sameAttribute(a)) {
                                                            and.remove(x);
                                                            OrTerm or = new OrTerm();
                                                            or.add(x); or.add(a);
                                                            and.add(or);
                                                            continue splits;
                                                    } else if (x instanceof OrTerm && ((OrTerm)x).terms().get(0) instanceof AtomTerm && ((AtomTerm)((OrTerm)x).terms().get(0)).sameAttribute(a)) {
                                                            ((OrTerm)x).terms().add(a);
                                                            continue splits;
                                                    }
                                            }
                                    }
                                    and.add(t);
                            }
                    }
                    return and;
            }
            if (query.startsWith("(") && query.endsWith(")")) return parse(query.substring(1, query.length() - 1).trim());
            if (query.startsWith("\"") && query.endsWith("\"") && query.length() >= 2) return new AtomTerm(null, query.substring(1, query.length() - 1).trim());
            int idx = query.indexOf(':');
            if (idx >= 0) {
                    return new AtomTerm(query.substring(0, idx).trim().toLowerCase(), query.substring(idx + 1).trim());
            } else {
                    return new AtomTerm(null, query);
            }
    }
    
    /**
     * Representation of a term in the query, that is attribute:value. Or a sub-query.
     */
    public static interface Term extends Serializable {
        /**
         * Does the term matches the provided object
         */
        public boolean match(TermMatcher m);
        /**
         * Print the term/sub-query
         */
        public String toString(QueryFormatter f);
        /**
         * Check if this term (or its sub-terms) contain a given attribute
         */
        public boolean hasAttribute(String attribute);
        /**
         * Does the term matches the provided object, returning null if does not apply (e.g., the object does not have the provided attribute)
         */
        public Boolean match(AmbigousTermMatcher m);
    }

    /**
     * Composition of one or more terms in a query (e.g., A and B and C)
     */
    public static abstract class CompositeTerm implements Term {
        private static final long serialVersionUID = 1L;
        private List<Term> iTerms = new ArrayList<Term>();

        public CompositeTerm() {}
        
        public CompositeTerm(Term... terms) {
            for (Term t: terms) add(t);
        }
        
        public CompositeTerm(Collection<Term> terms) {
            for (Term t: terms) add(t);
        }
        
        /**
         * Add a term
         */
        public void add(Term t) { iTerms.add(t); }
        
        /**
         * Remove a term
         */
        public void remove(Term t) { iTerms.remove(t); }
        
        /** List terms */
        protected List<Term> terms() { return iTerms; }
        
        /** Operation over the terms, such as AND or OR */
        public abstract String getOp();
        
        @Override
        public boolean hasAttribute(String attribute) {
            for (Term t: terms())
                if (t.hasAttribute(attribute)) return true;
            return false;
        }
        
        @Override
        public String toString() {
            String ret = "";
            for (Term t: terms()) {
                if (!ret.isEmpty()) ret += " " + getOp() + " ";
                ret += t;
            }
            return (terms().size() > 1 ? "(" + ret + ")" : ret);
        }
        
        @Override
        public String toString(QueryFormatter f) {
            String ret = "";
            for (Term t: terms()) {
                if (!ret.isEmpty()) ret += " " + getOp() + " ";
                ret += t.toString(f);
            }
            return (terms().size() > 1 ? "(" + ret + ")" : ret);
        }
    }
    
    /**
     * Representation of an OR between two or more terms.
     */    
    public static class OrTerm extends CompositeTerm {
        private static final long serialVersionUID = 1L;
        public OrTerm() { super(); }
        public OrTerm(Term... terms) { super(terms); }
        public OrTerm(Collection<Term> terms) { super(terms); }
        
        @Override
        public String getOp() { return "OR"; }
        
        /** A term matches, when at least one sub-term matches */
        @Override
        public boolean match(TermMatcher m) {
            if (terms().isEmpty()) return true;
            for (Term t: terms())
                if (t.match(m)) return true;
            return false;
        }
        
        @Override
        public Boolean match(AmbigousTermMatcher m) {
            if (terms().isEmpty()) return true;
            for (Term t: terms()) {
                Boolean r = t.match(m);
                if (r == null) return null;
                if (r) return true;
            }
            return false;
        }
    }
    
    /**
     * Representation of an AND between two or more terms.
     */    
    public static class AndTerm extends CompositeTerm {
        private static final long serialVersionUID = 1L;
        public AndTerm() { super(); }
        public AndTerm(Term... terms) { super(terms); }
        public AndTerm(Collection<Term> terms) { super(terms); }
        
        @Override
        public String getOp() { return "AND"; }
        
        /** A term matches, when all sub-term match */
        @Override
        public boolean match(TermMatcher m) {
            for (Term t: terms())
                if (!t.match(m)) return false;
            return true;
        }
        
        @Override
        public Boolean match(AmbigousTermMatcher m) {
            for (Term t: terms()) {
                Boolean r = t.match(m);
                if (r == null) return null;
                if (!r) return false;
            }
            return true;
        }
    }
    
    /**
     * Representation of a NOT term
     */
    public static class NotTerm implements Term {
        private static final long serialVersionUID = 1L;
        private Term iTerm;
        
        public NotTerm(Term t) {
                iTerm = t;
        }
        
        /** A term matches, when sub-term does not match */
        @Override
        public boolean match(TermMatcher m) {
            return !iTerm.match(m);
        }
        
        @Override
        public boolean hasAttribute(String attribute) {
            return iTerm.hasAttribute(attribute);
        }
        
        @Override
        public Boolean match(AmbigousTermMatcher m) {
            Boolean r = iTerm.match(m);
            if (r == null) return r;
            return !r;
        }
        
        @Override
        public String toString() { return "NOT " + iTerm.toString(); }
        
        @Override
        public String toString(QueryFormatter f) { return "NOT " + iTerm.toString(f); }
    }

    /**
     * Representation of one attribute:value term
     */
    public static class AtomTerm implements Term {
        private static final long serialVersionUID = 1L;
        private String iAttr, iBody;
        
        public AtomTerm(String attr, String body) {
            if (body.startsWith("\"") && body.endsWith("\"") && body.length()>1)
                body = body.substring(1, body.length() - 1);
            iAttr = attr; iBody = body;
        }
        
        /** A term matches, when {@link TermMatcher#match(String, String)} matches */
        @Override
        public boolean match(TermMatcher m) {
            return m.match(iAttr, iBody);
        }
        
        @Override
        public boolean hasAttribute(String attribute) {
            return attribute != null && attribute.equals(iAttr);
        }
        
        public boolean sameAttribute(AtomTerm t) {
            return t != null && hasAttribute(t.iAttr);
        }
        
        @Override
        public String toString() { return (iAttr == null ? "" : iAttr + ":") + (iBody.indexOf(' ') >= 0 ? "\"" + iBody + "\"" : iBody); }
        
        @Override
        public String toString(QueryFormatter f) { return f.format(iAttr, iBody); }

        @Override
        public Boolean match(AmbigousTermMatcher m) {
            return m.match(iAttr, iBody);
        }
    }
    
    /**
     * Term matcher interface. Check representing an object to be matched with the query.
     *
     */
    public static interface TermMatcher {
        /**
         * Does the object has the given attribute matching the given term? For example, for a student, major:M1
         * would match a student that has major M1 (attribute is major, term is M1). 
         */
        public boolean match(String attr, String term);
    }
    
    /**
     * Ambigous term matcher. Returning yes, no, or does not apply (null).
     */
    public static interface AmbigousTermMatcher {
        /**
         * Does the object has the given attribute matching the given term?
         * Returns null if does not apply.
         **/
        public Boolean match(String attr, String term);
    }
    
    /**
     * Query formatter class
     */
    public static interface QueryFormatter {
            String format(String attr, String term);
    }
    
    public static void main(String[] args) {
            System.out.println(parse("(dept:1124 or dept:1125) and area:bio"));
            System.out.println(parse("a \"b c\" or ddd f \"x:x\" x: s !(band or org) (a)or(b)"));
            System.out.println(parse("! f (a)or(b) d !d not x s"));
            System.out.println(parse(""));
            System.out.println(split("(a \"b c\")  ddd f", ""));
            System.out.println(split("a \"b c\" OR not ddd f", "or"));
            System.out.println(split("a or((\"b c\" or dddor) f) q", "or"));
            System.out.println(parse("false"));
    }
    
    
}