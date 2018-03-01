package com.gome.maven.codeInspection.dataFlow;

/**
 * @author peter
 */
public enum NullabilityProblem {
    callNPE,
    fieldAccessNPE,
    unboxingNullable,
    assigningToNotNull,
    nullableReturn,
    passingNullableToNotNullParameter,
    passingNullableArgumentToNonAnnotatedParameter,
}
