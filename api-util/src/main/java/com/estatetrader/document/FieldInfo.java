package com.estatetrader.document;

import com.estatetrader.annotation.Description;
import com.estatetrader.define.IllegalApiDefinitionException;

import java.io.Serializable;
import java.util.Objects;

@Description("接口实体字段信息")
public class FieldInfo implements Serializable {
    @Description("字段名")
    public String  name;

    @Description("字段的泛化类型")
    public GenericTypeInfo type;

    @Description("字段在生成的SDK中的名称")
    public String nameInSdk;

    @Description("注释")
    public String desc;

    @Description("标示此字段是否为客户端的字段（即由客户端代码定义，不参与json序列化和反序列化）")
    public boolean clientOnly;

    @Description("标示此字段是否已经被服务端标为已废弃（不建议客户端继续使用，并会在未来的版本中移除）")
    public boolean deprecated;

    @Description("字段的样例值")
    public String exampleValue;

    public void merge(FieldInfo f) {
        try {
            if (nameInSdk != null && f.nameInSdk != null && !Objects.equals(nameInSdk, f.nameInSdk)) {
                throw new IllegalApiDefinitionException("field info nameInSdk " + nameInSdk +
                    " is not matched with " + f.nameInSdk);
            }

            if (nameInSdk == null && f.nameInSdk != null) {
                nameInSdk = f.nameInSdk;
            }

            type = type.merge(f.type);

            if (!Objects.equals(desc, f.desc)) {
                desc += " " + f.desc;
            }

            deprecated |= f.deprecated;

            if (f.exampleValue != null) {
                exampleValue = f.exampleValue;
            }
        } catch (IllegalApiDefinitionException e) {
            throw new IllegalApiDefinitionException(e.getMessage() + " in field " + name, e);
        }
    }

    public void validate() {
        if (name == null || !name.matches("^[a-zA-Z0-9_]*$")) {
            throw new IllegalApiDefinitionException("invalid field name " + name);
        }
        type.validate();
    }
}
