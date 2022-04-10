package com.estatetrader.entity;

import com.estatetrader.define.ServiceInjectable;

/**
 * 用于处理半角逗号分隔的长整型数字字符串
 */
public class SimpleInjector implements ServiceInjectable {

    private String name;

    /**
     * 该参数的逻辑名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 设置参数的逻辑名称
     *
     * @param name 参数逻辑名称
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public InjectionData parseDataFromHttpParam(String param) {
        Data data = new Data();
        data.value = param;
        return data;
    }

    @Override
    public Class<? extends Data> getDataType() {
        return Data.class;
    }

    public static class Data implements InjectionData {
        /**
         * 从封装类型传递过来的名称
         */
        private String name;

        /**
         * 初始化的值数据存储在该字段中
         */
        private Object value;

        public Data() {}

        public Data(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 将同逻辑名的数据合并起来
         *
         * @param injection
         */
        @Override
        public void batchMerge(InjectionData injection) {
            if (injection.getValue() == null) return;
            value = injection.getValue();
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }
    }
}
