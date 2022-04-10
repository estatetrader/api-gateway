package com.estatetrader.rule.expire;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于管理user token的强制过期规则
 */
public interface ExpiredUserTokenManager {
    /**
     * 获取针对所有用户的强制过期规则
     * @return 过期规则列表
     */
    List<UserTokenExpireRule> getRulesForAllUsers();

    /**
     * 设置针对所有用户的强制过期规则
     *
     * 注意：这个函数的影响面非常大，请再三确认你的参数
     *
     * 这个规则主要用于强制下线有问题的token
     *
     * @param rules 需要设置给所有用户的强制过期规则列表
     * @param ttl 过期规则有效期，以毫秒为单位
     */
    void setRulesForAllUsers(List<UserTokenExpireRule> rules, long ttl);

    /**
     * 删除针对所有用户的强制过期规则
     */
    void deleteRulesForAllUsers();

    /**
     * 获取针对特定用户的强制过期规则列表
     * @param userId 用户编号
     * @return 强制过期规则列表
     */
    List<UserTokenExpireRule> getRulesForUser(long userId);

    /**
     * 为指定用设置token强制过期规则（覆盖已有规则）
     * @param userId 用户编号
     * @param rules 规则列表
     * @param ttl 过期规则有效期，以毫秒为单位
     */
    void setRulesForUser(long userId, List<UserTokenExpireRule> rules, long ttl);

    /**
     * 为指定的用户添加新的token强制过期规则（不覆盖）
     * @param userId 用户编号
     * @param rule 要添加的规则
     * @param ttl 过期规则有效期，以毫秒为单位
     */
    default void appendRuleForUser(long userId, UserTokenExpireRule rule, long ttl) {
        List<UserTokenExpireRule> rules = getRulesForUser(userId);
        List<UserTokenExpireRule> newRules;
        if (rules == null) {
            newRules = new ArrayList<>();
        } else {
            newRules = new ArrayList<>(rules);
        }
        newRules.add(rule);
        setRulesForUser(userId, newRules, ttl);
    }

    /**
     * 删除指定用户的所有token强制过期规则
     * @param userId 用户编号
     */
    void deleteRulesForUser(long userId);
}
