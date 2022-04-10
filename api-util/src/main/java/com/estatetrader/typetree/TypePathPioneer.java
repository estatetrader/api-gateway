package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

/**
 * 用于向前推进类型树的路径
 */
public interface TypePathPioneer {
    /**
     * 以指定的类型为起点，构造一条路径路径
     * @param type 作为起点的类型
     * @return 构造得到的类型路径
     */
    TypePath start(GenericType type);

    /**
     * 从当前路径开始，确定该路径的所有后续路径
     * @param path 类型路径
     * @return 搜索得到的后续路径列表
     */
    TypePathMove next(TypePath path);

    /**
     * 从当前路径开始，跳转到指定的目标类型
     * @param path 类型路径
     * @param targetType 要跳转的目标类型
     * @return 搜索得到的后续路径列表
     */
    JumpTypeSpan jump(TypePath path, GenericType targetType);

    /**
     * 使用的类型解析器
     * @return 类型解析器
     */
    RecordTypeResolver typeResolver();
}
