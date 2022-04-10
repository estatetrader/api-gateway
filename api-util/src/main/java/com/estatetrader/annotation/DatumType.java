package com.estatetrader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify which datum implementation to use.
 *
 * API-Gateway will determine the actual type of a field if it is declared by type Datum, for the document & SDK
 * generation purples.
 *
 * And also API-Gateway will do its best to inter that type, but if you inject more than more APIs and you declare
 * more than one fields as Datum type, you are required to use @DatumType to distinguish these two fields.
 * The value of @DatumType will be specified by a name defined as the value of a @DefineDatum annotation which is used
 * to annotate the response of the API you are injecting to your API and the Datum typed field is intended as the real
 * storage field. If such a @DefineDatum is not match, gateway will try to find the datum type by matching the name with
 * the provider name of the api, from which the datum type is defined
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface DatumType {
    /**
     * The name of the @DefineDatum you want to refer
     */
    String value();
}
