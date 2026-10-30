package com.risk.platform.rule;

import java.util.Map;

/** 原子条件：字段 操作符 值（如 risk_score >= 80）。 */
public record Condition(String field, Op op, Object value) {

    public enum Op {
        /** 等于 */
        EQ,
        /** 大于等于 */
        GE,
        /** 大于 */
        GT,
        /** 小于等于 */
        LTE,
        /** 字符串包含 */
        CONTAINS
    }

    public boolean match(Map<String, Object> facts) {
        Object actual = facts.get(field);
        if (actual == null) {
            return false;
        }
        return switch (op) {
            case EQ -> actual.equals(value);
            case GE -> compareTo(actual, value) >= 0;
            case GT -> compareTo(actual, value) > 0;
            case LTE -> compareTo(actual, value) <= 0;
            case CONTAINS -> String.valueOf(actual).contains(String.valueOf(value));
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compareTo(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        return ((Comparable) a).compareTo(b);
    }
}
