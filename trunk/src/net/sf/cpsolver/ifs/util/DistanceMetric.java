package net.sf.cpsolver.ifs.util;

/**
 * Common class for computing distances and back-to-back instructor / student conflicts.
 * 
 * When property Distances.Eclipsoid is set, the distances are computed using the given (e.g., WGS84, see {@link Eclipsoid}).
 * In the legacy mode (when eclipsoid is not set), distances are computed using Euclidian distance and 1 unit is consideted 10 meters.
 * <br><br>
 * For student back-to-back conflicts, Distances.Speed (in meters per minute) is considered and compared with the break time
 * of the earlier class.
 * <br><br>
 * For instructors, the preference is computed using the distance in meters and the three constants 
 * Instructor.NoPreferenceLimit (distance <= limit -> no preference), Instructor.DiscouragedLimit (distance <= limit -> discouraged),
 * Instructor.ProhibitedLimit (distance <= limit -> strongly discouraged), the back-to-back placement is prohibited when the distance is over the last limit.
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2010 Tomas Muller<br>
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
public class DistanceMetric {
    public static enum Eclipsoid {
        LEGACY ("Euclidean metric (1 unit equals to 10 meters)", "X-Coordinate", "Y-Coordinate", 0, 0, 0),
        WGS84 ("WGS-84 (GPS)", 6378137, 6356752.3142, 1.0 / 298.257223563),
        GRS80 ("GRS-80", 6378137, 6356752.3141, 1.0 / 298.257222101),
        Airy1830 ("Airy (1830)", 6377563.396, 6356256.909, 1.0 / 299.3249646),
        Intl1924 ("Int’l 1924", 6378388, 6356911.946, 1.0 / 297),
        Clarke1880 ("Clarke (1880)", 6378249.145, 6356514.86955, 1.0 / 293.465),
        GRS67 ("GRS-67", 6378160, 6356774.719, 1.0 / 298.25);
        
        private double iA, iB, iF;
        private String iName, iFirstCoord, iSecondCoord;
        
        Eclipsoid(String name, double a, double b) {
            this(name, "Latitude", "Longitude", a, b, (a - b) / a);
        }
        Eclipsoid(String name, double a, double b, double f) {
            this(name, "Latitude", "Longitude", a, b, f);
        }
        Eclipsoid(String name, String xCoord, String yCoord, double a, double b, double f) {
            iName = name;
            iFirstCoord = xCoord; iSecondCoord = yCoord;
            iA = a; iB = b; iF = f;
        }
        
        /** Major semiaxe A */
        public double a() { return iA; }
        /** Minor semiaxe B */
        public double b() { return iB; }
        /** Flattening (A-B) / A */
        public double f() { return iF; }
        
        /** Name of this coordinate system */
        public String getEclipsoindName() { return iName; }
        /** Name of the fist coordinate (e.g., Latitude) */
        public String getFirstCoordinateName() { return iFirstCoord; }
        /** Name of the second coordinate (e.g., Longitude) */
        public String getSecondCoordinateName() { return iSecondCoord; }
    }
    
    /** Ecliposid parameters, default to WGS-84 */
    private Eclipsoid iModel = Eclipsoid.WGS84;
    /** Student speed in meters per minute (defaults to 1000 meters in 15 minutes) */
    private double iSpeed = 1000.0 / 15;
    /** Back-to-back classes: maximal distance for no preference */
    private double iInstructorNoPreferenceLimit = 0.0;
    /** Back-to-back classes: maximal distance for discouraged preference */
    private double iInstructorDiscouragedLimit = 50.0;
    /**
     * Back-to-back classes: maximal distance for strongly discouraged preference
     * (everything above is prohibited)
     */
    private double iInstructorProhibitedLimit = 200.0;
    /** Default distance when given coordinates are null. */
    private double iNullDistance = 10000.0;
    
    /** Default properties */
    public DistanceMetric() {
    }
    
    /** With provided eclipsoid and student speed */
    public DistanceMetric(Eclipsoid model, double speed) {
        iModel = model;
        iSpeed = speed;
    }
    
    /** Configured using properties */
    public DistanceMetric(DataProperties properties) {
        if (properties.getProperty("Distances.Eclipsoid") == null) {
            //LEGACY MODE
            iModel = Eclipsoid.LEGACY;
            iSpeed = properties.getPropertyDouble("Student.DistanceLimit", 100.0 / 15);
        } else {
            iModel = Eclipsoid.valueOf(properties.getProperty("Distances.Eclipsoid", Eclipsoid.WGS84.name()));
            if (iModel == null) iModel = Eclipsoid.WGS84;
            iSpeed = properties.getPropertyDouble("Distances.Speed", 1000.0 / 15);
        }
        iNullDistance = properties.getPropertyDouble("Distances.NullDistance", iNullDistance);
        iInstructorNoPreferenceLimit = properties.getPropertyDouble("Instructor.NoPreferenceLimit", iInstructorNoPreferenceLimit);
        iInstructorDiscouragedLimit = properties.getPropertyDouble("Instructor.DiscouragedLimit", iInstructorDiscouragedLimit);
        iInstructorProhibitedLimit = properties.getPropertyDouble("Instructor.ProhibitedLimit", iInstructorProhibitedLimit);
    }

    /** Degrees to radians */
    protected double deg2rad(double deg) {
        return deg * Math.PI / 180;
    }
    
    /** Compute distance between the two given coordinates */
    public double getDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lat2 == null || lon1 == null || lon2 == null)
            return iNullDistance;
        
        // legacy mode -- euclidian distance, 1 unit is 10 meters
        if (iModel == Eclipsoid.LEGACY) {
            if (lat1 < 0 || lat2 < 0 || lon1 < 0 || lon2 < 0) return iNullDistance;
            double dx = lat1 - lat2;
            double dy = lon1 - lon2;
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        double a = iModel.a(), b = iModel.b(),  f = iModel.f();  // ellipsoid params
        double L = deg2rad(lon2-lon1);
        double U1 = Math.atan((1-f) * Math.tan(deg2rad(lat1)));
        double U2 = Math.atan((1-f) * Math.tan(deg2rad(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
        
        double lambda = L, lambdaP, iterLimit = 100;
        double cosSqAlpha, cos2SigmaM, sinSigma, cosSigma, sigma, sinLambda, cosLambda;
        do {
          sinLambda = Math.sin(lambda);
          cosLambda = Math.cos(lambda);
          sinSigma = Math.sqrt((cosU2*sinLambda) * (cosU2*sinLambda) + 
            (cosU1*sinU2-sinU1*cosU2*cosLambda) * (cosU1*sinU2-sinU1*cosU2*cosLambda));
          if (sinSigma==0) return 0;  // co-incident points
          cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;
          sigma = Math.atan2(sinSigma, cosSigma);
          double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
          cosSqAlpha = 1 - sinAlpha*sinAlpha;
          cos2SigmaM = cosSigma - 2*sinU1*sinU2/cosSqAlpha;
          if (Double.isNaN(cos2SigmaM)) cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (§6)
          double C = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
          lambdaP = lambda;
          lambda = L + (1-C) * f * sinAlpha *
            (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
        } while (Math.abs(lambda-lambdaP) > 1e-12 && --iterLimit>0);
        if (iterLimit==0) return Double.NaN; // formula failed to converge
       
        double uSq = cosSqAlpha * (a*a - b*b) / (b*b);
        double A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
        double B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
        double deltaSigma = B*sinSigma*(cos2SigmaM+B/4*(cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)-
          B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+4*cos2SigmaM*cos2SigmaM)));
        double s = b*A*(sigma-deltaSigma);
        
        // initial & final bearings
        // double fwdAz = Math.atan2(cosU2*sinLambda, cosU1*sinU2-sinU1*cosU2*cosLambda);
        // double revAz = Math.atan2(cosU1*sinLambda, -sinU1*cosU2+cosU1*sinU2*cosLambda);
        
        // s = s.toFixed(3); // round to 1mm precision
        return s;
    }
    
    /**
     * Compute distance in minutes.
     * Property Distances.Speed (in meters per minute) is used to convert meters to minutes, defaults to 1000 meters per 15 minutes (that means 66.67 meters per minute).
     */
    public int getDistanceInMinutes(double lat1, double lon1, double lat2, double lon2) {
        return (int) Math.round(getDistanceInMeters(lat1, lon1, lat2, lon2) / iSpeed);
    }
    
    /**
     * Converts minutes to meters.
     * Property Distances.Speed (in meters per minute) is used, defaults to 1000 meters per 15 minutes.
     */
    public double minutes2meters(int min) {
        return iSpeed * min;
    }
    

    /** Back-to-back classes in rooms within this limit have neutral preference */
    public double getInstructorNoPreferenceLimit() {
        return iInstructorNoPreferenceLimit;
    }

    /** Back-to-back classes in rooms within this limit have discouraged preference */
    public double getInstructorDiscouragedLimit() {
        return iInstructorDiscouragedLimit;
    }

    /** Back-to-back classes in rooms within this limit have strongly discouraged preference, it is prohibited to exceed this limit. */
    public double getInstructorProhibitedLimit() {
        return iInstructorProhibitedLimit;
    }

    /** Few tests */
    public static void main(String[] args) {
        System.out.println("Distance between Prague and Zlin: " + new DistanceMetric().getDistanceInMeters(50.087661, 14.420535, 49.226736, 17.668856) / 1000.0 + " km");
        System.out.println("Distance between ENAD and PMU: " + new DistanceMetric().getDistanceInMeters(40.428323, -86.912785, 40.425078, -86.911474) + " m");
        System.out.println("Distance between ENAD and ME: " + new DistanceMetric().getDistanceInMeters(40.428323, -86.912785, 40.429338, -86.91267) + " m");
        System.out.println("Distance between Prague and Zlin: " + new DistanceMetric().getDistanceInMinutes(50.087661, 14.420535, 49.226736, 17.668856) / 60 + " hours");
        System.out.println("Distance between ENAD and PMU: " + new DistanceMetric().getDistanceInMinutes(40.428323, -86.912785, 40.425078, -86.911474) + " minutes");
        System.out.println("Distance between ENAD and ME: " + new DistanceMetric().getDistanceInMinutes(40.428323, -86.912785, 40.429338, -86.91267) + " minutes");
    }

}
