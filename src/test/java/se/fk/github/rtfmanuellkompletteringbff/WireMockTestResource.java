package se.fk.github.rtfmanuellkompletteringbff;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class WireMockTestResource implements QuarkusTestResourceLifecycleManager
{

   private static WireMockServer server;

   public static WireMockServer getServer()
   {
      return server;
   }

   @Override
   public Map<String, String> start()
   {
      server = new WireMockServer(wireMockConfig().dynamicPort());
      server.start();
      return Map.of("quarkus.rest-client.backend.url", server.baseUrl());
   }

   @Override
   public void stop()
   {
      if (server != null)
      {
         server.stop();
         server = null;
      }
   }
}
