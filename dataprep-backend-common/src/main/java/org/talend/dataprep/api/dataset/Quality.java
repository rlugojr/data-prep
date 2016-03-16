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

package org.talend.dataprep.api.dataset;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Quality bean linked to column metadata.
 */
public class Quality implements Serializable {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /** Number of empty records. */
    @JsonProperty("empty")
    private int empty;

    /** The most frequent sub type of the type considered met while searching for invalids. */
    private String mostFrequentSubType;

    /** Number of invalid records. */
    @JsonProperty("invalid")
    private int invalid;

    /** Number of valid records. */
    @JsonProperty("valid")
    private int valid;

    /** List of invalid values. */
    @JsonProperty("invalidValues")
    private Set<String> invalidValues = new HashSet<>();

    /**
     * @return the number of empty records.
     */
    public int getEmpty() {
        return empty;
    }

    /**
     * @param empty the number of empty records to set.
     */
    public void setEmpty(int empty) {
        this.empty = empty;
    }

    /**
     * @return the type suggested for a potential new analysis
     */
    public String getMostFrequentSubType() {
        return mostFrequentSubType;
    }

    /**
     *
     * @param mostFrequentSubType the suggested type for a potential new analysis
     */
    public void setMostFrequentSubType(String mostFrequentSubType) {
        this.mostFrequentSubType = mostFrequentSubType;
    }

    /**
     * @return the number of invalid records.
     */
    public int getInvalid() {
        return invalid;
    }

    /**
     * @param invalid the number of invalid records to set.
     */
    public void setInvalid(int invalid) {
        this.invalid = invalid;
    }

    /**
     * @return the number of valid records.
     */
    public int getValid() {
        return valid;
    }

    /**
     * @param valid the number of valid records to set.
     */
    public void setValid(int valid) {
        this.valid = valid;
    }

    /**
     * @return the invalid values
     */
    public Set<String> getInvalidValues() {
        return invalidValues;
    }

    /**
     * @param invalidValues the invalid values to set.
     */
    public void setInvalidValues(Set<String> invalidValues) {
        this.invalidValues = invalidValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quality)) {
            return false;
        }

        Quality quality = (Quality) o;

        if (empty != quality.empty) {
            return false;
        }
        if (invalid != quality.invalid) {
            return false;
        }
        if (valid != quality.valid) {
            return false;
        }
        if (mostFrequentSubType != null ? !mostFrequentSubType.equals(quality.mostFrequentSubType) : quality.mostFrequentSubType != null) {
            return false;
        }
        return invalidValues != null ? invalidValues.equals(quality.invalidValues) : quality.invalidValues == null;

    }

    @Override
    public int hashCode() {
        int result = empty;
        result = 31 * result + (mostFrequentSubType != null ? mostFrequentSubType.hashCode() : 0);
        result = 31 * result + invalid;
        result = 31 * result + valid;
        result = 31 * result + (invalidValues != null ? invalidValues.hashCode() : 0);
        return result;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "Quality{" + "empty=" + empty + ", invalid=" + invalid + ", valid=" + valid + '}';
    }
}
