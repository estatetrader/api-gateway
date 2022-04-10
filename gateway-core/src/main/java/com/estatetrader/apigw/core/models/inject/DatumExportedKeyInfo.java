package com.estatetrader.apigw.core.models.inject;

public class DatumExportedKeyInfo {
    private final String name;
    private final boolean plural;

    public DatumExportedKeyInfo(String name, boolean plural) {
        this.name = name;
        this.plural = plural;
    }

    public String getName() {
        return name;
    }

    public boolean isPlural() {
        return plural;
    }

    public DatumExportedKeyInfo merge(DatumExportedKeyInfo another) {
        if (!name.equals(another.name)) {
            throw new IllegalArgumentException("could not merge the exposed-key-info " + name + " with " + another.name);
        }
        return plural ? this : another.plural ? another : this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DatumExportedKeyInfo)) return false;

        DatumExportedKeyInfo keyInfo = (DatumExportedKeyInfo) object;

        if (plural != keyInfo.plural) return false;
        if (!name.equals(keyInfo.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (plural ? 1 : 0);
        return result;
    }
}
