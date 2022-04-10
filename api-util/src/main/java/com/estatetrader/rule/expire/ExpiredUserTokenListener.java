package com.estatetrader.rule.expire;

import java.io.Closeable;
import java.util.List;

/**
 * 用于监听user token的强制过期规则变化
 */
public interface ExpiredUserTokenListener extends Closeable {
    /**
     * 查询针对所有用户的强制过期规则
     * @return 过期规则列表
     */
    List<UserTokenExpireRule> getRulesForAllUsers();

    /**
     * 查询针对特定用户的强制过期规则列表
     * @param userId 用户编号
     * @return 强制过期规则列表
     */
    List<UserTokenExpireRule> getRulesForUser(long userId);
}
