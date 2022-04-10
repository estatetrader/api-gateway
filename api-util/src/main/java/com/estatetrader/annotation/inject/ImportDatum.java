package com.estatetrader.annotation.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义datum注入过程中的datum受体<b>datum-acceptor</b>。
 * 使用{@link InjectDatum#datumType()}将其与相同实体类型中的其他<b>datum-acceptor</b>和<b>datum-exported-key</b>关联到一起，
 * 构成一个所谓的<b>injection-bundle</b>。
 * <br/>
 * 允许在一个实体中声明多个{@link InjectDatum}，它们的<b>datum-type</b>可以相同也可以不同，相同<b>datum-type</b>的注入过程互不干扰。
 * <br/>
 * 在调用了对应的API后，网关会根据每个<b>injection-bundle</b>中的<b>datum-exported-key</b>对收到的datum对象进行匹配，仅将匹配的datum对象
 * 注入到对应的datum受体中。
 * <br/>
 * 注意，每个实体类型中一旦定义了<b>datum-exported-key</b>，则必须定义<b>datum-acceptor</b>，反之无限制。
 *
 * @see ExportDatumKey
 * @see ImportDatumKey
 * @see DefineDatum
 *
 * @deprecated 已废弃，请使用{@link InjectDatum}
 */
@Deprecated
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportDatum {
    /**
     * 该datum受体所属的<b>datum-type</b>，用于支持多datum注入源
     * @return datum-type，非空
     */
    String datumType();
}
