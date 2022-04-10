package com.estatetrader.annotation.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义在被{@link DefineDatum}注解了的类的字段上，用于将其声明为<b>datum-key</b>。
 * <br/>
 * 一个datum的实现类可以定义多个<b>datum-key</b>，类似于关系型数据库中的主键，这些<b>datum-key</b>
 * 作为一个整体为一个datum对象提供了唯一标示。该唯一标示主要用于在API注入的datum绑定过程中将其绑定到特定的<b>datum-acceptor</b>。
 * <br/>
 * {@link DatumKey}定义了<b>datum-key</b>的基本形式，被该注解标注的字段的类型作为该<b>datum-key</b>标准类型。
 * 由{@link ExportDatumKey}定义的<b>datum-exported-key</b>和由{@link ImportDatumKey}<b>datum-imported-key</b>
 * 可以引用同<b>datum-type</b>下的<b>datum-key</b>。通过将<b>datum-exported-key</b>和<b>datum-imported-key</b>的声明类型与
 * <b>datum-key</b>相匹配，可以得出这两种key相对于<b>datum-key</b>的单复数形式。
 * @see ExportDatumKey
 * @see ImportDatumKey
 * @see DefineDatum
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DatumKey {
    /**
     * datum-key的名称，用于支持多datum-key
     * @return datum-key的名称，默认值为被修饰的字段的名称
     */
    String value() default "";
}
