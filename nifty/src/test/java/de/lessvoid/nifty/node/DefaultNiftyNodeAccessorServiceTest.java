package de.lessvoid.nifty.node;

import de.lessvoid.nifty.spi.node.NiftyNodeAccessorService;

import org.junit.Test;

import java.util.ServiceLoader;

import static org.junit.Assert.*;

/**
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class DefaultNiftyNodeAccessorServiceTest {
  @Test
  public void testPresenceInServiceLoader() {
    int count = 0;
    for (NiftyNodeAccessorService accessorService : ServiceLoader.load(NiftyNodeAccessorService.class)) {
      if (accessorService instanceof DefaultNiftyNodeAccessorService) {
        count++;
      }
    }
    assertEquals("Expected to find DefaultNiftyNodeAccessorService once among the services!", 1, count);
  }
}