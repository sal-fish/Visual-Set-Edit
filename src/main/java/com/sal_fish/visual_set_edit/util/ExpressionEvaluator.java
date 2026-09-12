package com.sal_fish.visual_set_edit.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpressionEvaluator {
    private ExpressionEvaluator() {}

    // %<属性id>%，id 字符集同 ResourceLocation
    private static final Pattern ATTRIBUTE_PLACEHOLDER = Pattern.compile("%([a-z0-9_:/.-]+)%");

    // 出现成对 %...% 才算表达式；"50%" 只有一个 %，按固定值处理
    public static boolean looksLikeExpression(String s) {
        return s != null && !s.isBlank() && ATTRIBUTE_PLACEHOLDER.matcher(s).find();
    }

    // 表达式求值，失败返回 NaN
    public static double resolveThreshold(LivingEntity entity, String raw) {
        if (raw == null || raw.isBlank()) return Double.NaN;
        String trimmed = raw.trim();
        if (!looksLikeExpression(trimmed)) {
            try {
                return Double.parseDouble(trimmed);
            } catch (Exception ignored) {
                return Double.NaN;
            }
        }
        double result = resolve(entity, trimmed, Double.NaN);
        return Double.isFinite(result) ? result : Double.NaN;
    }

    public static double resolve(LivingEntity entity, String expression, double fallback) {
        if (expression == null || expression.isBlank()) return fallback;
        try {
            String substituted = substituteAttributes(entity, expression);
            if (substituted == null || substituted.isBlank()) return fallback;
            double result = evalArithmetic(substituted);
            return Double.isFinite(result) ? result : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static String substituteAttributes(LivingEntity entity, String expression) {
        Matcher m = ATTRIBUTE_PLACEHOLDER.matcher(expression);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            double value = lookupAttributeValue(entity, m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(Double.toString(value)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // 查实体属性当前值（含加成），无效 id 返回 0
    public static double lookupAttributeValue(LivingEntity entity, String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return 0;
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
        if (attr == null) return 0;
        AttributeInstance instance = entity.getAttribute(attr);
        return instance != null ? instance.getValue() : 0;
    }

    // 四则、括号、一元负号、小数、^ 幂（右结合）
    public static double evalArithmetic(String expression) {
        return new ExprParser(expression).parse();
    }

    private static final class ExprParser {
        private final String s;
        private int pos;

        ExprParser(String s) {
            this.s = s;
        }

        double parse() {
            double v = expression();
            skipWs();
            if (pos != s.length()) throw new IllegalArgumentException("unexpected tail: " + s.substring(pos));
            return v;
        }

        private void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private double expression() {
            double v = term();
            while (true) {
                skipWs();
                if (pos < s.length() && s.charAt(pos) == '+') { pos++; v += term(); }
                else if (pos < s.length() && s.charAt(pos) == '-') { pos++; v -= term(); }
                else return v;
            }
        }

        private double term() {
            double v = unary();
            while (true) {
                skipWs();
                if (pos < s.length() && s.charAt(pos) == '*') { pos++; v *= unary(); }
                else if (pos < s.length() && s.charAt(pos) == '/') {
                    pos++;
                    double d = unary();
                    if (d == 0) throw new ArithmeticException("divide by zero");
                    v /= d;
                } else return v;
            }
        }

        private double unary() {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '-') { pos++; return -unary(); }
            if (pos < s.length() && s.charAt(pos) == '+') { pos++; return unary(); }
            return power();
        }

        private double power() {
            double v = primary();
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '^') {
                pos++;
                double exponent = unary();
                v = Math.pow(v, exponent);
            }
            return v;
        }

        private double primary() {
            skipWs();
            if (pos >= s.length()) throw new IllegalArgumentException("unexpected end");
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                double v = expression();
                skipWs();
                if (pos >= s.length() || s.charAt(pos) != ')') throw new IllegalArgumentException("missing ')'");
                pos++;
                return v;
            }
            int start = pos;
            boolean dotSeen = false;
            while (pos < s.length()) {
                char ch = s.charAt(pos);
                if (Character.isDigit(ch)) pos++;
                else if (ch == '.' && !dotSeen) { dotSeen = true; pos++; }
                else break;
            }
            if (start == pos) throw new IllegalArgumentException("unexpected char: " + c);
            return Double.parseDouble(s.substring(start, pos));
        }
    }
}
