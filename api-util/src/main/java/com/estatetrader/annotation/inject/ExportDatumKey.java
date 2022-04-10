package com.estatetrader.annotation.inject;

import com.estatetrader.annotation.ResponseInjectFromApi;
import com.estatetrader.define.Datum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将某一API的返回值实体（或嵌套实体）的字段声明为<b>datum-exported-key</b>，用于向datum注入的提供者提供参数，
 * 同时使用{@link InjectDatum}将同一实体的另一字段声明为<b>datum-acceptor</b>。
 * <br/>
 * 注意，{@link ExportDatumKey}与{@link InjectDatum}应使用相同的datum-type，以便将声明为一个特定的<b>injection-bundle</b>。
 * <br/>
 * 允许在同一实体中声明多个{@link ExportDatumKey}字段，这些字段通过该注解的{@link ExportDatumKey#datumType()}进行关联。
 * <br/>
 * 在同一实体中使用相同<b>datum-type</b>的多个{@link ExportDatumKey}与{@link InjectDatum}构成了一个<b>injection-bundle</b>。
 * 一个实体可以包含多个<b>injection-bundle</b>，多个实体也可以包含同一个<b>injection-bundle</b>。
 * <br/>
 * 在一次API返回值注入中，<b>datum-exported-key</b>用于提供被调用API的参数，<b>datum-acceptor</b>用于接收被调用API的返回值。
 * <br/>
 * {@link ExportDatumKey#datumType()}指定的<b>datum-type</b>需要使用{@link ResponseInjectFromApi}注解进行选择，
 * <b>ResponseInjectFromApi</b>用于选择一个能够提供特定<b>datum-type</b>的API作为datum提供者，以方便其返回值声明注入。
 * <br/>
 * {@link ExportDatumKey#keyName()}用于指定当前字段与{@link ImportDatumKey}和{@link DatumKey}的映射关系。
 * 指定的<b>keyName</b>必须在同<b>datum-type</b>的{@link DatumKey}或者{@link ImportDatumKey}的定义。
 * 且类型需要和与其对应的<b>datum-key</b>或<b>datum-imported-key</b>兼容（或是其数组或集合的类型）。
 *
 * <h3>datum-exported-key的单复数</h3>
 * 定义如下：
 * <ol>
 *     <li>如果{@link ExportDatumKey}所声明的字段类型和与其对应的<b>datum-key</b>或<b>datum-imported-key</b>兼容
 *     （此时不存在对应的<b>datum-key</b>），则称该<b>datum-exported-key</b>为单数形式</li>
 *     <li>否则，如果是其对应key的数组或集合类型，则称该<b>datum-exported-key</b>为复数形式</li>
 * </ol>
 * 例如：
 * <table>
 *     <tr><th>datum-exported-key</th><th>datum-key</th></tr>
 *     <tr><td>单数形式：int；复数形式：int[]</td><td>int</td></tr>
 *     <tr><td>单数形式：String；复数形式：List&lt;String&gt;</td><td>String</td></tr>
 * </table>
 *
 * @see InjectDatum
 * @see ImportDatumKey
 * @see DefineDatum
 * @see Datum
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ExportDatumKeyRepeated.class)
public @interface ExportDatumKey {
    /**
     * datum-type，表示datum注入的类型，用于将API返回值注入中的多个组分关联到一起，从而提供多API返回值注入的支持
     * @return datum-type，不得为空
     */
    String datumType();

    /**
     * datum-key的名称，用于区分在同一datum-type中的多个参数，从而支持多参数注入的支持
     * @return datum-key名称，不得为空
     */
    String keyName();
}
