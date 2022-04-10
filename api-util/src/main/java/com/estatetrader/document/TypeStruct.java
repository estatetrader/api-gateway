package com.estatetrader.document;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.estatetrader.annotation.Description;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.util.Lambda;

@Description("类型结构描述")
public class TypeStruct implements Serializable {
    @Description("结构名")
    public String name;

    @Description("分组名")
    public String groupName;

    @Description("成员")
    public List<FieldInfo> fields;

    @Description("类型的泛型变量列表，非泛型类型情况下，此数组为空")
    public List<String> typeVars;

    @Description("结构体类型的描述")
    public String description;

    public void merge(TypeStruct t) {
        try {
            if (!Objects.equals(name, t.name)) {
                throw new IllegalApiDefinitionException("struct name " + name + " is not matched with " + t.name);
            }

            if (!Objects.equals(typeVars, t.typeVars)) {
                throw new IllegalApiDefinitionException("struct type var does not match for " + name);
            }

            if (!Objects.equals(groupName, t.groupName)) {
                if (groupName == null || groupName.isEmpty()) {
                    groupName = t.groupName;
                } else if (t.groupName != null && !t.groupName.isEmpty()) {
                    groupName += "|" + t.groupName;
                }
            }

            if (!Objects.equals(description, t.description)) {
                if (description == null || description.isEmpty()) {
                    description = t.description;
                } else if (t.description != null && !t.description.isEmpty()) {
                    description += " " + t.description;
                }
            }

            for (FieldInfo field : t.fields) {
                FieldInfo target = Lambda.find(fields, f -> Objects.equals(f.name, field.name));
                if (target == null) {
                    fields.add(field);
                } else {
                    target.merge(field);
                }
            }
        } catch (IllegalApiDefinitionException e) {
            throw new IllegalApiDefinitionException(e.getMessage() + " in structure " + name, e);
        }
    }

    public FieldInfo findField(String fieldName) {
        if (fields == null) {
            return null;
        }
        for (FieldInfo field : fields) {
            if (fieldName.equals(field.name)) {
                return field;
            }
        }
        return null;
    }

    public String signature() {
        if (typeVars == null || typeVars.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name);
        sb.append('<');
        for (int i = 0; i < typeVars.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(typeVars.get(i));
        }
        sb.append('>');
        return sb.toString();
    }
//
//    public GenericTypeInfo declaredType() {
//        if (typeVars == null) {
//            return GenericTypeInfo.normalType(name);
//        }
//        List<GenericTypeInfo> typeArgs = new ArrayList<>(typeVars.size());
//        for (String typeVar : typeVars) {
//            typeArgs.add(GenericTypeInfo.typeVariable(typeVar));
//        }
//        return GenericTypeInfo.parameterizedType(name, typeArgs);
//    }

    public void validate() {
        if (name == null) {
            throw new IllegalApiDefinitionException("struct name should not be null");
        }
        if (!name.matches("^[A-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalApiDefinitionException("invalid struct name " + name);
        }
        for (FieldInfo field : fields) {
            field.validate();
        }
        if (typeVars != null) {
            for (String typeVar : typeVars) {
                if (!typeVar.matches("^[A-Z]+[0-9]*$")) {
                    throw new IllegalApiDefinitionException("invalid type var " + typeVar);
                }
            }
        }
    }

    public Map<String, GenericTypeInfo> variableBinding(GenericTypeInfo actualType) {
        if (!name.equals(actualType.typeName)) {
            throw new IllegalArgumentException("the given actual type " + actualType
                + " does not matches the struct " + signature());
        }
        if (typeVars == null || typeVars.isEmpty()) {
            return Collections.emptyMap();
        }
        if (actualType.typeArgs == null || actualType.typeArgs.size() != typeVars.size()) {
            throw new IllegalArgumentException("the given actual type " + actualType
                + " does not matches the struct " + signature());
        }
        Map<String, GenericTypeInfo> binding = new LinkedHashMap<>(typeVars.size());
        for (int i = 0; i < typeVars.size(); i++) {
            binding.put(typeVars.get(i), actualType.typeArgs.get(i));
        }
        if (binding.size() != typeVars.size()) {
            throw new IllegalArgumentException("duplicated type vars found in " + signature());
        }
        return binding;
    }
}
