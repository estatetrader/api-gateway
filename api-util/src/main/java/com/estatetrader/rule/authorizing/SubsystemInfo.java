package com.estatetrader.rule.authorizing;

public class SubsystemInfo {
    private boolean authorizingDisabled;
    private boolean onlyTrustedNetwork;

    public boolean isAuthorizingDisabled() {
        return authorizingDisabled;
    }

    public void setAuthorizingDisabled(boolean authorizingDisabled) {
        this.authorizingDisabled = authorizingDisabled;
    }

    public boolean isOnlyTrustedNetwork() {
        return onlyTrustedNetwork;
    }

    public void setOnlyTrustedNetwork(boolean onlyTrustedNetwork) {
        this.onlyTrustedNetwork = onlyTrustedNetwork;
    }
}
