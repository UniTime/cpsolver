package org.cpsolver.instructor.model;

public class Attribute {
    private Long iAttributeId;
    private String iAttributeName;
    private Type iType;
    
    public Attribute(long attributeId, String attributeName, Type type) {
        iAttributeId = attributeId;
        iAttributeName = attributeName;
        iType = type;
    }
    
    public Long getAttributeId() { return iAttributeId; }
    public String getAttributeName() { return iAttributeName == null ? "A" + iAttributeId : iAttributeName; }
    public Type getType() { return iType; }
    
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
    
    public static class Type {
        private Long iTypeId;
        private String iTypeName;
        private boolean iRequired;
        private boolean iConjunctive;

        public Type(long typeId, String typeName, boolean conjunctive, boolean required) {
            iTypeId = typeId;
            iTypeName = typeName;
            iConjunctive = conjunctive;
            iRequired = required;
        }
        
        public Long getTypeId() { return iTypeId; }
        public String getTypeName() { return iTypeName == null ? "T" + iTypeId : iTypeName; }
        public boolean isRequired() { return iRequired; }
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
