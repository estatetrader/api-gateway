package com.estatetrader.rule.authorizing;

import java.io.Closeable;

public interface AuthorizationTreeListener extends Closeable {
    /**
     * 获取子系统授权信息，不包含子节点
     * @param name 子系统名称
     */
    SubsystemInfo getSubsystem(String name);

    /**
     * 获取子系统下的某个API的授权信息
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @return API的授权信息
     */
    ApiInfo getApi(String subsystemName, String apiName);

    /**
     * 检查子系统的某个API是否包含给定的角色
     * 注意：此函数仅用于权限树的结构判断，不用于业务逻辑，实际的校验逻辑需要依赖子系统和API信息
     *
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param role 角色
     * @return 返回是否包含该角色
     */
    boolean containsApiRole(String subsystemName, String apiName, String role);

    /**
     * 获取子系统的某个API下挂载的所有角色
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @return 挂载的角色列表
     */
    Iterable<String> getApiRoles(String subsystemName, String apiName);
}
