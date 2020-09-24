package com.republicate.modality.annotations;

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Entity(val name: String)
{
}
