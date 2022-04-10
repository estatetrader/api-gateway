package com.estatetrader.annotation.inject;

import com.estatetrader.define.Datum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     定义一个可以为API返回值注入的受体（{@link Datum}类型）提供数据的实现类，并支持该实现类的<b>datum-type</b>
 * </p><p>
 *     只有被{@link DefineDatum}标注了的实现类可以作为API返回值注入的数据源，并且该API需要在其dubbo接口上声明
 *     {@link ResponseInjectProvider}，以将该API声明为datum的提供者。
 * </p><p>
 *     作为datum的提供者，API的返回值目前可以有以下两种模式：
 *     <ol>
 *       <li>单数模式：直接将注解标注在API的返回值类型上，此模式仅可以提供一条datum数据</li>
 *       <li>复数模式：返回值类型为数组或集合类型，其元素类型标注了{@link DefineDatum}，此模式可以提供多条datum数据</li>
 *     </ol>
 * </p><p>
 *     <b>从设计的角度说，datum-type表征的是Datum提供方的特征，与返回该类型的API无关，因此该注解放置的位置为实体类</b>
 * </p><p>
 *     <b>datum-type</b>是一个描述Datum实现类的名词，例如：<i>price-info</i>, <i>spu-info</i>, <i>activity-info</i>
 * </p><p>
 *     API返回值注入的消费者应使用{@link ExportDatumKey#datumType()}和{@link InjectDatum#datumType()}声明对该datum的消费。
 *     <i>datum-type</i>将这三者绑定到了一起。
 * </p>
 * @see Datum
 * @see ResponseInjectProvider
 * @see ExportDatumKey
 * @see ImportDatumKey
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefineDatum {
    /**
     * 被注解的实体类所属的<b>datum-type</b>
     * @return datum-type，非空
     */
    String value();
}
