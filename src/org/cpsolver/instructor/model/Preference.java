package org.cpsolver.instructor.model;

import org.cpsolver.coursett.Constants;

public class Preference<T> {
    protected T iTarget;
    protected int iPreference;
    
    public Preference(T target, int preference) {
        iTarget = target;
        iPreference = preference;
    }
    
    public boolean isRequired() { return iPreference < Constants.sPreferenceLevelRequired / 2; }
    
    public boolean isProhibited() { return iPreference > Constants.sPreferenceLevelProhibited / 2; }
    
    public int getPreference() { return iPreference; }
    
    public T getTarget() { return iTarget; }
    
    @Override
    public String toString() { return getTarget() + ": " + (isRequired() ? "R" : isProhibited() ? "P" : getPreference()); }
}
