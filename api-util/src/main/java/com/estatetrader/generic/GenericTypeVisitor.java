package com.estatetrader.generic;

import java.util.List;

/**
 * the visitor to visit the components of a given generic type
 * @param <R> the type of the visit report
 */
public interface GenericTypeVisitor<R> {
    R visitType(GenericType type, List<R> childrenReports);
}
