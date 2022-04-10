package com.estatetrader.rule.authorizing;

import java.util.Map;

/**
 * 用于维护权限树的管理类
 */
public interface AuthorizationTreeManager {
    /**
     * 获取子系统授权信息，不包含子节点
     * @param name 子系统名称
     */
    SubsystemInfo getSubsystem(String name);

    /**
     * 创建或更新子系统授权信息
     * @param subsystemName 子系统名称
     * @param info 子系统信息
     */
    void putSubsystem(String subsystemName, SubsystemInfo info);

    /**
     * 删除子系统以及挂载在此子系统下的所有API及其角色
     * @param subsystemName 待删除的子系统名称
     */
    void deleteSubsystem(String subsystemName);

    /**
     * 获取子系统下挂载的API
     * @param subsystemName 子系统名称
     * @return 子系统权限树下挂载的所有API列表
     */
    Iterable<String> getApis(String subsystemName);

    /**
     * 获取子系统下的某个API的授权信息
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @return API的授权信息
     */
    ApiInfo getApi(String subsystemName, String apiName);

    /**
     * 获取子系统下挂载的所有API及其授权信息
     * @param subsystemName 子系统名称
     * @return API名称->授权信息表
     */
    Map<String, ApiInfo> getApiInfos(String subsystemName);

    /**
     * 创建或更新给定子系统下指定API的授权信息
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param info API授权信息
     */
    void putApiInfo(String subsystemName, String apiName, ApiInfo info);

    /**
     * 创建或更新给定子系统下一组API的授权信息（不删除表中不存在的API授权信息）
     * @param subsystemName 子系统名称
     * @param infoMap API名称->授权信息表
     */
    void putApiInfos(String subsystemName, Map<String, ApiInfo> infoMap);

    /**
     * 删除子系统下指定的API授权信息及其角色
     * @param subsystemName 子系统名称
     * @param apiName API名称
     */
    void deleteApi(String subsystemName, String apiName);

    /**
     * 删除子系统下指定的一组API授权信息及其角色
     * @param subsystemName 子系统名称
     * @param apis API名称列表
     */
    void deleteApis(String subsystemName, Iterable<String> apis);

    /**
     * 获取子系统的某个API下挂载的所有角色
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @return 挂载的角色列表
     */
    Iterable<String> getApiRoles(String subsystemName, String apiName);

    /**
     * 将角色加入到子系统的指定API授权信息中，只增加，不移除
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param role 角色
     */
    void addRoleToApi(String subsystemName, String apiName, String role);

    /**
     * 将一组角色加入到子系统的指定API授权信息中，只增加，不移除
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param roles 待加入的角色列表
     */
    void addRolesToApi(String subsystemName, String apiName, Iterable<String> roles);

    /**
     * 将一组角色设置为子系统的指定API角色组（会删除掉所有不在roles中出现的角色）
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param roles 新的角色列表
     */
    void setRolesToApi(String subsystemName, String apiName, Iterable<String> roles);

    /**
     * 从子系统的指定API中删除某个角色
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param role 角色
     */
    void removeRoleFromApi(String subsystemName, String apiName, String role);

    /**
     * 从子系统的指定API中删除一组角色
     * @param subsystemName 子系统名称
     * @param apiName API名称
     * @param roles 需要删除的角色列表
     */
    void removeRoleFromApis(String subsystemName, String apiName, Iterable<String> roles);

    /**
     * 将角色添加到指定子系统的一组API
     * @param subsystemName 子系统名称
     * @param apiNames 被赋予API列表
     * @param role 角色名
     */
    void addRoleToApis(String subsystemName, Iterable<String> apiNames, String role);

    /**
     * 将角色从指定子系统的一组API中删除
     * @param subsystemName 子系统名称
     * @param apiNames 被赋予API列表
     * @param role 角色名
     */
    void removeRoleFromApis(String subsystemName, Iterable<String> apiNames, String role);
}
