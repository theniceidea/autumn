package org.summerframework.core.base;

public interface ExcludeStrategy {
    boolean isExclude(Class<?> cls, String methodName);
}
