package com.estatetrader.define;

import java.util.List;

public class ExtensionTokenIssuerInfo {
    public final int[] allowedAppIds;
    private final List<String> allowedFields;
    public final String mainField;

    public ExtensionTokenIssuerInfo(int[] allowedAppIds, List<String> allowedFields, String mainField) {
        this.allowedAppIds = allowedAppIds;
        this.allowedFields = allowedFields;
        this.mainField = mainField;
    }

    public boolean isFieldAllowed(String field) {
        return allowedFields.contains(field);
    }

    public boolean isAppAllowed(int appId) {
        for (int n : allowedAppIds) {
            if (n == appId) {
                return true;
            }
        }
        return false;
    }
}
