package com.estatetrader.document;

import java.io.Serializable;
import java.util.*;

import com.estatetrader.annotation.Description;
import com.estatetrader.define.IllegalApiDefinitionException;
import com.estatetrader.util.Lambda;

/**
 * 接口文档
 */
@Description("接口文档")
public class ApiDocument implements Serializable {
    /**
     * 应用接口信息
     */
    @Description("应用接口信息")
    public List<MethodInfo> apis;

    /**
     * 通用异常信息
     */
    @Description("通用异常信息")
    public List<CodeInfo> codes;

    /**
     * 接口参数/返回值类型结构描述
     */
    @Description("接口参数/返回值类型结构描述")
    public List<TypeStruct> structures;

    /**
     * 通用参数列表描述
     */
    @Description("通用参数列表描述")
    public List<CommonParameterInfo> commonParams;

    public void merge(ApiDocument doc) {
        mergeApiList(doc);
        mergeCodeList(doc);
        mergeStructList(doc);
        mergeSystemParameterInfoList(doc);
        // 在node清理完结构体冲突之后取消以下注释
//        mergeAllStructures();
    }

    // call this method if doc is changed
    public void mergeAllStructures() {
        if (structures == null) {
            return;
        }
        Map<String, TypeStruct> map = new HashMap<>();

        // 将同名结构体合并到一个结构体中，并更新list使其使用同一个合并之后的结构体
        for (int i = 0; i < structures.size(); i++) {
            TypeStruct struct = structures.get(i);
            TypeStruct old = map.get(struct.name);
            if (old == null) {
                map.put(struct.name, struct);
            } else if (old != struct) {
                // 合并结构体
                old.merge(struct);
                structures.set(i, old);
            }
        }
    }

    @SuppressWarnings("unused")
    public void checkForDuplicatedStructCaseInsensitive() {
        // 排查文档中的类型重名情况，不区分大小写
        Map<String, List<TypeStruct>> map = new TreeMap<>(String::compareToIgnoreCase);
        if (structures != null) {
            for (TypeStruct t : structures) {
                map.compute(t.name, (key, list) -> {
                    if (list == null) {
                        return new ArrayList<>(Collections.singletonList(t));
                    }

                    list.add(t);
                    return list;
                });
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<TypeStruct>> entry : map.entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }

            // 只关注大小写不一致的结构体
            if (Lambda.all(entry.getValue(), p -> entry.getKey().equals(p.name))) {
                continue;
            }

            sb.append(entry.getKey()).append(": ");
            for (TypeStruct struct : entry.getValue()) {
                sb.append(struct.name);
                sb.append(", ");
            }

            // 删掉最后一个多余的逗号和空格
            sb.delete(sb.length() - 2, sb.length());
            sb.append('\n');
        }

        if (sb.length() == 0) {
            return;
        }

        // 删掉最后一个多余的换行符
        sb.delete(sb.length() - 1, sb.length());

        sb.insert(0, "Duplicated structures with same name(case-insensitive) are found: \n");
        throw new IllegalApiDefinitionException(sb.toString());
    }

    public StructFinder createStructFinder() {
        return new StructFinder(structures == null ? Collections.emptyList() : structures);
    }

    public TypeStruct findTypeStruct(String structName) {
        if (structures != null) {
            for (TypeStruct t : structures) {
                if (structName.equals(t.name)) {
                    return t;
                }
            }
        }
        return null;
    }

    public void addTypeStruct(TypeStruct struct) {
        struct.validate();

        if (structures == null) {
            structures = new ArrayList<>();
            structures.add(struct);
            return;
        }

        for (TypeStruct t : structures) {
            if (struct.name.equals(t.name)) {
                t.merge(struct);
                return;
            }
        }

        structures.add(struct);
    }

    public MethodInfo findMethod(String methodName) {
        if (apis == null) {
            return null;
        }
        for (MethodInfo method : apis) {
            if (methodName.equals(method.methodName)) {
                return method;
            }
        }
        return null;
    }

    private void mergeSystemParameterInfoList(ApiDocument doc) {
        if (commonParams == null) {
            if (doc.commonParams == null) {
                commonParams = new ArrayList<>();
            } else {
                commonParams = new ArrayList<>(doc.commonParams);
            }
        } else if (doc.commonParams != null) {
            for (CommonParameterInfo t : doc.commonParams) {
                if (Lambda.any(commonParams, x -> x.name.equals(t.name))) {
                    throw new IllegalApiDefinitionException("duplicated system parameter info found: " + t.name);
                } else {
                    commonParams.add(t);
                }
            }
        }
    }

    private void mergeStructList(ApiDocument doc) {
        structures = mergeStructList(structures, doc.structures);
    }

    private List<TypeStruct> mergeStructList(List<TypeStruct> thisList, List<TypeStruct> thatList) {
        if (thisList == null) {
            if (thatList == null) {
                return null;
            } else {
                return new ArrayList<>(thatList);
            }
        } else if (thatList != null) {
            for (TypeStruct t : thatList) {
                TypeStruct s = Lambda.find(thisList, x -> x.name.equals(t.name));
                if (s == null) {
                    thisList.add(t);
                } else {
                    s.merge(t);
                }
            }
            return thisList;
        } else {
            return thisList;
        }
    }

    private void mergeCodeList(ApiDocument doc) {
        if (codes == null) {
            if (doc.codes == null) {
                codes = new ArrayList<>();
            } else {
                codes = new ArrayList<>(doc.codes);
            }
        } else if (doc.codes != null) {
            for (CodeInfo info : doc.codes) {
                CodeInfo old = Lambda.find(codes, x -> x.code == info.code);
                if (old == null) {
                    codes.add(info);
                } else if (!Objects.equals(old.name, info.name)) {
                    throw new IllegalApiDefinitionException("error code " + info.code +
                        " is defined both by name " + info.name + " and " + old.name);
                }
            }
        }
    }

    private void mergeApiList(ApiDocument doc) {
        if (apis == null) {
            if (doc.apis == null) {
                apis = new ArrayList<>();
            } else {
                apis = new ArrayList<>(doc.apis);
            }
        } else if (doc.apis != null) {
            for (MethodInfo info : doc.apis) {
                if (Lambda.any(apis, x -> x.methodName.equals(info.methodName))) {
                    throw new IllegalApiDefinitionException("duplicated method info found: " + info.methodName);
                } else {
                    apis.add(info);
                }
            }
        }
    }
}
