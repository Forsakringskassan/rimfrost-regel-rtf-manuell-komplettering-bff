package se.fk.github.rtfmanuellkompletteringbff.integration;

/** Thrown when ILogg publishing fails, preventing data from being returned (RTFKB-FR-05.5). */
public class InformationsaccessPublishException extends RuntimeException
{
   public InformationsaccessPublishException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
