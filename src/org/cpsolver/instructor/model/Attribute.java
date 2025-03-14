package org.cpsolver.instructor.model;

/**
 * Attributes of an instructor. Each instructor can have a number of attributes and there are attribute preferences on teaching requests.
 * Each attribute has an id, a name and a {@link Type}.
 * 
 * @author  Tomas Muller
 * @version IFS 1.3 (Instructor Sectioning)<br>
 *          Copyright (C) 2016 Tomas Muller<br>
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
public class Attribute {
    private Long iAttributeId;
    private String iAttributeName;
    private Type iType;
    private Attribute iParentAttribute;
    
    /**
     * Constructor
     * @param attributeId attribute id
     * @param attributeName attribute name
     * @param type attribute type
     */
    public Attribute(long attributeId, String attributeName, Type type) {
        iAttributeId = attributeId;
        iAttributeName = attributeName;
        iType = type;
    }
    
    /**
     * Attribute id that was provided in the constructor
     * @return attribute id
     */
    public Long getAttributeId() { return iAttributeId; }
    
    /**
     * Attribute name that was provided in the constructor
     * @return attribute name
     */
    public String getAttributeName() { return iAttributeName == null ? "A" + iAttributeId : iAttributeName; }
    
    /**
     * Attribute type that was provided in the constructor
     * @return attribute type
     */
    public Type getType() { return iType; }
    
    /**
     * Parent attribute
     */
    public Attribute getParentAttribute() { return iParentAttribute; }
    
    /**
     * Parent attribute
     */
    public void setParentAttribute(Attribute parent) { iParentAttribute = parent; }
    
    @Override
    public int hashCode() {
        return (getAttributeId() == null ? getAttributeName().hashCode() : getAttributeId().hashCode());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Attribute)) return false;
        Attribute a = (Attribute)o;
        return getAttributeId() == null ? getAttributeName().equals(a.getAttributeName()) : getAttributeId().equals(a.getAttributeId());
    }
    
    @Override
    public String toString() { return getAttributeName() + " (" + getType() + ")"; }
    
    /**
     * Attribute type. Each type has an id and a name. It can also define whether attributes of this type are required and/or conjunctive.
     * If an attribute is required, this means that only instructors that have the attribute can be used, even if it is only preferred. This
     * allows to put different preferences on multiple attribute that are required.
     * If a teaching requests require two attributes that are of the same type which is conjunctive, only instructors that have BOTH attributes can be used.
     * It the type is disjunctive (not conjunctive), it is sufficient for the instructor to have one of the two required attributes. 
     */
    public static class Type {
        private Long iTypeId;
        private String iTypeName;
        private boolean iRequired;
        private boolean iConjunctive;

        /**
         * Constructor
         * @param typeId attribute type id
         * @param typeName attribute type name
         * @param conjunctive is attribute type conjunctive (if two attributes are required a student must have both). 
         * @param required is the attribute type required
         */
        public Type(long typeId, String typeName, boolean conjunctive, boolean required) {
            iTypeId = typeId;
            iTypeName = typeName;
            iConjunctive = conjunctive;
            iRequired = required;
        }
        
        /**
         * Attribute type id that was provided in the constructor
         * @return attribute type id
         */
        public Long getTypeId() { return iTypeId; }
        
        /**
         * Attribute type name that was provided in the constructor
         * @return attribute type name
         */
        public String getTypeName() { return iTypeName == null ? "T" + iTypeId : iTypeName; }
        
        /**
         * If an attribute is required, this means that only instructors that have the attribute can be used, even if it is only preferred. This
         * allows to put different preferences on multiple attribute that are required.
         * @return true if this attribute type is required
         */
        public boolean isRequired() { return iRequired; }
        
        /**
         * If a teaching requests require two attributes that are of the same type which is conjunctive, only instructors that have BOTH attributes can be used.
         * It the type is disjunctive (not conjunctive), it is sufficient for the instructor to have one of the two required attributes.
         * @return true if this attribute type is conjunctive, false if disjunctive
         */
        public boolean isConjunctive() { return iConjunctive; }

        @Override
        public int hashCode() {
            return getTypeId() == null ? getTypeName().hashCode() : getTypeId().hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Type)) return false;
            Type t = (Type)o;
            return getTypeId() == null ? getTypeName().equals(t.getTypeName()) : getTypeId().equals(t.getTypeId());
        }
        
        @Override
        public String toString() { return getTypeName(); }
    }

}
