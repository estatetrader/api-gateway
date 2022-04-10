package com.estatetrader.rule.definition;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ApiInfo implements Serializable {
    public String methodName;
    public String groupName;
    public String subsystem;
    public boolean authorizing;
    public List<ApiParamInfo> parameters;

    public ApiInfo() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, groupName, subsystem, authorizing, parameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ApiInfo)) {
            return false;
        }
        ApiInfo that = (ApiInfo) obj;
        return Objects.equals(this.methodName, that.methodName) &&
            Objects.equals(this.groupName, that.groupName) &&
            Objects.equals(this.subsystem, that.subsystem) &&
            this.authorizing == that.authorizing &&
            Objects.equals(this.parameters, that.parameters);
    }
}
