package com.estatetrader.generic;

/**
 * represent the key-value relationship, so-called Map type in Java world
 */
public interface MapType extends StaticType {
    /**
     * get the key type of the map
     * @return the generic presentation of the key type
     */
    GenericType getKeyType();

    /**
     * get the value type of the map
     * @return the generic presentation of the value type
     */
    GenericType getValueType();
}
