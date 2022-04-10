package com.estatetrader.annotation.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为datum注入提供者定义<b>datum-imported-key</b>，与{@link ResponseInjectProvider}配对使用，
 * 用于接收来自datum注入使用者提供的参数。如果指定的{@link ImportDatumKey#keyName()}与对应API返回值中定义的某个<b>datum-key</b>一致，
 * 则这两者的类型需要一致，或前者是后者的数组或集合形式。
 * <h3>datum-imported-key的单复数形式</h3>
 * <ol>
 *     <li>单数，类型与对应的<b>datum-key</b>类型兼容，或者为非集合或数组形式（如果无对应的<b>datum-key</b>）</li>
 *     <li>复数，类型是对应的<b>datum-key</b>类型的数组或集合形式，或者本身为数组或集合类型（如果无对应的<b>datum-key</b>）</li>
 * </ol>
 * <h3>datum-imported-key规范</h3>
 * <b>datum-imported-key</b>允许使用以下三种方式声明：
 * <ol>
 *     <li>直接参数方式：在API对应函数的参数上声明，将整个参数声明为datum-imported-key</li>
 *     <li>实体方式：将API的参数声明为实体类型，然后将该实体类型的每一个字段声明为datum-imported-key</li>
 *     <li>实体集合方式：将API的参数声明为实体的数组或集合类型，然后将该实体类型的每一个字段声明为datum-imported-key</li>
 * </ol>
 * 可以为一个API声明多个<b>datum-imported-key</b>。为了避免歧义，组合key的声明有如下约束：
 * <ol>
 *     <li>API的所有参数（实体、实体集合）范围内声明的key不得重名</li>
 *     <li>如果使用实体集合方式声明，则其他参数不得再包含任何形式的<b>datum-imported-key</b></li>
 *     <li>如果使用实体集合方式声明，则该集合的实体类型的所有字段都必须定义为<b>datum-imported-key</b>，且均不得为复数形式</li>
 *     <li>如果不使用实体集合方式声明，则在API参数范围内最多允许出现一个复数形式的key</li>
 * </ol>
 * 多种声明方式的适用场景：
 * <ol>
 *     <li>仅提供单一datum：使用直接参数和实体方式声明key（单数），返回值类型为Datum的实现类</li>
 *     <li>仅包含一个复数key的多datum场景：可使用直接参数和实体方式（包含一个复数key），或者使用一个实体集合方式</li>
 *     <li>包含多个复数key的多datum场景，只能使用实体集合方式，且该实体的每个字段均使用单数模式</li>
 * </ol>
 *
 * @deprecated 已过期，请使用 {@link ImportDatumKey}
 * @see ExportDatumKey
 * @see DatumKey
 * @see ResponseInjectProvider
 */
@Deprecated
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectDatumKey {
    /**
     * 该参数或字段所对应的datum-exported-key，用于多datum-key支持
     * @return datum-exported-key名称
     */
    String keyName();

    /**
     * 该key是否为必填key，非必填的字段可以允许datum注入的使用者不提供对应的<b>datum-exported-key</b>
     * @return 是否为必填key
     */
    boolean required() default true;
}
