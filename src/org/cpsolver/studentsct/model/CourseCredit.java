package org.cpsolver.studentsct.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourseCredit {
    private String credit;
    private static Float creditValue;

    public void setCredit(String credit) {
        this.credit = credit;
        if (creditValue == null && credit != null) {
            int split = credit.indexOf('|');
            String abbv = null;
            if (split >= 0) {
                abbv = credit.substring(0, split);
            } else {
                abbv = credit;
            }
            Matcher m = Pattern.compile("(^| )(\\d+\\.?\\d*)([,-]?(\\d+\\.?\\d*))?($| )").matcher(abbv);
            if (m.find())
                creditValue = Float.parseFloat(m.group(2));
        }
    }

    public String getCredit() {
        return credit;
    }

    public static boolean hasCreditValue() {
        return creditValue != null;
    }

    public void setCreditValue(Float creditValue) {
        this.creditValue = creditValue;
    }

    public static Float getCreditValue() {
        return creditValue;
    }
}
