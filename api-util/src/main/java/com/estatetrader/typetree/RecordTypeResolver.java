package com.estatetrader.typetree;

import com.estatetrader.generic.GenericField;
import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.StaticType;

/**
 * used to resolve the types related with the record type.
 * a record is an object type which have well-defined fields which used to represents strong-typed data info
 */
public interface RecordTypeResolver {
    /**
     * 获取传入的泛型类型的所有可能运行时类型
     * @param type 要检测的类型
     * @return 返回一个包含了所有运行时可能出现的类型的一个表达
     */
    GenericType concreteType(GenericType type);

    /**
     * 判断指定的类型是否为记录类型，记录类型指的是除简单类型、数组、集合、哈希表、注解、接口以及少数特殊的保留类型外的所有具体类类型
     * @param type 要检测的类型
     * @return 返回true表示指定的type是记录类型
     */
    boolean isRecordType(StaticType type);

    /**
     * 返回指定的记录类型的所有字段
     * @param type 需要获取字段的类型
     * @return 该记录类型的所有实例字段
     * @throws IllegalArgumentException 在传入的类型不为记录类型时报错
     */
    GenericField[] recordFields(StaticType type);
}
