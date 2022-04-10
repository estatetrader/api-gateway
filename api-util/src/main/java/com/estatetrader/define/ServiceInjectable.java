package com.estatetrader.define;

import com.estatetrader.dubboext.DubboExtProperty;
import com.estatetrader.entity.IDInjector;
import com.estatetrader.entity.IDListInjector;
import com.estatetrader.entity.SimpleInjector;

/**
 * 处理接口间将A接口的返回结果作为B接口某个入参的执行器
 * 请保证实现类有public的无参构造函数
 */
public interface ServiceInjectable {
    /**
     * 该参数的逻辑名称, 例如 product.productids
     */
    String getName();

    /**
     * 设置参数的逻辑名称
     */
    void setName(String name);

    /**
     * 将请求数据封装为 InjectionData
     */
    InjectionData parseDataFromHttpParam(String param);

    /**
     * 返回其对应的数据类型
     */
    Class<? extends InjectionData> getDataType();

    /**
     * 执行器的相关数据类型, 该类型的数据对象被序列化成json后
     * 用于在dubbo服务之间相互传递隐式参数(使用notification)
     * 请保证实现类有public的无参构造函数
     */
    interface InjectionData {
        /**
         * 将同逻辑名的数据合并起来
         */
        void batchMerge(InjectionData injection);

        Object getValue();

        void setValue(Object value);

        String getName();

        void setName(String name);
    }

    static void export(String name, Object value) {
        DubboExtProperty.exportServiceData(new SimpleInjector.Data(name, value));
    }

    static void exportID(long id) {
        DubboExtProperty.exportServiceData(new IDInjector.Data(IDInjector.DEFAULT_NAME, id));
    }

    static void exportID(String name, long id) {
        DubboExtProperty.exportServiceData(new IDInjector.Data(name, id));
    }

    static void exportIDs(long[] ids) {
        DubboExtProperty.exportServiceData(new IDInjector.Data(IDListInjector.DEFAULT_NAME, ids));
    }

    static void exportIDs(String name, long[] ids) {
        DubboExtProperty.exportServiceData(new IDInjector.Data(name, ids));
    }
}
