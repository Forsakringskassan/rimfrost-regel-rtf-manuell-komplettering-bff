package se.fk.github.rtfmanuellkompletteringbff;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class GlobalExceptionMapper
{
   private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionMapper.class);

   private static final String LOGGING_RESPONSE_FILTER = "se.fk.github.logging.callerinfo.filter.LoggingContextClientResponseFilter";

   @ServerExceptionMapper
   public Response handleWebApplicationException(WebApplicationException e)
   {
      LOGGER.error("Upstream error status={}", e.getResponse().getStatus(), e);
      return Response.status(e.getResponse().getStatus())
            .entity(Map.of("error", "Upstream error")).build();
   }

   // MicroProfile REST Client spec: transport-level errors throw ProcessingException
   @ServerExceptionMapper
   public Response handleProcessingException(ProcessingException e)
   {
      LOGGER.error("Backend unreachable", e);
      return Response.status(502).entity(Map.of("error", "Upstream unavailable")).build();
   }

   // Catch-all: handles exceptions not matched above. In particular, LoggingContextClientResponseFilter
   // from fk-logging NPEs when there are no response headers (connection-reset scenario), which bypasses
   // the ProcessingException path.
   @ServerExceptionMapper
   public Response handleException(Exception e)
   {
      if (isNetworkError(e))
      {
         LOGGER.error("Backend unreachable", e);
         return Response.status(502).entity(Map.of("error", "Upstream unavailable")).build();
      }
      LOGGER.error("Internal error", e);
      return Response.status(500).entity(Map.of("error", "Internal server error")).build();
   }

   // Walks the full exception graph (cause chain + suppressed) looking for evidence that the
   // backend connection failed. Suppressed traversal is needed because RESTEasy Reactive may
   // suppress the original network IOException under the NPE thrown by the logging filter.
   private static boolean isNetworkError(Throwable root)
   {
      Set<Throwable> visited = new HashSet<>();
      Deque<Throwable> queue = new ArrayDeque<>();
      if (root != null)
         queue.add(root);
      while (!queue.isEmpty())
      {
         Throwable t = queue.poll();
         if (!visited.add(t))
            continue;
         if (t instanceof IOException || isLoggingFilterNpe(t))
            return true;
         if (t.getCause() != null)
            queue.add(t.getCause());
         for (Throwable s : t.getSuppressed())
            queue.add(s);
      }
      return false;
   }

   // On a reset connection the response has no headers, and fk-logging's response filter
   // dereferences them unguarded. The resulting NPE carries no IOException anywhere in its
   // graph, so the only signal left that this was a transport failure is the frame it was
   // thrown from. If fk-logging ever guards the null, this stops matching and the request
   // surfaces through the ProcessingException path above instead — which is also 502.
   private static boolean isLoggingFilterNpe(Throwable t)
   {
      if (!(t instanceof NullPointerException))
         return false;
      for (StackTraceElement frame : t.getStackTrace())
      {
         if (LOGGING_RESPONSE_FILTER.equals(frame.getClassName()))
            return true;
      }
      return false;
   }
}
