package se.fk.github.rtfmanuellkompletteringbff;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.bff.errorhandling.ErrorResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that NPEs not thrown by fk-logging's response filter get the framework's response (RTFKB-FR-04).
 */
@QuarkusTest
class LoggingFilterNpeMapperTest
{
   @Inject
   LoggingFilterNpeMapper mapper;

   @Test
   @DisplayName("RTFKB-FR-04.2: En NPE med ett undertryckt IOException från annan källa ger 502 som i ramverket")
   void npeWithSuppressedIOException_returns502()
   {
      var npe = new NullPointerException("elsewhere");
      npe.addSuppressed(new IOException("connection reset"));

      var response = mapper.handleNullPointerException(npe);

      assertEquals(502, response.getStatus());
      assertEquals(new ErrorResponse("Upstream unavailable"), response.getEntity());
   }

   @Test
   @DisplayName("RTFKB-FR-04.3: En annan NPE ger ramverkets enhetliga 500-svar")
   void otherNpe_returns500()
   {
      var response = mapper.handleNullPointerException(new NullPointerException("bug"));

      assertEquals(500, response.getStatus());
      assertEquals(new ErrorResponse("Internal server error"), response.getEntity());
   }
}
