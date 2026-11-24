package se.fk.github.rtfmanuellkompletteringbff;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.rtfmanuellkompletteringbff.integration.InformationsaccessPublisher;
import se.fk.github.rtfmanuellkompletteringbff.integration.RtfManuellKompletteringClient;
import se.fk.github.rtfmanuellkompletteringbff.model.BackendGetResponse;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

// The Authorization header is forwarded to the backend by rimfrost-framework-bff (RTFKB-NFR-02.1),
// and upstream errors are mapped by its GlobalExceptionMapper (RTFKB-FR-04).
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RtfManuellKompletteringBffController
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RtfManuellKompletteringBffController.class);

   @RestClient
   RtfManuellKompletteringClient backendClient;

   @Inject
   InformationsaccessPublisher informationsaccessPublisher;

   @GET
   @Path("/{handlaggningId}/komplettering")
   public Response getKomplettering(@PathParam("handlaggningId") String handlaggningId)
   {
      LOGGER.debug("GET /api/{}/komplettering", handlaggningId);
      BackendGetResponse response = backendClient.getKomplettering(handlaggningId);
      // Publish before returning so no data is shown without a logged access (RTFKB-FR-05.5)
      informationsaccessPublisher.publish(handlaggningId, response);
      return Response.ok(response.data()).build();
   }

   @PATCH
   @Path("/{handlaggningId}/komplettering")
   public Response patchKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         @Valid RtfKompletteringData body)
   {
      LOGGER.debug("PATCH /api/{}/komplettering", handlaggningId);
      backendClient.patchKomplettering(handlaggningId, body);
      return Response.noContent().build();
   }

   @POST
   @Path("/{handlaggningId}/komplettering/done")
   @Consumes(MediaType.WILDCARD)
   public Response kompletteringDone(@PathParam("handlaggningId") String handlaggningId)
   {
      LOGGER.debug("POST /api/{}/komplettering/done", handlaggningId);
      backendClient.kompletteringDone(handlaggningId);
      return Response.noContent().build();
   }

   // uppgiftstyp is accepted in the path for FE compatibility but the backend exposes a single endpoint
   @GET
   @Path("/uppgiftsbeskrivning/{uppgiftstyp}")
   public Response getUppgiftsbeskrivning()
   {
      LOGGER.debug("GET /api/uppgiftsbeskrivning");
      GetUtokadUppgiftsbeskrivningResponse data = backendClient.getUtokadUppgiftsbeskrivning();
      return Response.ok(data).build();
   }
}
