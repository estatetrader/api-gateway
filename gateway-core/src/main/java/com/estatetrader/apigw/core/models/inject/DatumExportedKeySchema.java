package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.apigw.core.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

public class DatumExportedKeySchema {
    private final boolean multiple;
    private final DatumExportedKeyInfo[] keyInfos;
    private final DatumExportedKeyInfo pluralKeyInfo;

    public DatumExportedKeySchema(boolean multiple, DatumExportedKeyInfo[] keyInfos) {
        this.multiple = multiple;
        DatumExportedKeyInfo pluralKeyInfo = null;
        for (DatumExportedKeyInfo keyInfo : keyInfos) {
            if (!keyInfo.isPlural()) continue;
            if (pluralKeyInfo != null) {
                throw new IllegalArgumentException("最多仅能定义一个需要导出的datum-key：" + pluralKeyInfo.getName()
                    + "+" + keyInfo.getName());
            }
            pluralKeyInfo = keyInfo;
        }
        this.keyInfos = keyInfos;
        this.pluralKeyInfo = pluralKeyInfo;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public DatumExportedKeyInfo getPluralKey() {
        return pluralKeyInfo;
    }

    public DatumExportedKeySchema asMultiple() {
       return multiple ? this : new DatumExportedKeySchema(true, keyInfos);
    }

    public DatumExportedKeyInfo keyInfoOf(String name) {
        for (DatumExportedKeyInfo keyInfo : keyInfos) {
            if (keyInfo.getName().equals(name)) {
                return keyInfo;
            }
        }
        return null;
    }

    public static DatumExportedKeySchema merge(DatumExportedKeySchema spec1, DatumExportedKeySchema spec2, boolean asMultiple) {
        if (spec1 == null) {
            return spec2;
        }
        if (spec2 == null) {
            return spec1;
        }
        DatumExportedKeyInfo[] newKeyInfos = ArrayUtils.mergeElement(spec1.keyInfos, spec2.keyInfos,
            (x, y) -> Objects.equals(x.getName(), y.getName()), DatumExportedKeyInfo::merge);
        if (newKeyInfos == spec1.keyInfos) {
            return asMultiple || spec2.multiple ? spec1.asMultiple() : spec1;
        } else if (newKeyInfos == spec2.keyInfos) {
            return asMultiple || spec1.multiple ? spec2.asMultiple() : spec2;
        } else {
            return new DatumExportedKeySchema(asMultiple || spec1.multiple || spec2.multiple, newKeyInfos);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DatumExportedKeySchema)) return false;

        DatumExportedKeySchema keySchema = (DatumExportedKeySchema) object;

        if (multiple != keySchema.multiple) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(keyInfos, keySchema.keyInfos)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (multiple ? 1 : 0);
        result = 31 * result + Arrays.hashCode(keyInfos);
        return result;
    }
}
