package com.github.ralgond.rls.db;

import java.util.regex.Pattern;

public class Rule {
    private int id;
    private String keyType;
    private String method;
    private String pathPattern;
    private int burst;
    private int tokenCount;
    private int tokenTimeUnit;

    private Pattern compiledPathPattern;

    public void updateCompiledPathPattern() {
        compiledPathPattern = Pattern.compile(pathPattern);
    }

    public Pattern getCompiledPathPattern() {
        return compiledPathPattern;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public int getBurst() {
        return burst;
    }

    public void setBurst(int burst) {
        this.burst = burst;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public int getTokenTimeUnit() {
        return tokenTimeUnit;
    }

    public void setTokenTimeUnit(int tokenTimeUnit) {
        this.tokenTimeUnit = tokenTimeUnit;
    }
}
