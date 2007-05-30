package net.sf.cpsolver.ifs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

/** Support for CSV (comma separated) text files.
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

public class CSVFile implements Serializable {
	private static final long serialVersionUID = 1L;
	Hashtable iHeaderMap = null;
	CSVLine iHeader = null;
	Vector iLines = null;
	String iSeparator = ",";
	String iQuotationMark = "\"";
	
	public CSVFile() {}
	
	public CSVFile(File file) throws IOException {
		load(file);
	}
	
	public CSVFile(File file, String separator) throws IOException {
		setSeparator(separator);
		load(file);
	}
	
	public CSVFile(File file, String separator, String quotationMark) throws IOException {
		setSeparator(separator);
		setQuotationMark(quotationMark);
		load(file);
	}
	
	public void setSeparator(String separator) { iSeparator = separator; }
	public String getSeparator() { return iSeparator; }
	public void setQuotationMark(String quotationMark) { iQuotationMark = quotationMark; }
	public String getQuotationMark() { return iQuotationMark; }

	public void load(File file) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			iHeader = new CSVLine(reader.readLine()); //read header
			iHeaderMap = new Hashtable();
			iLines = new Vector();
			int idx = 0;
			for (Enumeration e=iHeader.fields();e.hasMoreElements();idx++) {
				CSVField field = (CSVField)e.nextElement();
				iHeaderMap.put(field.toString(),new Integer(idx));
			}
			String line = null;
			while ((line=reader.readLine())!=null) {
				if (line.trim().length()==0) continue;
				iLines.addElement(new CSVLine(line));
			}
		} finally {
			if (reader!=null) reader.close();
		}
	}
	
	public void save(File file) throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(file));
			if (iHeader!=null) 
				writer.println(iHeader.toString());
		
			if (iLines!=null) {
				for (Enumeration e=iLines.elements();e.hasMoreElements();) {
					Object line = e.nextElement();
					writer.println(line.toString());
				}
			}
		
			writer.flush();
		} finally {
			if (writer!=null) writer.close();
		}
	}
	
	public CSVLine getHeader() { return iHeader; }
	public void setHeader(CSVLine header) { iHeader = header; }
	public Vector getLines() { return iLines; }
	public int size() { return iLines.size(); }
	public boolean isEmpty() { return iLines.isEmpty(); }
	public CSVLine getLine(int idx) { return (CSVLine)iLines.elementAt(idx); }
	public Enumeration lines() { return iLines.elements(); }
	public void addLine(CSVLine line) { 
		if (iLines==null) iLines = new Vector();
		iLines.addElement(line);
	}
	public void addLine(String line) { 
		if (iLines==null) iLines = new Vector();
		iLines.addElement(line);
	}
	public Vector filter(CSVFilter filter) {
		Vector ret = new Vector();
		for (Enumeration e=iLines.elements();e.hasMoreElements();) {
			CSVLine line = (CSVLine)e.nextElement();
			if (filter.match(line))
				ret.addElement(line);
		}
		return ret;
	}
	
	public CSVLine addLine() {
		CSVLine line = new CSVLine(); 
		addLine(line);
		return line;
	}
	public CSVLine addLine(CSVField fields[]) {
		CSVLine line = new CSVLine(fields);
		addLine(line);
		return line;
	}
	public CSVLine addLine(Collection fields) {
		CSVLine line = new CSVLine(fields);
		addLine(line);
		return line;
	}
	public CSVLine setHeader(CSVField fields[]) {
		CSVLine header = new CSVLine(fields);
		setHeader(header);
		return header;
	}
	public CSVLine setHeader(Collection fields) {
		CSVLine header = new CSVLine(fields);
		setHeader(header);
		return header;
	}
	
    /** Representation of a  line of a CSV file */
	public class CSVLine implements Serializable {
		private static final long serialVersionUID = 1L;
		Vector iFields = new Vector(iHeader==null?10:iHeader.size());
		
		public CSVLine(String line) {
			int idx = 0;
			int newIdx = 0;
			int fromIdx = 0; 
			while ((newIdx = line.indexOf(iSeparator, fromIdx))>=0) {
				String field = line.substring(idx, newIdx);
				if (iQuotationMark!=null && field.startsWith(iQuotationMark) && !field.endsWith(iQuotationMark)) {
					fromIdx = newIdx + iSeparator.length();
					continue;
				}
				iFields.addElement(new CSVField(field, iQuotationMark));
				idx = newIdx + iSeparator.length();
				fromIdx = idx;
			}
			iFields.addElement(new CSVField(line.substring(idx), iQuotationMark));
		}
		public CSVLine() {}
		public CSVLine(CSVField fields[]) {
			for (int i=0;i<fields.length;i++)
				iFields.addElement(fields[i]);
		}
		public CSVLine(Collection fields) {
			iFields.addAll(fields);
		}
		
		public Vector getFields() { return iFields; }
		public int size() { return iFields.size(); }
		public boolean isEmpty() { return iFields.isEmpty(); }
		public CSVField getField(int idx) { 
			try {
				return (CSVField)iFields.elementAt(idx);
			} catch (ArrayIndexOutOfBoundsException e) {
				return null;
			}
		}
		public void setField(int idx, CSVField field) {
			iFields.setElementAt(field, idx);
		}
		public Enumeration fields() { return iFields.elements(); }
		public CSVField getField(String name) {
			Integer idx = (Integer)iHeaderMap.get(name);
			return (idx==null?null:getField(idx.intValue()));
		}
		public void setField(String name, CSVField field) {
			Integer idx = (Integer)iHeaderMap.get(name);
			if (idx!=null) setField(idx.intValue(), field);
		}
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (Enumeration e=iFields.elements();e.hasMoreElements();) {
				CSVField field = (CSVField)e.nextElement();
				if (field!=null)
					sb.append((iQuotationMark==null?"":iQuotationMark)+(field==null?"":field.toString())+(iQuotationMark==null?"":iQuotationMark));
				if (e.hasMoreElements()) sb.append(iSeparator);
			}
			return sb.toString();
		}
		public void debug(int offset, PrintWriter out) {
			int idx=0;
			for (Enumeration e=iFields.elements();e.hasMoreElements();idx++) {
				CSVField field = (CSVField)e.nextElement();
				if (field==null || field.toString().length()==0) continue;
				for (int i=0;i<offset;i++)
					out.print(" ");
				out.println(iHeader.getField(idx)+"="+(iQuotationMark==null?"":iQuotationMark)+field+(iQuotationMark==null?"":iQuotationMark));
			}
		}
	}
	
    /** Representation of a field of a CSV file */
	public static class CSVField implements Serializable {
		private static final long serialVersionUID = 1L;
		String iField = null;
		public CSVField(String field, String quotationMark) {
			field = field.trim();
			if (quotationMark!=null && field.startsWith(quotationMark) && field.endsWith(quotationMark)) 
				field = field.substring(1, field.length()-1);
			iField = field.trim();
		}
		public CSVField(Object field) { set(field==null?"":field.toString()); }
		public CSVField(int field) { set(field); }
		public CSVField(boolean field) { set(field); }
		public CSVField(double field) { set(field); }
		public CSVField(long field) { set(field); }
		public CSVField(float field) { set(field); }
		
		public void set(Object value) { iField = (value==null?"":value.toString()); }
		public void set(int value) { iField = String.valueOf(value); }
		public void set(boolean value) { iField = (value?"1":"0"); }
		public void set(double value) { iField = String.valueOf(value); }
		public void set(long value) { iField = String.valueOf(value); }
		public void set(float value) { iField = String.valueOf(value); }
		
		public String toString() { return (iField==null?"":iField); }
		public boolean isEmpty() { return (iField.length()==0); }
		public int toInt() { return toInt(0); }
		public int toInt(int defaultValue) { 
			try {
				return Integer.parseInt(iField);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		public long toLong() { return toLong(0); }
		public long toLong(long defaultValue) { 
			try {
				return Long.parseLong(iField);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		public double toDouble() { return toDouble(0); }
		public double toDouble(double defaultValue) { 
			try {
				return Double.parseDouble(iField);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		public Date toDate() {
			int month = Integer.parseInt(iField.substring(0,2));
			int day = Integer.parseInt(iField.substring(3,5));
			int year = Integer.parseInt(iField.substring(6,8));
			Calendar c = Calendar.getInstance(Locale.US);
			c.set(year,month-1,day,0,0,0);
			return c.getTime();
		}
		public boolean toBoolean() { return "Y".equalsIgnoreCase(iField) || "on".equalsIgnoreCase(iField) || "true".equalsIgnoreCase(iField) || "1".equalsIgnoreCase(iField); }
	}
	
    /** An interface for filtering lines of a CSV file */
	public static interface CSVFilter {
		public boolean match(CSVLine line);
	}
	
	public static CSVFilter eq(String name, String value) {
		return (new CSVFilter() {
			String n,v;
			public boolean match(CSVLine line) {
				return line.getField(n).equals(v);
			}
			private CSVFilter set(String n, String v) { this.n=n; this.v=v; return this; }
		}).set(name, value);
	}
	
	public static CSVFilter and(CSVFilter first, CSVFilter second) {
		return (new CSVFilter() {
			CSVFilter a,b;
			public boolean match(CSVLine line) {
				return a.match(line) && b.match(line);
			}
			private CSVFilter set(CSVFilter a, CSVFilter b) { this.a=a; this.b=b; return this; }
		}).set(first, second);
	}

	public static CSVFilter or(CSVFilter first, CSVFilter second) {
		return (new CSVFilter() {
			CSVFilter a,b;
			public boolean match(CSVLine line) {
				return a.match(line) || b.match(line);
			}
			private CSVFilter set(CSVFilter a, CSVFilter b) { this.a=a; this.b=b; return this; }
		}).set(first, second);
	}

	public static CSVFilter not(CSVFilter filter) {
		return (new CSVFilter() {
			CSVFilter f;
			public boolean match(CSVLine line) {
				return !f.match(line);
			}
			private CSVFilter set(CSVFilter f) { this.f=f; return this; }
		}).set(filter);
	}
}
