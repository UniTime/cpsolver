package org.cpsolver.ifs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cpsolver.studentsct.constraint.HardDistanceConflicts;

/**
 * Common class for computing distances and back-to-back instructor / student conflicts.
 * 
 * When property Distances.Ellipsoid is set, the distances are computed using the given (e.g., WGS84, see {@link Ellipsoid}).
 * In the legacy mode (when ellipsoid is not set), distances are computed using Euclidian distance and 1 unit is considered 10 meters.
 * <br><br>
 * For student back-to-back conflicts, Distances.Speed (in meters per minute) is considered and compared with the break time
 * of the earlier class.
 * <br><br>
 * For instructors, the preference is computed using the distance in meters and the three constants 
 * Instructor.NoPreferenceLimit (distance &lt;= limit &rarr; no preference), Instructor.DiscouragedLimit (distance &lt;= limit &rarr; discouraged),
 * Instructor.ProhibitedLimit (distance &lt;= limit &rarr; strongly discouraged), the back-to-back placement is prohibited when the distance is over the last limit.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
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
public class DistanceMetric {
    public static enum Ellipsoid {
        LEGACY ("Euclidean metric (1 unit equals to 10 meters)", "X-Coordinate", "Y-Coordinate", 0, 0, 0),
        WGS84 ("WGS-84 (GPS)", 6378137, 6356752.3142, 1.0 / 298.257223563),
        GRS80 ("GRS-80", 6378137, 6356752.3141, 1.0 / 298.257222101),
        Airy1830 ("Airy (1830)", 6377563.396, 6356256.909, 1.0 / 299.3249646),
        Intl1924 ("Int'l 1924", 6378388, 6356911.946, 1.0 / 297),
        Clarke1880 ("Clarke (1880)", 6378249.145, 6356514.86955, 1.0 / 293.465),
        GRS67 ("GRS-67", 6378160, 6356774.719, 1.0 / 298.25);
        
        private double iA, iB, iF;
        private String iName, iFirstCoord, iSecondCoord;
        
        Ellipsoid(String name, double a, double b) {
            this(name, "Latitude", "Longitude", a, b, (a - b) / a);
        }
        Ellipsoid(String name, double a, double b, double f) {
            this(name, "Latitude", "Longitude", a, b, f);
        }
        Ellipsoid(String name, String xCoord, String yCoord, double a, double b, double f) {
            iName = name;
            iFirstCoord = xCoord; iSecondCoord = yCoord;
            iA = a; iB = b; iF = f;
        }
        
        /** Major semiaxe A 
         * @return major semiaxe A
         **/
        public double a() { return iA; }
        /** Minor semiaxe B
         * @return major semiaxe B
         **/
        public double b() { return iB; }
        /** Flattening (A-B) / A
         * @return Flattening (A-B) / A 
         **/
        public double f() { return iF; }
        
        /** Name of this coordinate system
         * @return elipsoid name 
         **/
        public String getEclipsoindName() { return iName; }
        /** Name of the fist coordinate (e.g., Latitude) 
         * @return first coordinate's name 
         **/
        public String getFirstCoordinateName() { return iFirstCoord; }
        /** Name of the second coordinate (e.g., Longitude)
         * @return second coordinate's name
         **/
        public String getSecondCoordinateName() { return iSecondCoord; }
    }
    
    /** Elliposid parameters, default to WGS-84 */
    private Ellipsoid iModel = Ellipsoid.WGS84;
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
    /** 
     * When Distances.ComputeDistanceConflictsBetweenNonBTBClasses is enabled, distance limit (in minutes)
     * for a long travel.  
     */
    private double iInstructorLongTravelInMinutes = 30.0;
    
    /** Default distance when given coordinates are null. */
    private double iNullDistance = 10000.0;
    /** Maximal travel time in minutes when no coordinates are given. */
    private int iMaxTravelTime = 60;
    /** Travel times overriding the distances computed from coordintaes */
    private Map<Long, Map<Long, Integer>> iTravelTimes = new HashMap<Long, Map<Long,Integer>>();
    /** Distance cache  */
    private Map<String, Double> iDistanceCache = new HashMap<String, Double>();
    /** True if distances should be considered between classes that are NOT back-to-back */
    private boolean iComputeDistanceConflictsBetweenNonBTBClasses = false;
    /** Reference of the accommodation of students that need short distances */
    private String iShortDistanceAccommodationReference = "SD";
    /** Allowed distance in minutes (for {@link HardDistanceConflicts}) */
    private int iAllowedDistanceInMinutes = 30;
    /** Hard distance limit in minutes (for {@link HardDistanceConflicts}) */
    private int iDistanceHardLimitInMinutes = 60;
    /** Long distance limit in minutes (for display) */
    private int iDistanceLongLimitInMinutes = 60;
    /** Hard distance conflicts enabled (for {@link HardDistanceConflicts}) */
    private boolean iHardDistanceConflicts = false;
    
    private final ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();
    
    /** Default properties */
    public DistanceMetric() {
    }
    
    public DistanceMetric(DistanceMetric m) {
        iModel = m.iModel;
        iSpeed = m.iSpeed;
        iInstructorNoPreferenceLimit = m.iInstructorNoPreferenceLimit;
        iInstructorDiscouragedLimit = m.iInstructorDiscouragedLimit;
        iInstructorProhibitedLimit = m.iInstructorProhibitedLimit;
        iInstructorLongTravelInMinutes = m.iInstructorLongTravelInMinutes;
        iNullDistance = m.iNullDistance;
        iMaxTravelTime = m.iMaxTravelTime;
        iComputeDistanceConflictsBetweenNonBTBClasses = m.iComputeDistanceConflictsBetweenNonBTBClasses;
        iShortDistanceAccommodationReference = m.iShortDistanceAccommodationReference;
        iAllowedDistanceInMinutes = m.iAllowedDistanceInMinutes;
        iDistanceHardLimitInMinutes = m.iDistanceHardLimitInMinutes;
        iDistanceLongLimitInMinutes = m.iDistanceLongLimitInMinutes;
        iHardDistanceConflicts = m.iHardDistanceConflicts;
        m.iLock.readLock().lock();
        try {
            for (Map.Entry<Long, Map<Long, Integer>> e: m.iTravelTimes.entrySet())
                iTravelTimes.put(e.getKey(), new HashMap<Long, Integer>(e.getValue()));
        } finally {
            m.iLock.readLock().unlock();
        }
    }
    
    /** With provided ellipsoid 
     * @param model ellipsoid model
     **/
    public DistanceMetric(Ellipsoid model) {
        iModel = model;
        if (iModel == Ellipsoid.LEGACY) {
            iSpeed = 100.0 / 15;
            iInstructorDiscouragedLimit = 5.0;
            iInstructorProhibitedLimit = 20.0;
        }
    }

    /** With provided ellipsoid and student speed
     * @param model ellipsoid model
     * @param speed student speed in meters per minute 
     **/
    public DistanceMetric(Ellipsoid model, double speed) {
        iModel = model;
        iSpeed = speed;
    }
    
    /** Configured using properties 
     * @param properties solver configuration
     **/
    public DistanceMetric(DataProperties properties) {
        if (Ellipsoid.LEGACY.name().equals(properties.getProperty("Distances.Ellipsoid",Ellipsoid.LEGACY.name()))) {
            //LEGACY MODE
            iModel = Ellipsoid.LEGACY;
            iSpeed = properties.getPropertyDouble("Student.DistanceLimit", 1000.0 / 15) / 10.0;
            iInstructorNoPreferenceLimit = properties.getPropertyDouble("Instructor.NoPreferenceLimit", 0.0);
            iInstructorDiscouragedLimit = properties.getPropertyDouble("Instructor.DiscouragedLimit", 5.0);
            iInstructorProhibitedLimit = properties.getPropertyDouble("Instructor.ProhibitedLimit", 20.0);
            iNullDistance = properties.getPropertyDouble("Distances.NullDistance", 1000.0);
            iMaxTravelTime = properties.getPropertyInt("Distances.MaxTravelDistanceInMinutes", 60);
        } else {
            iModel = Ellipsoid.valueOf(properties.getProperty("Distances.Ellipsoid", Ellipsoid.WGS84.name()));
            if (iModel == null) iModel = Ellipsoid.WGS84;
            iSpeed = properties.getPropertyDouble("Distances.Speed", properties.getPropertyDouble("Student.DistanceLimit", 1000.0 / 15));
            iInstructorNoPreferenceLimit = properties.getPropertyDouble("Instructor.NoPreferenceLimit", iInstructorNoPreferenceLimit);
            iInstructorDiscouragedLimit = properties.getPropertyDouble("Instructor.DiscouragedLimit", iInstructorDiscouragedLimit);
            iInstructorProhibitedLimit = properties.getPropertyDouble("Instructor.ProhibitedLimit", iInstructorProhibitedLimit);
            iNullDistance = properties.getPropertyDouble("Distances.NullDistance", iNullDistance);
            iMaxTravelTime = properties.getPropertyInt("Distances.MaxTravelDistanceInMinutes", 60);
        }
        iComputeDistanceConflictsBetweenNonBTBClasses = properties.getPropertyBoolean(
                "Distances.ComputeDistanceConflictsBetweenNonBTBClasses", iComputeDistanceConflictsBetweenNonBTBClasses);
        iShortDistanceAccommodationReference = properties.getProperty(
                "Distances.ShortDistanceAccommodationReference", iShortDistanceAccommodationReference);
        iInstructorLongTravelInMinutes = properties.getPropertyDouble("Instructor.InstructorLongTravelInMinutes", 30.0);
        iAllowedDistanceInMinutes = properties.getPropertyInt("HardDistanceConflict.AllowedDistanceInMinutes", iAllowedDistanceInMinutes);
        iDistanceHardLimitInMinutes = properties.getPropertyInt("HardDistanceConflict.DistanceHardLimitInMinutes", iDistanceHardLimitInMinutes);
        iDistanceLongLimitInMinutes = properties.getPropertyInt("HardDistanceConflict.DistanceLongLimitInMinutes", iDistanceLongLimitInMinutes);
        iHardDistanceConflicts = properties.getPropertyBoolean("Sectioning.HardDistanceConflict", iHardDistanceConflicts);
    }

    /** Degrees to radians 
     * @param deg degrees
     * @return radians
     **/
    protected double deg2rad(double deg) {
        return deg * Math.PI / 180;
    }
    
    /** Compute distance between the two given coordinates
     * @param lat1 first coordinate's latitude
     * @param lon1 first coordinate's longitude
     * @param lat2 second coordinate's latitude
     * @param lon2 second coordinate's longitude
     * @return distance in meters
     * @deprecated Use @{link {@link DistanceMetric#getDistanceInMeters(Long, Double, Double, Long, Double, Double)} instead (to include travel time matrix when available).
     */
    @Deprecated
    public double getDistanceInMeters(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lat2 == null || lon1 == null || lon2 == null)
            return iNullDistance;
        
        if (lat1.equals(lat2) && lon1.equals(lon2)) return 0.0;
        
        // legacy mode -- euclidian distance, 1 unit is 10 meters
        if (iModel == Ellipsoid.LEGACY) {
            if (lat1 < 0 || lat2 < 0 || lon1 < 0 || lon2 < 0) return iNullDistance;
            double dx = lat1 - lat2;
            double dy = lon1 - lon2;
            return Math.sqrt(dx * dx + dy * dy);
        }
        
        String id = null;
        if (lat1 < lat2 || (lat1 == lat2 && lon1 <= lon2)) {
            id =
                Long.toHexString(Double.doubleToRawLongBits(lat1)) +
                Long.toHexString(Double.doubleToRawLongBits(lon1)) +
                Long.toHexString(Double.doubleToRawLongBits(lat2)) +
                Long.toHexString(Double.doubleToRawLongBits(lon2));
        } else {
            id =
                Long.toHexString(Double.doubleToRawLongBits(lat1)) +
                Long.toHexString(Double.doubleToRawLongBits(lon1)) +
                Long.toHexString(Double.doubleToRawLongBits(lat2)) +
                Long.toHexString(Double.doubleToRawLongBits(lon2));
        }
        
        iLock.readLock().lock();
        try {
            Double distance = iDistanceCache.get(id);
            if (distance != null) return distance;
        } finally {
            iLock.readLock().unlock();
        }
        
        iLock.writeLock().lock();
        try {
            Double distance = iDistanceCache.get(id);
            if (distance != null) return distance;

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
              if (Double.isNaN(cos2SigmaM)) cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (�6)
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
            
            // initial & final bearings
            // double fwdAz = Math.atan2(cosU2*sinLambda, cosU1*sinU2-sinU1*cosU2*cosLambda);
            // double revAz = Math.atan2(cosU1*sinLambda, -sinU1*cosU2+cosU1*sinU2*cosLambda);
            
            // s = s.toFixed(3); // round to 1mm precision

            distance = b*A*(sigma-deltaSigma);
            iDistanceCache.put(id, distance);
            return distance;
        } finally {
            iLock.writeLock().unlock();
        }
    }
    
    /**
     * Compute distance in minutes.
     * Property Distances.Speed (in meters per minute) is used to convert meters to minutes, defaults to 1000 meters per 15 minutes (that means 66.67 meters per minute).
     * @param lat1 first coordinate's latitude
     * @param lon1 first coordinate's longitude
     * @param lat2 second coordinate's latitude
     * @param lon2 second coordinate's longitude
     * @return distance in minutes
     * @deprecated Use @{link {@link DistanceMetric#getDistanceInMinutes(Long, Double, Double, Long, Double, Double)} instead (to include travel time matrix when available).
     */
    @Deprecated
    public int getDistanceInMinutes(double lat1, double lon1, double lat2, double lon2) {
        return (int) Math.round(getDistanceInMeters(lat1, lon1, lat2, lon2) / iSpeed);
    }
    
    /**
     * Converts minutes to meters.
     * Property Distances.Speed (in meters per minute) is used, defaults to 1000 meters per 15 minutes.
     * @param min minutes to travel
     * @return meters to travel
     */
    public double minutes2meters(int min) {
        return iSpeed * min;
    }
    

    /** Back-to-back classes in rooms within this limit have neutral preference 
     * @return limit in meters
     **/
    public double getInstructorNoPreferenceLimit() {
        return iInstructorNoPreferenceLimit;
    }

    /** Back-to-back classes in rooms within this limit have discouraged preference 
     * @return limit in meters
     **/
    public double getInstructorDiscouragedLimit() {
        return iInstructorDiscouragedLimit;
    }

    /** Back-to-back classes in rooms within this limit have strongly discouraged preference, it is prohibited to exceed this limit.
     * @return limit in meters 
     **/
    public double getInstructorProhibitedLimit() {
        return iInstructorProhibitedLimit;
    }
    
    /**
     * When Distances.ComputeDistanceConflictsBetweenNonBTBClasses is enabled, distance limit (in minutes)
     * for a long travel.
     * @return travel time in minutes
     */
    public double getInstructorLongTravelInMinutes() {
        return iInstructorLongTravelInMinutes;
    }
    
    /** True if legacy mode is used (Euclidian distance where 1 unit is 10 meters) 
     * @return true if the ellipsoid model is the old one
     **/
    public boolean isLegacy() {
        return iModel == Ellipsoid.LEGACY;
    }
    
    /** Maximal travel distance between rooms when no coordinates are given 
     * @return travel time in minutes
     **/
    public int getMaxTravelDistanceInMinutes() {
        return iMaxTravelTime;
    }
    
    /** Set maximal travel distance between rooms when no coordinates are given
     * @param maxTravelTime max travel time in minutes
     */
    public void setMaxTravelDistanceInMinutes(int maxTravelTime) {
        iMaxTravelTime = maxTravelTime;
    }

    /** Add travel time between two locations 
     * @param roomId1 first room's id
     * @param roomId2 second room's id
     * @param travelTimeInMinutes travel time in minutes 
     **/
    public void addTravelTime(Long roomId1, Long roomId2, Integer travelTimeInMinutes) {
        iLock.writeLock().lock();
        try {
            if (roomId1 == null || roomId2 == null) return;
            if (roomId1 < roomId2) {
                Map<Long, Integer> times = iTravelTimes.get(roomId1);
                if (times == null) { times = new HashMap<Long, Integer>(); iTravelTimes.put(roomId1, times); }
                if (travelTimeInMinutes == null)
                    times.remove(roomId2);
                else
                    times.put(roomId2, travelTimeInMinutes);
            } else {
                Map<Long, Integer> times = iTravelTimes.get(roomId2);
                if (times == null) { times = new HashMap<Long, Integer>(); iTravelTimes.put(roomId2, times); }
                if (travelTimeInMinutes == null)
                    times.remove(roomId1);
                else
                    times.put(roomId1, travelTimeInMinutes);
            }            
        } finally {
            iLock.writeLock().unlock();
        }
    }
    
    /** Return travel time between two locations. 
     * @param roomId1 first room's id
     * @param roomId2 second room's id
     * @return travel time in minutes
     **/
    public Integer getTravelTimeInMinutes(Long roomId1, Long roomId2) {
        iLock.readLock().lock();
        try {
            if (roomId1 == null || roomId2 == null) return null;
            if (roomId1 < roomId2) {
                Map<Long, Integer> times = iTravelTimes.get(roomId1);
                return (times == null ? null : times.get(roomId2));
            } else {
                Map<Long, Integer> times = iTravelTimes.get(roomId2);
                return (times == null ? null : times.get(roomId1));
            }
        } finally {
            iLock.readLock().unlock();
        }
    }
    
    /** Return travel time between two locations. Travel times are used when available, use coordinates otherwise. 
     * @param roomId1 first room's id
     * @param lat1 first room's latitude
     * @param lon1 first room's longitude
     * @param roomId2 second room's id
     * @param lat2 second room's latitude
     * @param lon2 second room's longitude
     * @return distance in minutes
     **/
    public Integer getDistanceInMinutes(Long roomId1, Double lat1, Double lon1, Long roomId2, Double lat2, Double lon2) {
        Integer distance = getTravelTimeInMinutes(roomId1, roomId2);
        if (distance != null) return distance;
        
        if (lat1 == null || lat2 == null || lon1 == null || lon2 == null)
            return getMaxTravelDistanceInMinutes();
        else 
            return (int) Math.min(getMaxTravelDistanceInMinutes(), Math.round(getDistanceInMeters(lat1, lon1, lat2, lon2) / iSpeed));
    }
    
    /** Return travel distance between two locations.  Travel times are used when available, use coordinates otherwise
     * @param roomId1 first room's id
     * @param lat1 first room's latitude
     * @param lon1 first room's longitude
     * @param roomId2 second room's id
     * @param lat2 second room's latitude
     * @param lon2 second room's longitude
     * @return distance in meters
     **/
    public double getDistanceInMeters(Long roomId1, Double lat1, Double lon1, Long roomId2, Double lat2, Double lon2) {
        Integer distance = getTravelTimeInMinutes(roomId1, roomId2);
        if (distance != null) return minutes2meters(distance);
        
        return getDistanceInMeters(lat1, lon1, lat2, lon2);
    }
    
    /** Return travel times matrix
     * @return travel times matrix
     **/
    public Map<Long, Map<Long, Integer>> getTravelTimes() { return iTravelTimes; }
    
    /**
     * True if distances should be considered between classes that are NOT back-to-back. Distance in minutes is then 
     * to be compared with the difference between end of the last class and start of the second class plus break time of the first class.
     * @return true if distances should be considered between classes that are NOT back-to-back
     **/
    public boolean doComputeDistanceConflictsBetweenNonBTBClasses() {
        return iComputeDistanceConflictsBetweenNonBTBClasses;
    }
    
    public void setComputeDistanceConflictsBetweenNonBTBClasses(boolean computeDistanceConflictsBetweenNonBTBClasses) {
        iComputeDistanceConflictsBetweenNonBTBClasses = computeDistanceConflictsBetweenNonBTBClasses;
    }
    
    /**
     * Reference of the accommodation of students that need short distances
     */
    public String getShortDistanceAccommodationReference() {
        return iShortDistanceAccommodationReference;
    }
    
    /** Allowed distance in minutes (for {@link HardDistanceConflicts}) */
    public int getAllowedDistanceInMinutes() {
        return iAllowedDistanceInMinutes;
    }
    /** Hard distance limit in minutes (for {@link HardDistanceConflicts}) */
    public int getDistanceHardLimitInMinutes() {
        return iDistanceHardLimitInMinutes;
    }
    /** Long distance limit in minutes (for display) */
    public int getDistanceLongLimitInMinutes() {
        return iDistanceLongLimitInMinutes;
    }
    /** Hard distance conflicts enabled (for {@link HardDistanceConflicts}) */
    public boolean isHardDistanceConflictsEnabled() {
        return iHardDistanceConflicts;
    }

    
    /** Few tests 
     * @param args program arguments
     **/
    public static void main(String[] args) {
        System.out.println("Distance between Prague and Zlin: " + new DistanceMetric().getDistanceInMeters(50.087661, 14.420535, 49.226736, 17.668856) / 1000.0 + " km");
        System.out.println("Distance between ENAD and PMU: " + new DistanceMetric().getDistanceInMeters(40.428323, -86.912785, 40.425078, -86.911474) + " m");
        System.out.println("Distance between ENAD and ME: " + new DistanceMetric().getDistanceInMeters(40.428323, -86.912785, 40.429338, -86.91267) + " m");
        System.out.println("Distance between Prague and Zlin: " + new DistanceMetric().getDistanceInMinutes(50.087661, 14.420535, 49.226736, 17.668856) / 60 + " hours");
        System.out.println("Distance between ENAD and PMU: " + new DistanceMetric().getDistanceInMinutes(40.428323, -86.912785, 40.425078, -86.911474) + " minutes");
        System.out.println("Distance between ENAD and ME: " + new DistanceMetric().getDistanceInMinutes(40.428323, -86.912785, 40.429338, -86.91267) + " minutes");
    }

}
