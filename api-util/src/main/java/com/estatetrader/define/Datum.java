package com.estatetrader.define;

import java.io.Serializable;

/**
 * 表示一条实体数据，在API返回值注入中，充当被注入数据的占位符，在声明Datum接收方的时候用作字段类型（或集合类型字段的元素类型）的声明类型。
 * 在声明API返回值注入的提供方时，其返回值的实体类型（或集合的元素类型）无需继承此接口
 */
public interface Datum extends Serializable {
}
