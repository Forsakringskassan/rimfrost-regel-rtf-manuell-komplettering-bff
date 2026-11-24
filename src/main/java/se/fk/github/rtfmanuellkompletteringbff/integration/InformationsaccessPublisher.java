package se.fk.github.rtfmanuellkompletteringbff.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import se.fk.github.rtfmanuellkompletteringbff.model.BackendAnvandare;
import se.fk.github.rtfmanuellkompletteringbff.model.BackendGetResponse;
import se.fk.rimfrost.framework.bff.ilogg.ILogg;
import se.fk.rimfrost.framework.bff.ilogg.exception.ILoggException;
import se.fk.rimfrost.informationsaccess.Idtyp;
import se.fk.rimfrost.informationsaccess.InformationsaccessEvent;

/**
 * Publishes an informationsaccess event for the data the BFF sends to the frontend (RTFKB-FR-05).
 */
@ApplicationScoped
public class InformationsaccessPublisher
{
   static final String KALLA = "rimfrost-regel-rtf-manuell-komplettering-bff";

   private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>()
   {
   };

   @Inject
   ILogg iLogg;

   @Inject
   ObjectMapper objectMapper;

   /**
    * Publishes an informationsaccess event for a successful GET of kompletteringsdata.
    *
    * @param handlaggningId the handlaggning being accessed
    * @param response the backend response containing both data and informationsaccess metadata
    * @throws InformationsaccessPublishException if ILogg publishing fails (RTFKB-FR-05.5)
    */
   public void publish(String handlaggningId, BackendGetResponse response)
   {
      var informationsaccess = response.informationsaccess();

      var event = new InformationsaccessEvent();
      event.setId(UUID.randomUUID().toString());
      event.setAnvandare(toIdtyp(informationsaccess.anvandare()));
      event.setTidpunkt(OffsetDateTime.now());
      event.setKalla(KALLA);
      event.setHandlaggningId(handlaggningId);
      event.setUppgiftId(informationsaccess.uppgiftId().toString());
      event.setRegelId(informationsaccess.regelId().toString());
      event.setRegelVersion(informationsaccess.regelVersion());
      event.setVisadInformation(objectMapper.convertValue(response.data(), MAP_TYPE));

      try
      {
         iLogg.publish(event);
      }
      catch (ILoggException e)
      {
         // RTFKB-FR-05.5: no data is shown unless the access could be logged
         throw new InformationsaccessPublishException(
               "Failed to publish informationsaccess event for uppgift " + informationsaccess.uppgiftId(), e);
      }
   }

   private static Idtyp toIdtyp(BackendAnvandare anvandare)
   {
      var idtyp = new Idtyp();
      idtyp.setTypId(anvandare.typId());
      idtyp.setVarde(anvandare.varde());
      return idtyp;
   }
}
