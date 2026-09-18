package se.fk.github.rtfmanuellkompletteringbff.integration;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

@RegisterRestClient(configKey = "backend")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface RtfManuellKompletteringClient
{
   @GET
   @Path("/{handlaggningId}")
   RtfKompletteringData getKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         @HeaderParam("Authorization") String authorization);

   @PATCH
   @Path("/{handlaggningId}")
   void patchKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         RtfKompletteringData body,
         @HeaderParam("Authorization") String authorization);

   // Response-typed: the backend distinguishes 204 (done), 409 (correlation state already
   // cleared by timeout) and 422 (yrkande still incomplete). All three are meaningful to the
   // frontend, so the status is read and forwarded rather than raised as an exception.
   @POST
   @Path("/{handlaggningId}/done")
   Response kompletteringDone(
         @PathParam("handlaggningId") String handlaggningId,
         @HeaderParam("Authorization") String authorization);

   @GET
   @Path("/utokadUppgiftsbeskrivning")
   GetUtokadUppgiftsbeskrivningResponse getUtokadUppgiftsbeskrivning();
}
