package de.uni_muenster.imi.fhirFacade.fhir

import ca.uhn.fhir.context.FhirContext
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import org.hl7.fhir.r4.model.IdType
import java.util.*

private val fhirContext = FhirContext.forR4()
private val xmlParser = fhirContext.newXmlParser()
private val jsonParser = fhirContext.newJsonParser()


fun encodeFromResource(resource: IBaseResource, parserType: ParserType = ParserType.XML): String? {
    return when (parserType) {
        ParserType.XML -> stripNamespaceFromXML(xmlParser.encodeResourceToString(resource))
        ParserType.JSON -> jsonParser.encodeResourceToString(resource)
        else -> null
    }
}


fun decodeFromString(resourceString: String): IBaseResource? {
    return try {
        if (resourceString.startsWith("<")) {
            decodeFromString(resourceString, ParserType.XML)
        } else {
            decodeFromString(resourceString, ParserType.JSON)
        }
    } catch(e: Exception) {
        null
    }
}

private fun decodeFromString(resourceString: String, parserType: ParserType): IBaseResource? {
    return when (parserType) {
        ParserType.XML -> xmlParser.parseResource(resourceString)
        ParserType.JSON -> jsonParser.parseResource(resourceString)
        else -> null
    }
}

fun decodeQueryResults(resultString: String): List<IBaseResource> {
    val result = mutableListOf<IBaseResource>()
    splitSearchResults(resultString).forEach {
        result.add(decodeFromString(it.trim())!!)
    }
    return result
}

fun splitSearchResults(result: String): List<String> {
    return StringUtils.substringsBetween(result, "<result>", "</result>").asList()
}

fun stripNamespaceFromXML(resource: String): String {
    return resource.replace("xmlns=\"http://hl7.org/fhir\"", "")
}

fun IBaseResource.incrementVersion() {
    if (this.idElement.hasVersionIdPart()) {
        this.setId(
            IdType(
                this.fhirType(),
                this.idElement.idPart,
                "${this.idElement.versionIdPart.toInt() + 1}"
            )
        )
        this.meta.lastUpdated = Date(System.currentTimeMillis())
    } else {
        this.addVersion()
    }
}

fun IBaseResource.addVersion() {
    this.setId(
        IdType(
            this.fhirType(),
            this.idElement.idPart,
            "1"
        )
    )
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.hasVersionIdPart(): Boolean {
    return this.idElement!!.hasVersionIdPart()
}