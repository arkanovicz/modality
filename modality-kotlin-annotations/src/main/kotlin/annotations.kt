package com.republicate.modality.kotlin;

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Entity(val name: String)
{
}
