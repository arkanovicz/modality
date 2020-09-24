package com.republicate.modality.annotations;

import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.KClass

@SupportedAnnotationTypes("com.republicate.modality.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ModalityProcessor : AbstractProcessor() {

    override fun init(processingEnv: ProcessingEnvironment) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Initializing Modality processor")
        super.init(processingEnv)
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Processing ${annotations?.size ?: 0} annotations")
        val entityElements =
                roundEnv.getElementsAnnotatedWith(processingEnv.elementUtils.getTypeElement("com.republicate.modality.annotations.Entity"))
        entityElements.forEach {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Found annotated element ${it.simpleName}")
        }


/*
        val annotatedElements = roundEnv.getElementsAnnotatedWith(TestAnnotation::class.java)
        if (annotatedElements.isEmpty()) return false
        
        val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"] ?: run {
            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        val generatedKtFile = kotlinFile("test.generated") {
            for (element in annotatedElements) {
                val typeElement = element.toTypeElementOrNull() ?: continue

                property("simpleClassName") {
                    receiverType(typeElement.qualifiedName.toString())
                    getterExpression("this::class.java.simpleName")
                }
            }
        }

        File(kaptKotlinGeneratedDir, "testGenerated.kt").apply {
            parentFile.mkdirs()
            writeText(generatedKtFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
        }
*/
        return true
    }

    /*
    fun Element.toTypeElementOrNull(): TypeElement? {
        if (this !is TypeElement) {
            processingEnv.messager.printMessage(ERROR, "Invalid element type, class expected", this)
            return null
        }

        return this
    }
     */
}
