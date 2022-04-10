package com.estatetrader.entity;

/**
 * 用于处理半角逗号分隔的长整型数字字符串
 */
public class LongArrayStringInjector extends SimpleInjector {

    @Override
    public InjectionData parseDataFromHttpParam(String param) {
        Data data = new Data(getName());
        data.sb = new StringBuilder(param);
        return data;
    }

    @Override
    public Class<? extends Data> getDataType() {
        return Data.class;
    }

    public static class Data extends SimpleInjector.Data {
        /**
         * 当sb不为空的时候,使用它存储的值
         */
        private StringBuilder sb;

        public Data() {
        }

        public Data(String name) {
            super(name, null);
        }

        public Data(String name, String value) {
            super(name, value);
        }

        public Data(String name, long value) {
            this(name, String.valueOf(value));
        }

        @Override
        public void batchMerge(InjectionData injection) {
            if (injection.getValue() == null) return;

            if (sb == null) {
                if (getValue() == null) {
                    setValue(injection.getValue());
                    return;
                } else {
                    sb = new StringBuilder();
                    sb.append(getValue());
                }
            }
            sb.append(",").append(injection.getValue());
        }

        public void merge(long n) {
            if (sb == null) {
                if (getValue() == null) {
                    sb = new StringBuilder();
                    sb.append(n);
                    return;
                } else {
                    sb = new StringBuilder();
                    sb.append(getValue());
                }
            }
            sb.append(",").append(n);
        }

        @Override
        public Object getValue() {
            if (sb != null) {
                setValue(sb.toString());
            }
            return super.getValue();
        }

        @Override
        public void setValue(Object value) {
            super.setValue(value);
            sb = null;
        }
    }
}
