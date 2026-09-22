package se.fk.github.rtfmanuellkompletteringbff;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.github.rtfmanuellkompletteringbff.integration.RtfManuellKompletteringClient;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RtfManuellKompletteringBffController
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RtfManuellKompletteringBffController.class);

   @RestClient
   RtfManuellKompletteringClient backendClient;

   @GET
   @Path("/{handlaggningId}/komplettering")
   public Response getKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         @HeaderParam("Authorization") String authorization)
   {
      LOGGER.debug("GET /api/{}/komplettering", handlaggningId);
      RtfKompletteringData response = backendClient.getKomplettering(handlaggningId, authorization);
      return Response.ok(response).build();
   }

   @PATCH
   @Path("/{handlaggningId}/komplettering")
   public Response patchKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         @Valid RtfKompletteringData body,
         @HeaderParam("Authorization") String authorization)
   {
      LOGGER.debug("PATCH /api/{}/komplettering", handlaggningId);
      backendClient.patchKomplettering(handlaggningId, body, authorization);
      return Response.noContent().build();
   }

   @POST
   @Path("/{handlaggningId}/komplettering/done")
   @Consumes(MediaType.WILDCARD)
   public Response kompletteringDone(
         @PathParam("handlaggningId") String handlaggningId,
         @HeaderParam("Authorization") String authorization)
   {
      LOGGER.debug("POST /api/{}/komplettering/done", handlaggningId);
      backendClient.kompletteringDone(handlaggningId, authorization);
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
