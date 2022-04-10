package com.estatetrader.generic;

/**
 * represents the collection or array type
 */
public interface CollectionLikeType extends GenericType {
    /**
     * the element type of the collection or array
     * @return element type
     */
    GenericType getElementType();
}
