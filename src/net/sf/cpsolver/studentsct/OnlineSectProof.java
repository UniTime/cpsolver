package net.sf.cpsolver.studentsct;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.cpsolver.ifs.util.JProf;

/**
 * A test class to demonstrate and compare different online sectioning
 * approaches. It assumes only a single course with just one instructional type
 * (subpart) and that we know the correct expected demand in advance. <br>
 * <br>
 * With the given configuration (e.g., two sections of size 5), it tries all
 * combinations of students how they can be enrolled and compute average and
 * worst case scenarios. <br>
 * <br>
 * Execution:<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;java -cp cpsolver-all-1.1.jar
 * net.sf.cpsolver.studentsct.OnlineSectProof n1 n2 n3 ...<br>
 * where n1, n2, etc. are the sizes of particular sections, e.g., 10 10 for two
 * sections of size 10. <br>
 * <br>
 * 
 * @version StudentSct 1.2 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2010 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          Lazenska 391, 76314 Zlin, Czech Republic<br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 2.1 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not, write to the Free Software
 *          Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *          02110-1301 USA
 */
public class OnlineSectProof {
    private static DecimalFormat sDF = new DecimalFormat("0.000");

    /** A representation of a long number of given base. */
    public static class Sequence {
        private int iBase;
        private int[] iSequence;
        private int[] iCnt;

        /**
         * Constructor
         * 
         * @param length
         *            size of the vector
         * @param base
         *            base (e.g., 2 for a binary vector)
         */
        public Sequence(int length, int base) {
            iSequence = new int[length];
            for (int i = 0; i < iSequence.length; i++)
                iSequence[i] = 0;
            iCnt = new int[base];
            for (int i = 0; i < iCnt.length; i++)
                iCnt[i] = 0;
            iCnt[0] = length;
            iBase = base;
        }

        /**
         * Increment vector by 1, returns false it flips from the highest
         * possible number to zero
         */
        public boolean inc() {
            return inc(0);
        }

        /** Base of the sequence */
        public int base() {
            return iBase;
        }

        private boolean inc(int pos) {
            if (pos >= iSequence.length)
                return false;
            iCnt[iSequence[pos]]--;
            iSequence[pos] = (iSequence[pos] + 1) % iBase;
            iCnt[iSequence[pos]]++;
            if (iSequence[pos] == 0)
                return inc(pos + 1);
            return true;
        }

        /** Count number of occurrences of given number in the sequence */
        public int count(int i) {
            return iCnt[i];
        }

        /** Size of the sequence */
        public int size() {
            return iSequence.length;
        }

        /**
         * Return number on the given position, zero is the number of the least
         * significant value, size()-1 is the highest one
         */
        public int seq(int i) {
            return iSequence[i];
        }

        /**
         * Set the sequence from a string representation (A..0, B..1, C..2,
         * etc.)
         */
        public void set(String seq) {
            for (int i = 0; i < iCnt.length; i++)
                iCnt[i] = 0;
            for (int i = 0; i < iSequence.length; i++) {
                iSequence[i] = (seq.charAt(i) - 'A');
                iCnt[iSequence[i]]++;
            }
        }

        /**
         * String representation (A..0, B..1, C..2, etc.) going from the least
         * significant value to the highest
         */
        @Override
        public String toString() {
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < iSequence.length; i++)
                s.append((char) ('A' + iSequence[i]));
            return s.toString();
        }

        /**
         * If a sequence of all zeros is considered as 0, and the highest
         * possible sequence (sequence of all base-1) is 1, this returns the
         * position of the current sequence between these two bounds.
         */
        public double progress() {
            double ret = 0.0;
            double mx = 1.0;
            for (int i = size() - 1; i >= 0; i--) {
                ret += mx * (iSequence[i]) / iBase;
                mx *= 1.0 / iBase;
            }
            return ret;
        }

        /**
         * Category of a sequence, i.e., a string representation of the count of
         * each number in the sequence. E.g., A5B3C1 means that there are 5
         * zeros, 3 ones, and 1 two int the sequence.
         */
        public String cat() {
            StringBuffer s = new StringBuffer();
            for (int i = 0; i < iBase; i++)
                if (iCnt[i] > 0) {
                    s.append((char) ('A' + i));
                    s.append(iCnt[i]);
                }
            return s.toString();
        }
    }

    /**
     * Extension of {@link OnlineSectProof.Sequence} that represents an ordered
     * set of students as they are to be enrolled into a course (given set of
     * sections).
     */
    public static class StudentSequence extends Sequence {
        private int[] iColumns;
        private int[] iMaxCnt;

        /**
         * Constructor
         * 
         * @param columns
         *            limits of sections of a course (e.g., new int[] {5, 10}
         *            for two sections, first of the size 5, second of the size
         *            of 10)
         */
        public StudentSequence(int[] columns) {
            super(length(columns), base(columns.length));
            iColumns = columns;
            iMaxCnt = new int[base()];
            for (int i = 0; i < iMaxCnt.length; i++)
                iMaxCnt[i] = maxCnt(i);
        }

        /*
         * Number of columns, i.e., number of sections in a course.
         */
        public int nrColumns() {
            return iColumns.length;
        }

        /**
         * Limit of a column (section of a course).
         */
        public int limit(int col) {
            return iColumns[col];
        }

        /**
         * Check that the underlying sequence is a valid sequence of students.
         * I.e., that each student can be enrolled into a section.
         * 
         * @return true, if valid
         */
        public boolean check() {
            for (int i = 0; i < base(); i++)
                if (maxCnt(i) < count(i))
                    return false;
            for (int c = 0; c < nrColumns(); c++) {
                int allowed = 0;
                for (int i = 0; i < size() && allowed < limit(c); i++)
                    if (allow(seq(i), c))
                        allowed++;
                if (allowed < limit(c))
                    return false;
            }
            return true;
        }

        private static int length(int columns[]) {
            int len = 0;
            for (int i = 0; i < columns.length; i++)
                len += columns[i];
            return len;
        }

        private static int base(int nrColumns) {
            return (1 << nrColumns) - 1;
        }

        /**
         * Check whether it is possible to allow student of given type into the
         * given section. Student type can be seen as a binary string that has 1
         * for each section into which a student can be enrolled. I.e., student
         * of type 0 can be enrolled into the fist section, type 2 into the
         * second section, type 3 into both first and section section, type 4
         * into the third section, type 5 into the first and third section etc.
         */
        public boolean allow(int x, int col) {
            return ((x + 1) & (1 << col)) != 0;
        }

        /**
         * Number of sections into which a student of a given type can be
         * enrolled (see {@link OnlineSectProof.StudentSequence#allow(int, int)}
         * ).
         */
        public int nrAllow(int x) {
            int ret = 0;
            for (int i = 0; i < nrColumns(); i++)
                if (allow(x, i))
                    ret++;
            return ret;
        }

        /**
         * Maximum number of student of the given type that can be enrolled into
         * the provided sections (i.e., sum of limits of sections that are
         * allowed fot the student of the given type, see
         * {@link OnlineSectProof.StudentSequence#allow(int, int)}).
         */
        public int maxCnt(int x) {
            int ret = 0;
            for (int i = 0; i < nrColumns(); i++)
                if (allow(x, i))
                    ret += limit(i);
            return ret;
        }
    }

    /** Implemented online algorithms (heuristics) */
    public static String sOnlineAlgs[] = new String[] { "Max(Available)", "Min(Used/Limit)", "Min(Expected-Available)",
            "Min(Expected/Available)", "Min((Expected-Available)/Limit)", "Min(Expected/(Available*Limit))" };

    /**
     * Return true, if the given heuristics should be skipped (not evaluated).
     * 
     * @param computeExpectations
     *            true, if expected demand should be computed in advance
     * @param alg
     *            online algorithm (see {@link OnlineSectProof#sOnlineAlgs})
     * @param allTheSame
     *            true, if all the sections are of the same size
     * @return true if the given heuristics does not need to be computed (e.g.,
     *         it is the same of some other)
     */
    public static boolean skip(boolean computeExpectations, int alg, boolean allTheSame) {
        switch (alg) {
            case 0:
                return !computeExpectations;
            case 1:
                return !computeExpectations;
        }
        return false;
    }

    /**
     * Implementation of the sectioning algorithms.
     * 
     * @param limit
     *            section limit
     * @param used
     *            number of space already used
     * @param expected
     *            expected demand for the given section
     * @param alg
     *            online algorithm (see {@link OnlineSectProof#sOnlineAlgs})
     * @return value that is to be minimized (i.e., a section with the lowest
     *         number will be picked for the student)
     */
    public static double onlineObjective(double limit, double used, double expected, int alg) {
        double available = limit - used;
        switch (alg) {
            case 0:
                return -available;
            case 1:
                return used / limit;
            case 2:
                return expected - available;
            case 3:
                return expected / available;
            case 4:
                return (expected - available) / limit;
            case 5:
                return expected / (available * limit);
        }
        return 0.0;
    }

    /**
     * Section given sequence of students into the course and return the number
     * of students that cannot be sectioned.
     * 
     * @param sq
     *            sequence of studends
     * @param computeExpectations
     *            true, if expected demand for each section should be computed
     *            in advance (otherwise, it is initially set to zero)
     * @param alg
     *            online algorithm (see {@link OnlineSectProof#sOnlineAlgs})
     * @param debug
     *            if true, some debug messages are printed
     * @return number of students that will not be sectioned in such a case
     */
    public static int checkOnline(StudentSequence sq, boolean computeExpectations, int alg, boolean debug) {
        int used[] = new int[sq.nrColumns()];
        double exp[] = new double[sq.nrColumns()];
        for (int i = 0; i < sq.nrColumns(); i++) {
            used[i] = 0;
            exp[i] = 0.0;
        }
        if (computeExpectations) {
            for (int i = 0; i < sq.size(); i++) {
                int x = sq.seq(i);
                double ex = 1.0 / sq.nrAllow(x);
                for (int c = 0; c < sq.nrColumns(); c++) {
                    if (!sq.allow(x, c))
                        continue;
                    exp[c] += ex;
                }
            }
        }
        if (debug) {
            StringBuffer sbExp = new StringBuffer();
            StringBuffer sbUse = new StringBuffer();
            for (int c = 0; c < sq.nrColumns(); c++) {
                if (c > 0) {
                    sbExp.append(",");
                    sbUse.append(",");
                }
                sbExp.append(sDF.format(exp[c]));
                sbUse.append(used[c]);
            }
            System.out.println("      -- initial USE:[" + sbUse + "], EXP:[" + sbExp + "], SQ:" + sq.toString()
                    + ", ALG:" + sOnlineAlgs[alg]);
        }
        int ret = 0;
        for (int i = 0; i < sq.size(); i++) {
            int x = sq.seq(i);
            int bestCol = -1;
            double bestObj = 0.0;
            for (int c = 0; c < sq.nrColumns(); c++) {
                if (!sq.allow(x, c))
                    continue;
                if (used[c] >= sq.limit(c))
                    continue;
                double obj = onlineObjective(sq.limit(c), used[c], exp[c], alg);
                if (debug)
                    System.out.println("      -- test " + ((char) ('A' + x)) + " --> " + (c + 1) + " (OBJ="
                            + sDF.format(obj) + ")");
                if (bestCol < 0 || bestObj > obj) {
                    bestCol = c;
                    bestObj = obj;
                }
            }
            if (bestCol >= 0) {
                used[bestCol]++;
                double ex = 1.0 / sq.nrAllow(x);
                for (int c = 0; c < sq.nrColumns(); c++) {
                    if (!sq.allow(x, c))
                        continue;
                    exp[c] -= ex;
                }
                if (debug) {
                    StringBuffer sbExp = new StringBuffer();
                    StringBuffer sbUse = new StringBuffer();
                    for (int c = 0; c < sq.nrColumns(); c++) {
                        if (c > 0) {
                            sbExp.append(",");
                            sbUse.append(",");
                        }
                        sbExp.append(sDF.format(exp[c]));
                        sbUse.append(used[c]);
                    }
                    System.out.println("    " + ((char) ('A' + x)) + " --> " + (bestCol + 1) + " (OBJ="
                            + sDF.format(bestObj) + ", USE:[" + sbUse + "], EXP:[" + sbExp + "])");
                }
            } else {
                if (debug)
                    System.out.println("    " + ((char) ('A' + x)) + " --> FAIL");
                ret++;
            }
        }
        return ret;
    }

    /** Simple integer counter */
    public static class Counter {
        private int iCnt = 0;

        /** A counter starting from zero */
        public Counter() {
        }

        /** A counter starting from the given number */
        public Counter(int init) {
            iCnt = init;
        }

        /** Increase counter by one */
        public void inc() {
            iCnt++;
        }

        /** Increase counter by the given value */
        public void inc(int val) {
            iCnt += val;
        }

        /** Return counter value */
        public int intValue() {
            return iCnt;
        }
    }

    /** Comparison of two categories */
    public static class CatCmp implements Comparator<String> {
        Hashtable<String, Integer> iWorstCaseCat;
        Hashtable<String, Counter> iTotalCat, iCountCat;

        /**
         * Constructor
         * 
         * @param countCat
         *            table (category, number of sequences of that category)
         * @param totalCat
         *            table (category, total number of students that were not
         *            sectioned of a sequence from this category)
         * @param worstCaseCat
         *            (category, worst number of students that were not
         *            sectioned of a sequence from this category)
         */
        public CatCmp(Hashtable<String, Counter> countCat, Hashtable<String, Counter> totalCat,
                Hashtable<String, Integer> worstCaseCat) {
            iWorstCaseCat = worstCaseCat;
            iTotalCat = totalCat;
            iCountCat = countCat;
        }

        /**
         * Higher number of not-sectioned students in the worst case goes first.
         * If the same, higher average number of not-sectioned students goes
         * first. If the same, compare by category.
         */
        public int compare(String c1, String c2) {
            int wc1 = (iWorstCaseCat.get(c1)).intValue();
            int wc2 = (iWorstCaseCat.get(c2)).intValue();
            int cmp = Double.compare(wc2, wc1);
            if (cmp != 0)
                return cmp;
            int cc1 = (iCountCat.get(c1)).intValue();
            int tc1 = (iTotalCat.get(c1)).intValue();
            int cc2 = (iCountCat.get(c2)).intValue();
            int tc2 = (iTotalCat.get(c2)).intValue();
            cmp = Double.compare(((double) tc2) / cc2, ((double) tc1) / cc1);
            if (cmp != 0)
                return cmp;
            return c1.compareTo(c2);
        }
    }

    /**
     * Test given course (set of sections)
     * 
     * @param args
     *            set of integers -- limits of each sections
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        int[] x = new int[args.length];
        for (int i = 0; i < args.length; i++)
            x[i] = Integer.parseInt(args[i]);
        if (args.length == 0)
            x = new int[] { 5, 5 };
        boolean categories = "true".equals(System.getProperty("cat", "true"));
        boolean allTheSameSize = true;
        String filter = System.getProperty("filter");

        StudentSequence sq = new StudentSequence(x);
        System.out.println("base: " + sq.base());
        System.out.println("columns:");
        int sameSize = -1;
        for (int col = 0; col < x.length; col++) {
            System.out.println("  " + (col + 1) + ". column of limit " + sq.limit(col));
            if (sameSize < 0)
                sameSize = sq.limit(col);
            else if (sameSize != sq.limit(col))
                allTheSameSize = false;
        }
        System.out.println("combinations:");
        for (int i = 0; i < sq.base(); i++) {
            System.out.println("  case " + (char) ('A' + i) + ": ");
            System.out.println("    max: " + sq.maxCnt(i));
            for (int col = 0; col < x.length; col++) {
                if (sq.allow(i, col))
                    System.out.println("      " + (col + 1) + ". column allowed");
            }
        }

        if (System.getProperty("check") != null) {
            sq.set(System.getProperty("check"));
            if (System.getProperty("case") != null) {
                int i = Integer.parseInt(System.getProperty("case")) - 1;
                System.out.println("Online sectioning #" + (i + 1) + " " + sOnlineAlgs[i / 2]
                        + ((i % 2) == 0 ? "" : " w/o precomputed expectations"));
                checkOnline(sq, (i % 2) == 0, i / 2, true);
            } else {
                for (int i = 0; i < 2 * sOnlineAlgs.length; i++) {
                    if (skip((i % 2) == 0, i / 2, allTheSameSize))
                        continue;
                    System.out.println("Online sectioning #" + (i + 1) + " " + sOnlineAlgs[i / 2]
                            + ((i % 2) == 0 ? "" : " w/o precomputed expectations"));
                    checkOnline(sq, (i % 2) == 0, i / 2, true);
                }
            }
            return;
        }

        TreeSet<String>[] worstCaseSq = new TreeSet[2 * sOnlineAlgs.length];
        int[] worstCase = new int[2 * sOnlineAlgs.length];
        int[] total = new int[2 * sOnlineAlgs.length];
        Hashtable<String, String>[] worstCaseSqCat = new Hashtable[2 * sOnlineAlgs.length];
        Hashtable<String, Integer>[] worstCaseCat = new Hashtable[2 * sOnlineAlgs.length];
        Hashtable<String, Counter>[] totalCat = new Hashtable[2 * sOnlineAlgs.length];
        Hashtable<String, Counter>[] countCat = new Hashtable[2 * sOnlineAlgs.length];
        for (int i = 0; i < 2 * sOnlineAlgs.length; i++) {
            total[i] = 0;
            worstCase[i] = -1;
            worstCaseSq[i] = null;
            worstCaseSqCat[i] = new Hashtable<String, String>();
            worstCaseCat[i] = new Hashtable<String, Integer>();
            totalCat[i] = new Hashtable<String, Counter>();
            countCat[i] = new Hashtable<String, Counter>();
        }
        long nrCases = 0;
        System.out.println("N=" + sDF.format(Math.pow(sq.base(), sq.size())));
        long t0 = JProf.currentTimeMillis();
        long mark = t0;
        long nc = 0, hc = 0;
        do {
            // System.out.println(sq+" (cat="+sq.cat()+", check="+sq.check()+")");
            if ((filter == null || filter.equals(sq.cat())) && sq.check()) {
                for (int i = 0; i < 2 * sOnlineAlgs.length; i++) {
                    if (skip((i % 2) == 0, i / 2, allTheSameSize))
                        continue;
                    int onl = checkOnline(sq, (i % 2) == 0, i / 2, false);
                    total[i] += onl;
                    if (worstCaseSq[i] == null || worstCase[i] < onl) {
                        if (worstCaseSq[i] == null)
                            worstCaseSq[i] = new TreeSet<String>();
                        else
                            worstCaseSq[i].clear();
                        worstCaseSq[i].add(sq.toString());
                        worstCase[i] = onl;
                    } else if (worstCase[i] == onl && onl > 0 && worstCaseSq[i].size() < 100) {
                        worstCaseSq[i].add(sq.toString());
                    }
                    if (categories) {
                        String cat = sq.cat();
                        Counter cc = countCat[i].get(cat);
                        if (cc == null) {
                            countCat[i].put(cat, new Counter(1));
                        } else {
                            cc.inc();
                        }
                        if (onl > 0) {
                            Counter tc = totalCat[i].get(cat);
                            if (tc == null) {
                                totalCat[i].put(cat, new Counter(onl));
                            } else {
                                tc.inc(onl);
                            }
                            Integer wc = worstCaseCat[i].get(cat);
                            if (wc == null || wc.intValue() < onl) {
                                worstCaseCat[i].put(cat, new Integer(onl));
                                worstCaseSqCat[i].put(cat, sq.toString());
                            }
                        }
                    }
                }
                nrCases++;
                hc++;
            }
            nc++;
            if ((nc % 1000000) == 0) {
                mark = JProf.currentTimeMillis();
                double progress = sq.progress();
                double exp = ((1.0 - progress) / progress) * (mark - t0);
                System.out.println("  " + sDF.format(100.0 * progress) + "% done in "
                        + sDF.format((mark - t0) / 60000.0) + " min (" + sDF.format(exp / 60000.0) + " min to go, hit "
                        + sDF.format(100.0 * hc / nc) + "%)");
            }
        } while (sq.inc());
        System.out.println("Number of combinations:" + nrCases + " (hit " + sDF.format(100.0 * hc / nc) + "%)");
        for (int i = 0; i < 2 * sOnlineAlgs.length; i++) {
            if (skip((i % 2) == 0, i / 2, allTheSameSize))
                continue;
            System.out.println("Online sectioning #" + (i + 1) + " " + sOnlineAlgs[i / 2]
                    + ((i % 2) == 0 ? "" : " w/o precomputed expectations"));
            System.out.println("  worst case: " + sDF.format((100.0 * worstCase[i]) / sq.size()) + "% (" + worstCase[i]
                    + " of " + sq.size() + ", sequence " + worstCaseSq[i] + ")");
            System.out.println("  average case: " + sDF.format((100.0 * total[i]) / (sq.size() * nrCases)) + "%");
            sq.set(worstCaseSq[i].first());
            checkOnline(sq, (i % 2) == 0, i / 2, true);
            TreeSet<String> cats = new TreeSet<String>(new CatCmp(countCat[i], totalCat[i], worstCaseCat[i]));
            cats.addAll(totalCat[i].keySet());
            for (Iterator<String> j = cats.iterator(); j.hasNext();) {
                String cat = j.next();
                int cc = (countCat[i].get(cat)).intValue();
                int tc = (totalCat[i].get(cat)).intValue();
                int wc = (worstCaseCat[i].get(cat)).intValue();
                String wcsq = worstCaseSqCat[i].get(cat);
                System.out.println("  Category " + cat + " (size=" + cc + ")");
                System.out.println("    worst case: " + sDF.format((100.0 * wc) / sq.size()) + "% (" + wc + " of "
                        + sq.size() + ", sequence " + wcsq + ")");
                System.out.println("    average case: " + sDF.format((100.0 * tc) / (sq.size() * cc)) + "%");
            }
        }
        /*
         * sq.set("EEEBBBAAA");
         * System.out.println(sq+" (cat="+sq.cat()+", check="+sq.check()+")");
         * sq.set("CACACAAABB"); checkOnline(sq, true, 2, true);
         */
    }

}
