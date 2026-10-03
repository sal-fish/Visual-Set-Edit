package com.sal_fish.visual_set_edit.api;

// 比较符工具，供外部条件字段实现常用比较
public final class ConditionCompare {

    private ConditionCompare() {}

    // 与内置条件相同的比较符语义
    public static boolean compare(double actual, String comparator, double expected) {
        return switch (comparator == null ? "" : comparator) {
            case "EQ" -> actual == expected;
            case "NEQ" -> actual != expected;
            case "GT" -> actual > expected;
            case "LT" -> actual < expected;
            case "GTE" -> actual >= expected;
            case "LTE" -> actual <= expected;
            default -> false;
        };
    }
}
