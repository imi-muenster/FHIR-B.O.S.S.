package de.uni_muenster.imi.fhirFacade.fhir

import de.uni_muenster.imi.fhirFacade.basex.BaseX
import de.uni_muenster.imi.fhirFacade.basex.BaseXQueries
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.Create
import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.Read
import ca.uhn.fhir.rest.annotation.ResourceParam
import ca.uhn.fhir.rest.annotation.Update
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.RestfulServer
import de.uni_muenster.imi.fhirFacade.utils.Properties
import mu.KotlinLogging
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet

private lateinit var settings: Properties
private lateinit var baseX: BaseX



/**
 * Main Servlet. Main Context is initialized here.
 */
@WebServlet(urlPatterns = ["/fhir/*"], displayName = "FHIR Server")
class FhirServer: RestfulServer() {

    private val log = KotlinLogging.logger {  }
    private val serialVersionUID = 1L


    @Throws(ServletException::class)
    override fun initialize() {
        settings = Properties(servletContext, "WEB-INF/classes/settings/default.properties")
        baseX = BaseX(settings)

        fhirContext = FhirContext.forR4()

        registerProviders(mutableListOf(
            PlainProvider(Patient())
        ))

        log.info("FHIR Server started")
    }
}

class PlainProvider<T : IBaseResource>(val resourceType: T): IResourceProvider {

    override fun getResourceType(): Class<out IBaseResource> {
        return resourceType::class.java
    }
    //TODO: Change methods to use decodeQueryResults (rewrite?) to support multiple results
    //TODO: Read FHIR Documentation on how these functions should behave

    @Read(version = true)
    fun getResourceById(@IdParam theId: IdType): IBaseResource? {
        return if(theId.hasVersionIdPart()) {
            decodeFromString(
                baseX.executeXQuery(
                    BaseXQueries.getByIdAndVersion("${resourceType::class.simpleName}", theId.idPart, theId.versionIdPart)
                )
            )
        } else {
            decodeFromString(
                baseX.executeXQuery(
                    BaseXQueries.getById("${resourceType::class.simpleName}", theId.idPart)
                )
            )
        }
    }


    @Update()
    fun update(@IdParam theId: IdType, @ResourceParam theResource: IBaseResource): MethodOutcome {

        return MethodOutcome()
    }

    @Create
    fun create(@ResourceParam theResource: String): MethodOutcome {
        val decodedResource = decodeFromString(theResource)
        return if (decodedResource != null) {
            baseX.postResourceToBaseX(decodedResource)

            MethodOutcome().apply {
                id = decodedResource.idElement
                created = true
            }
        } else {
            MethodOutcome().apply {
                created = false
            }
        }
    }
}
