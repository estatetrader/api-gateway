package com.estatetrader.annotation.inject;

import com.estatetrader.define.Datum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将某一API的返回值实体（或嵌套实体）的字段声明为<b>datum-exported-key</b>，
 * 同时使用{@link InjectDatum}将同一实体的另一字段声明为<b>datum-acceptor</b>。
 * <br/>
 * 注意，{@link ExportDatumKey}与{@link InjectDatum}应使用相同的datum-type，以便将声明为一个特定的<b>injection-bundle</b>。
 * <br/>
 * 允许在同一实体中声明多个{@link ExportDatumKey}字段，这些字段通过该注解的{@link ExportDatumKey#datumType()}进行关联。
 * <br/>
 * 在同一实体中使用相同<b>datum-type</b>的多个{@link ExportDatumKey}与{@link InjectDatum}构成了一个<b>injection-bundle</b>。
 * 一个实体可以包含多个<b>injection-bundle</b>，多个实体也可以包含同一个<b>injection-bundle</b>。
 *
 * 在一次API返回值注入中，<b>datum-exported-key</b>用于提供被调用API的参数，<b>datum-acceptor</b>用于接收被调用API的返回值
 *
 * @see ExportDatumKey
 * @see InjectDatum
 * @see ImportDatumKey
 * @see DefineDatum
 * @see Datum
 *
 * @deprecated 已废弃，请使用{@link ExportDatumKey}（功能相同，可直接替换）
 */
@Deprecated
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExposeDatumKey {
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
