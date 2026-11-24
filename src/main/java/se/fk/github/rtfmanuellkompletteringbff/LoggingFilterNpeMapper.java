package se.fk.github.rtfmanuellkompletteringbff;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.bff.errorhandling.ErrorResponse;
import se.fk.rimfrost.framework.bff.errorhandling.GlobalExceptionMapper;

/**
 * Supplements rimfrost-framework-bff's GlobalExceptionMapper for one case it does not cover
 * (RTFKB-FR-04.2).
 *
 * <p>On a reset connection the response has no headers, and fk-logging's response filter
 * dereferences them unguarded. The resulting NPE carries no IOException anywhere in its graph,
 * so the framework's catch-all answers 500 instead of 502. The only signal left that this was a
 * transport failure is the frame it was thrown from. Being more specific than the framework's
 * {@code Exception} handler, this mapper takes precedence for every NPE; NPEs from anywhere else
 * are handed back to the framework's handler, so they get exactly the response it would give.
 *
 * <p>Remove this class when rimfrost-framework-bff recognises the logging-filter NPE itself, or
 * fk-logging guards the null.
 */
@ApplicationScoped
public class LoggingFilterNpeMapper
{
   private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilterNpeMapper.class);

   private static final String LOGGING_RESPONSE_FILTER = "se.fk.github.logging.callerinfo.filter.LoggingContextClientResponseFilter";

   @Inject
   GlobalExceptionMapper globalExceptionMapper;

   @ServerExceptionMapper
   public Response handleNullPointerException(NullPointerException e)
   {
      if (isLoggingFilterNpe(e))
      {
         LOGGER.error("Backend unreachable", e);
         return Response.status(502).entity(new ErrorResponse("Upstream unavailable")).build();
      }
      return globalExceptionMapper.handleException(e);
   }

   private static boolean isLoggingFilterNpe(NullPointerException e)
   {
      for (StackTraceElement frame : e.getStackTrace())
      {
         if (LOGGING_RESPONSE_FILTER.equals(frame.getClassName()))
            return true;
      }
      return false;
   }
}
