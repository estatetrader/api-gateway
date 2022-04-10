package com.estatetrader.document;

import com.estatetrader.annotation.Description;
import com.estatetrader.define.IllegalApiDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Description("泛化的类型信息")
public class GenericTypeInfo implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericTypeInfo.class);

    public static final String STRING_TYPE_NAME = "string";
    public static final String LIST_TYPE_NAME = "list";
    public static final String MAP_TYPE_NAME = "map";

    @Description("泛化类型的原始类型，如果本类型表示的是非泛化类型，则此字段表示的是该类型的具体类型。除typeArgs外不可与其他字段同时使用")
    public String typeName;

    @Description("泛化类型的类型参数，如果本类型为非泛化类型，则字段为null。只可与typeName同时使用")
    public List<GenericTypeInfo> typeArgs;

    @Description("非空表示本类型实际上为一个类型变量，不可与其他字段同时使用")
    public String typeVar;

    @Description("非空表示本类型是个运行时动态类型，可取多个可能的类型中的某一个。不可与其他字段同时使用")
    public Map<String, GenericTypeInfo> possibleTypes;

    private GenericTypeInfo() {
    }

    public GenericTypeInfo merge(GenericTypeInfo another) {
        if (typeName != null) {
            if (another.typeName == null) {
                throw new IllegalApiDefinitionException("type name does not match " + typeName);
            }
            String newTypeName;
            if (typeName.equals(another.typeName)) {
                newTypeName = typeName;
            } else {
                String promotedType = tryPromoteToMatchType(typeName, another.typeName);
                if (promotedType != null) {
                    newTypeName = promotedType;
                } else {
                    LOGGER.warn("type name does not match {} with {}", typeName, another.typeName);
                    newTypeName = another.typeName;
//                    throw new IllegalApiDefinitionException("type name does not match "
//                    + typeName + " with " + another.typeName);
                }
            }
            if (typeArgs != null) {
                if (another.typeArgs == null || typeArgs.size() != another.typeArgs.size()) {
                    throw new IllegalApiDefinitionException("the param length is not matched for type " + typeName);
                }
                List<GenericTypeInfo> newTypeArgs = new ArrayList<>(typeArgs.size());
                for (int i = 0; i < typeArgs.size(); i++) {
                    newTypeArgs.add(typeArgs.get(i).merge(another.typeArgs.get(i)));
                }
                return parameterizedType(newTypeName, newTypeArgs);
            } else {
                return normalType(newTypeName);
            }
        } else if (typeVar != null) {
            if (!typeVar.equals(another.typeVar)) {
                throw new IllegalApiDefinitionException("type var mismatch " + typeVar + " with " + another.typeVar);
            }
            return typeVariable(typeVar);
        } else if (possibleTypes != null) {
            if (another.possibleTypes == null) {
                throw new IllegalApiDefinitionException("generic type mismatch");
            }
            Map<String, GenericTypeInfo> newPossibleTypes = new LinkedHashMap<>(possibleTypes);
            for (Map.Entry<String, GenericTypeInfo> entry : another.possibleTypes.entrySet()) {
                GenericTypeInfo possibleType = possibleTypes.get(entry.getKey());
                if (possibleType == null) {
                    newPossibleTypes.put(entry.getKey(), entry.getValue());
                } else {
                    newPossibleTypes.put(entry.getKey(), possibleType.merge(entry.getValue()));
                }
            }
            return unionType(newPossibleTypes);
        } else {
            throw new IllegalApiDefinitionException("invalid generic type");
        }
    }

    public GenericTypeInfo resolveTypeVars(Map<String, GenericTypeInfo> binding) {
        if (typeVar != null) {
            GenericTypeInfo typeArg = binding.get(typeVar);
            return typeArg != null ? typeArg : this;
        } else if (typeName != null) {
            if (typeArgs == null) {
                return this;
            }
            List<GenericTypeInfo> newTypeArgs = new ArrayList<>(typeArgs.size());
            for (GenericTypeInfo typeArg : typeArgs) {
                newTypeArgs.add(typeArg.resolveTypeVars(binding));
            }
            return parameterizedType(typeName, newTypeArgs);
        } else if (possibleTypes != null) {
            Map<String, GenericTypeInfo> newPossibleTypes = new LinkedHashMap<>(possibleTypes.size());
            for (Map.Entry<String, GenericTypeInfo> entry : possibleTypes.entrySet()) {
                newPossibleTypes.put(entry.getKey(), entry.getValue().resolveTypeVars(binding));
            }
            return unionType(newPossibleTypes);
        } else {
            return this;
        }
    }

    public void validate() {
        if (typeName != null) {
            if (LIST_TYPE_NAME.equals(typeName)) {
                if (typeArgs != null && typeArgs.size() == 1) {
                    return;
                }
                throw new IllegalApiDefinitionException("exactly one type argument is required for list");
            }
            if (!typeName.matches("^[a-zA-Z]+[a-zA-Z0-9_\\.]*$")) {
                throw new IllegalApiDefinitionException("invalid type name " + typeName);
            }
            if (typeArgs == null) {
                return;
            }
            for (GenericTypeInfo t : typeArgs) {
                t.validate();
            }
        } else if (typeVar != null) {
            if (typeVar.isEmpty() || !typeVar.matches("^[A-Z]+[0-9]*$")) {
                throw new IllegalApiDefinitionException("invalid type var " + typeVar);
            }
        } else if (possibleTypes != null) {
            if (possibleTypes.isEmpty()) {
                throw new IllegalApiDefinitionException("possible types must not be empty");
            }
            for (GenericTypeInfo t : possibleTypes.values()) {
                if (t.typeName == null) {
                    throw new IllegalApiDefinitionException("the typeName of the possible type "
                        + t + " should not be empty");
                }
                t.validate();
            }
        } else {
            throw new IllegalApiDefinitionException("empty generic type");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GenericTypeInfo)) return false;

        GenericTypeInfo that = (GenericTypeInfo) object;

        if (!Objects.equals(typeName, that.typeName)) return false;
        if (!Objects.equals(typeArgs, that.typeArgs)) return false;
        if (!Objects.equals(typeVar, that.typeVar)) return false;
        if (!Objects.equals(possibleTypes, that.possibleTypes))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = typeName != null ? typeName.hashCode() : 0;
        result = 31 * result + (typeArgs != null ? typeArgs.hashCode() : 0);
        result = 31 * result + (typeVar != null ? typeVar.hashCode() : 0);
        result = 31 * result + (possibleTypes != null ? possibleTypes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (typeName != null) {
            if (typeArgs == null || typeArgs.isEmpty()) {
                return typeName;
            }
            if (LIST_TYPE_NAME.equals(typeName) && typeArgs.size() == 1) {
                return "[" + typeArgs.get(0).toString() + "]";
            }
            if (MAP_TYPE_NAME.equals(typeName) && typeArgs.size() == 2) {
                return "[" + typeArgs.get(0) + ": " + typeArgs.get(1) + "]";
            }
            StringBuilder sb = new StringBuilder(typeName);
            sb.append('<');
            for (int i = 0; i < typeArgs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                GenericTypeInfo arg = typeArgs.get(i);
                sb.append(arg);
            }
            sb.append('>');
            return sb.toString();
        } else if (typeVar != null) {
            return typeVar;
        } else if (possibleTypes != null) {
            return possibleTypes.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(" | ", "(", ")"));
        } else {
            return null;
        }
    }

    public static GenericTypeInfo normalType(String typeName) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = typeName;
        return type;
    }

    public static GenericTypeInfo parameterizedType(String typeName, GenericTypeInfo... typeArgs) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = typeName;
        type.typeArgs = Arrays.asList(typeArgs);
        return type;
    }

    public static GenericTypeInfo stringType() {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = STRING_TYPE_NAME;
        return type;
    }

    public static GenericTypeInfo listType(GenericTypeInfo elementType) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = LIST_TYPE_NAME;
        type.typeArgs = Collections.singletonList(elementType);
        return type;
    }

    public static GenericTypeInfo mapType(GenericTypeInfo keyType, GenericTypeInfo valueType) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = MAP_TYPE_NAME;
        type.typeArgs = Arrays.asList(keyType, valueType);
        return type;
    }

    public static GenericTypeInfo parameterizedType(String typeName, List<GenericTypeInfo> typeArgs) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeName = typeName;
        type.typeArgs = Collections.unmodifiableList(typeArgs);
        return type;
    }

    public static GenericTypeInfo typeVariable(String typeVar) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.typeVar = typeVar;
        return type;
    }

    public static GenericTypeInfo unionType(Map<String, GenericTypeInfo> possibleTypes) {
        GenericTypeInfo type = new GenericTypeInfo();
        type.possibleTypes = possibleTypes;
        return type;
    }

    private static String tryPromoteToMatchType(String type1, String type2) {
        if (Objects.equals(type1, type2)) {
            return type1;
        }
        String pType1 = promoteType(type1);
        String pType2 = promoteType(type2);
        if (Objects.equals(pType1, pType2)) {
            return pType1;
        }
        if ("int".equals(pType1) && "long".equals(pType2) || "int".equals(pType2) && "long".equals(pType1)) {
            return "long";
        }
        if ("float".equals(pType1) && "double".equals(pType2) || "float".equals(pType2) && "double".equals(pType1)) {
            return "double";
        }
        return null;
    }

    private static String promoteType(String type) {
        if ("byte".equals(type)
            || "char".equals(type)
            || "short".equals(type)
            || "int".equals(type)) {
            return "int";
        }
        if ("long".equals(type)
            || "float".equals(type)
            || "double".equals(type)) {
            return type;
        }
        return null;
    }
}