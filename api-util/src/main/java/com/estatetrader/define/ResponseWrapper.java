package com.estatetrader.define;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.estatetrader.generic.*;
import com.estatetrader.responseEntity.*;
import com.estatetrader.util.RawString;
import com.estatetrader.generic.*;
import com.estatetrader.responseEntity.*;

/**
 * Created by nick on 2018/7/26.
 */
public interface ResponseWrapper {
    GenericType responseType();
    Object wrap(Object obj);

    static ResponseWrapper getResponseWrapper(GenericType type) {
        ResponseWrapperImpl predefined = ResponseWrapperImpl.PREDEFINED_WRAPPERS.get(type);
        if (predefined != null) {
            return predefined;
        }

        // 将数组或集合类型包装为实体类型，用于降低后续API返回值类型变更时的兼容成本
        if (type instanceof CollectionLikeType) {
            GenericType elementType = ((CollectionLikeType) type).getElementType();
            if (elementType.equals(String.class)) {
                Function<?, StringArrayResp> convert;
                if (type instanceof ArrayType) {
                    convert = (Function<String[], StringArrayResp>) StringArrayResp::convert;
                } else {
                    convert = (Function<Collection<String>, StringArrayResp>) StringArrayResp::convert;
                }
                return ResponseWrapperImpl.of(StringArrayResp.class, convert);
            } else if (elementType instanceof ClassType && ((ClassType) elementType).isEnum()) {
                Function<?, EnumArrayResp<?>> convert;
                if (type instanceof ArrayType) {
                    convert = (Function<Object[], EnumArrayResp<?>>) EnumArrayResp::convert;
                } else {
                    convert = (Function<Collection<Object>, EnumArrayResp<?>>) EnumArrayResp::convert;
                }
                GenericType respType = GenericTypes.parameterizedType(EnumArrayResp.class, null, elementType);
                return ResponseWrapperImpl.of(respType, convert);
            } else {
                Function<?, ObjectArrayResp<?>> convert;
                if (type instanceof ArrayType) {
                    convert = (Function<Object[], ObjectArrayResp<?>>) ObjectArrayResp::convert;
                } else {
                    convert = (Function<Collection<Object>, ObjectArrayResp<?>>) ObjectArrayResp::convert;
                }
                GenericType respType = GenericTypes.parameterizedType(ObjectArrayResp.class, null, elementType);
                return ResponseWrapperImpl.of(respType, convert);
            }
        } else {
            return ResponseWrapperImpl.of(type, Function.identity());
        }
    }

    class ResponseWrapperImpl implements ResponseWrapper {
        private final GenericType responseType;
        private final Function<Object, Object> wrap;

        public ResponseWrapperImpl(GenericType responseType, Function<Object, Object> wrap) {
            this.responseType = responseType;
            this.wrap = wrap;
        }

        @Override
        public Object wrap(Object obj) {
            return wrap.apply(obj);
        }

        @Override
        public GenericType responseType() {
            return responseType;
        }

        public static ResponseWrapperImpl of(GenericType responseType, Function<?, ?> converter) {
            //noinspection unchecked
            return new ResponseWrapperImpl(responseType, (Function<Object, Object>)converter);
        }

        public static <R> ResponseWrapperImpl of(Class<R> responseType, Function<?, R> converter) {
            //noinspection unchecked
            return new ResponseWrapperImpl(GenericTypes.of(responseType), (Function<Object, Object>) converter);
        }

        private static final Map<GenericType, ResponseWrapperImpl> PREDEFINED_WRAPPERS = new Supplier<Map<GenericType, ResponseWrapperImpl>>() {

            private final Map<GenericType, ResponseWrapperImpl> map = new HashMap<>();

            private <T, R> void put(Class<T> returnType, Class<R> responseType, Function<T, R> wrap) {
                //noinspection unchecked
                ResponseWrapperImpl r = new ResponseWrapperImpl(
                    GenericTypes.of(responseType),
                    (Function<Object, Object>) wrap
                );
                map.put(GenericTypes.of(returnType), r);
            }

            @Override
            public Map<GenericType, ResponseWrapperImpl> get() {
                put(boolean.class, BoolResp.class, BoolResp::convert);
                put(boolean[].class, BoolArrayResp.class, BoolArrayResp::convert);
                put(byte.class, NumberResp.class, NumberResp::convert);
                put(byte[].class, NumberArrayResp.class, NumberArrayResp::convert);
                put(short.class, NumberResp.class, NumberResp::convert);
                put(short[].class, NumberArrayResp.class, NumberArrayResp::convert);
                put(char.class, NumberResp.class, NumberResp::convert);
                put(char[].class, NumberArrayResp.class, NumberArrayResp::convert);
                put(int.class, NumberResp.class, NumberResp::convert);
                put(int[].class, NumberArrayResp.class, NumberArrayResp::convert);
                put(long.class, LongResp.class, LongResp::convert);
                put(long[].class, LongArrayResp.class, LongArrayResp::convert);

                put(String.class, StringResp.class, StringResp::convert);
                put(String[].class, StringArrayResp.class, StringArrayResp::convert);

                put(RawString.class, RawString.class, Function.identity());
                put(Void.class, VoidResp.class, VoidResp::convert);
                put(void.class, VoidResp.class, VoidResp::convert);

                return map;
            }
        }.get();
    }
}
