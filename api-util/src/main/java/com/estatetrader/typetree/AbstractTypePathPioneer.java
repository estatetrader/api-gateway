package com.estatetrader.typetree;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTypePathPioneer implements TypePathPioneer {

    protected final RecordTypeResolver typeResolver;

    public AbstractTypePathPioneer(RecordTypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    /**
     * 以指定的类型为起点，构造一条路径路径
     *
     * @param type 作为起点的类型
     * @return 构造得到的类型路径
     */
    @Override
    public TypePath start(GenericType type) {
        return new TypePath(rootType(type), null);
    }

    /**
     * 从当前路径开始，确定该路径的所有后续路径
     *
     * @param path 类型路径
     * @return 搜索得到的后续路径列表
     */
    @Override
    public TypePathMove next(TypePath path) {
        GenericType type = path.endType();
        if (type instanceof ArrayType) {
            return new ArrayTypePathMoveImpl(arrayType(path));
        } else if (type instanceof CollectionType) {
            return new CollectionTypePathMoveImpl(collectionType(path));
        } else if (type instanceof MapType) {
            return new MapTypeMoveImpl(mapType(path));
        } else if (type instanceof UnionType) {
            return new UnionTypeMoveImpl(possibleTypes(path));
        } else if (type instanceof StaticType) {
            StaticType staticType = (StaticType) path.endType();
            if (typeResolver.isRecordType(staticType)) {
                return new RecordTypeMoveImpl(staticType, recordFields(path));
            } else {
                return new TypePathEndMoveImpl();
            }
        } else {
            return new TypePathEndMoveImpl();
        }
    }

    /**
     * 从当前路径开始，跳转到指定的目标类型
     *
     * @param path       类型路径
     * @param targetType 要跳转的目标类型
     * @return 搜索得到的后续路径列表
     */
    @Override
    public JumpTypeSpan jump(TypePath path, GenericType targetType) {
        return jumpType(path, targetType);
    }

    /**
     * 使用的类型解析器
     *
     * @return 类型解析器
     */
    @Override
    public RecordTypeResolver typeResolver() {
        return typeResolver;
    }

    protected List<PossibleSpan> possibleTypes(TypePath path) {
        UnionType unionType = (UnionType) path.endType();
        StaticType[] possibleTypes = unionType.getPossibleTypes();
        List<PossibleSpan> spans = new ArrayList<>(possibleTypes.length);
        for (StaticType possibleType : possibleTypes) {
            PossibleSpan span = possibleType(possibleType, path);
            if (span != null) {
                spans.add(span);
            }
        }
        return spans;
    }

    protected List<FieldSpan> recordFields(TypePath path) {
        StaticType staticType = (StaticType) path.endType();
        GenericField[] fields = typeResolver.recordFields(staticType);
        List<FieldSpan> spans = new ArrayList<>(fields.length);
        for (GenericField field : fields) {
            FieldSpan span = recordField(field, path);
            if (span != null) {
                spans.add(span);
            }
        }
        return spans;
    }

    protected RootSpan rootType(GenericType type) {
        return new RootSpan(type, null);
    }

    protected ArraySpan arrayType(TypePath path) {
        return new ArraySpan((ArrayType) path.endType(), null);
    }

    protected JumpTypeSpan jumpType(TypePath path, GenericType targetType) {
        return new JumpTypeSpan(targetType, null);
    }

    protected CollectionSpan collectionType(TypePath path) {
        return new CollectionSpan((CollectionType) path.endType(), null);
    }

    protected MapSpan mapType(TypePath path) {
        return new MapSpan((MapType) path.endType(), null);
    }

    protected PossibleSpan possibleType(StaticType possibleType, TypePath path) {
        return new PossibleSpan((UnionType)path.endType(), possibleType, null);
    }

    protected FieldSpan recordField(GenericField field, TypePath path) {
        return new FieldSpan((StaticType) path.endType(), field, null);
    }

    protected static class ArrayTypePathMoveImpl implements ArrayTypePathMove {
        private final ArraySpan span;

        public ArrayTypePathMoveImpl(ArraySpan span) {
            this.span = span;
        }

        @Override
        public ArraySpan next() {
            return span;
        }
    }

    protected static class CollectionTypePathMoveImpl implements CollectionTypePathMove {
        private final CollectionSpan span;

        public CollectionTypePathMoveImpl(CollectionSpan span) {
            this.span = span;
        }

        @Override
        public CollectionSpan next() {
            return span;
        }
    }

    protected static class MapTypeMoveImpl implements MapTypeMove {
        private final MapSpan span;

        public MapTypeMoveImpl(MapSpan span) {
            this.span = span;
        }

        @Override
        public MapType mapType() {
            return span.getMapType();
        }

        @Override
        public MapSpan next() {
            return span;
        }
    }

    protected static class TypePathEndMoveImpl implements TypePathEndMove {
        @Override
        public List<TypeSpan> next() {
            return Collections.emptyList();
        }
    }

    protected static class UnionTypeMoveImpl implements UnionTypeMove {
        private final List<PossibleSpan> possibleSpans;

        public UnionTypeMoveImpl(List<PossibleSpan> possibleSpans) {
            this.possibleSpans = possibleSpans;
        }

        @Override
        public List<? extends TypeSpan> next() {
            return possibleSpans;
        }

        @Override
        public List<PossibleSpan> possibleSpans() {
            return possibleSpans;
        }
    }

    protected static class RecordTypeMoveImpl implements RecordTypeMove {
        private final StaticType recordType;
        private final List<FieldSpan> fieldSpans;

        public RecordTypeMoveImpl(StaticType recordType, List<FieldSpan> fieldSpans) {
            this.recordType = recordType;
            this.fieldSpans = fieldSpans;
        }

        @Override
        public List<? extends TypeSpan> next() {
            return fieldSpans;
        }

        @Override
        public StaticType recordType() {
            return recordType;
        }

        @Override
        public List<FieldSpan> fieldSpans() {
            return fieldSpans;
        }
    }
}
