//  ============================================================================
//
//  Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.api.type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataquality.semantic.classifier.SemanticCategoryEnum;
import org.talend.dataquality.semantic.statistics.SemanticType;
import org.talend.dataquality.statistics.type.DataTypeEnum;

public class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Compute the dataset metadata columns valid/invalid, empty/count values.
     *
     * @return the dataset column types in DQ libraries.
     * @param columns The Data Prep {@link ColumnMetadata columns} to convert to DQ library's types.
     */
    public static DataTypeEnum[] convert(List<ColumnMetadata> columns) {
        DataTypeEnum[] types = new DataTypeEnum[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            final String suggestedType = columns.get(i).getQuality().getMostFrequentSubType();
            final String type = suggestedType != null && !columns.get(i).isTypeForced() ? suggestedType : columns.get(i).getType();
            types[i] = convert(Type.get(type));
        }
        return types;
    }

    public static DataTypeEnum convert(Type type) {
        switch (type) {
        case ANY:
        case STRING:
            return DataTypeEnum.STRING;
        case NUMERIC:
            return DataTypeEnum.INTEGER;
        case INTEGER:
            return DataTypeEnum.INTEGER;
        case DOUBLE:
        case FLOAT:
            return DataTypeEnum.DOUBLE;
        case BOOLEAN:
            return DataTypeEnum.BOOLEAN;
        case DATE:
            return DataTypeEnum.DATE;
        default:
            return DataTypeEnum.STRING;
        }

    }

    public static DataTypeEnum[] convert(Type[] types) {
        DataTypeEnum[] converted = new DataTypeEnum[types.length];
        for (int i = 0; i < types.length; i++) {
            converted[i] = convert(types[i]);
        }
        return converted;
    }

    /**
     * @param semanticType A {@link SemanticType semantic type} as returned by the DQ's
     * {@link org.talend.dataquality.semantic.statistics.SemanticAnalyzer}.
     * @return A display name for the semantic type's suggested category or empty string if none found.
     */
    public static String getDomainLabel(SemanticType semanticType) {
        if (semanticType == null) {
            return StringUtils.EMPTY;
        } else {
            return getDomainLabel(semanticType.getSuggestedCategory());
        }
    }

    /**
     * @param categoryId A category id from supported {@link SemanticCategoryEnum categories}.
     * @return A display name for the category id or empty string if none found.
     * @see SemanticCategoryEnum
     */
    public static String getDomainLabel(String categoryId) {
        if (categoryId == null) {
            return StringUtils.EMPTY;
        } else {
            final SemanticCategoryEnum category = SemanticCategoryEnum.getCategoryById(categoryId.toUpperCase());
            return category == null ? StringUtils.EMPTY : category.getDisplayName();
        }
    }

    /**
     * Returns the type which is the subtype of the other or the first one in case of equality. If one of the specified
     * type is null then the other is returned. It returns null if there is no relation of order between the specified
     * types.
     *
     * @param t1 the first specified type
     * @param t2 the second specified type
     * @return
     */
    public static Type subTypeOfOther(final Type t1, final Type t2) {
        // Null values checks
        if (t1 == null && t2 == null) {
            return null;
        } else if (t1 == null) {
            return t2;
        } else if (t2 == null) {
            return t1;
        }
        // Build path from "deepest" type to the top type
        Function<Type, List<Type>> parentTypesBuilder = t -> {
            List<Type> path = new ArrayList<>();
            Type current = t;
            while (current != null) {
                path.add(current);
                current = current.getSuperType();
            }
            return path;
        };
        final List<Type> t1Ancestors = parentTypesBuilder.apply(t1);
        final List<Type> t2Ancestors = parentTypesBuilder.apply(t2);
        final List<Type> ancestors;
        if (t1Ancestors.size() > t2Ancestors.size()) {
            ancestors = t1Ancestors;
        } else {
            ancestors = t2Ancestors;
        }
        // Select the "deepest" type in all known types
        if (ancestors.indexOf(t1) < 0 || ancestors.indexOf(t2) < 0) {
            return null; // t1 and t2 don't share same inheritance tree.
        } else {
            // t1 and t2 share same inheritance tree, returns the most specific one (lowest in inheritance tree).
            return ancestors.indexOf(t1) > ancestors.indexOf(t2) ? t2 : t1;
        }
    }

}
