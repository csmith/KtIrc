package com.dmdirc.ktirc.util

/**
 * Documents when a deprecated feature will be removed.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.TYPEALIAS)
annotation class RemoveIn(val version: String)
