package se.fk.github.rtfmanuellkompletteringbff.integration;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import se.fk.github.rtfmanuellkompletteringbff.model.BackendGetResponse;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.regel.rtf.manuell.komplettering.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

// The incoming Authorization header is added to every call by rimfrost-framework-bff.
@RegisterRestClient(configKey = "backend")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface RtfManuellKompletteringClient
{
   /** Hämtar kompletteringsdata inkl. informationsaccess-wrapper (RTFKB-FR-05). */
   @GET
   @Path("/{handlaggningId}")
   BackendGetResponse getKomplettering(@PathParam("handlaggningId") String handlaggningId);

   @PATCH
   @Path("/{handlaggningId}")
   void patchKomplettering(
         @PathParam("handlaggningId") String handlaggningId,
         RtfKompletteringData body);

   // The backend answers 204 on success, and 409 (correlation state already cleared by timeout)
   // or 422 (yrkande still incomplete) otherwise. Both error statuses matter to the frontend and
   // reach it unchanged via the framework's GlobalExceptionMapper, which passes upstream statuses through.
   @POST
   @Path("/{handlaggningId}/done")
   void kompletteringDone(@PathParam("handlaggningId") String handlaggningId);

   @GET
   @Path("/utokadUppgiftsbeskrivning")
   GetUtokadUppgiftsbeskrivningResponse getUtokadUppgiftsbeskrivning();
}
