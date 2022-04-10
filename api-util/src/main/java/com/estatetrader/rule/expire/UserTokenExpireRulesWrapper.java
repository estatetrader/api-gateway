package com.estatetrader.rule.expire;

import java.util.List;

public class UserTokenExpireRulesWrapper {
    private List<UserTokenExpireRule> rules;

    public UserTokenExpireRulesWrapper() {}

    public UserTokenExpireRulesWrapper(List<UserTokenExpireRule> rules) {
        this.rules = rules;
    }

    public List<UserTokenExpireRule> getRules() {
        return rules;
    }

    public void setRules(List<UserTokenExpireRule> rules) {
        this.rules = rules;
    }
}
