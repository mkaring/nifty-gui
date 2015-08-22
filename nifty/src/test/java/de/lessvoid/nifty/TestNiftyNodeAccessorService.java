package de.lessvoid.nifty;

import de.lessvoid.nifty.spi.node.NiftyNodeAccessor;
import de.lessvoid.nifty.spi.node.NiftyNodeAccessorService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class TestNiftyNodeAccessorService implements NiftyNodeAccessorService {
  @Override
  public Iterator<NiftyNodeAccessor> iterator() {
    List<NiftyNodeAccessor> nodes = new ArrayList<>();
    nodes.add(new NiftyNodeLongAccessor());
    nodes.add(new NiftyNodeStringAccessor());

    return nodes.iterator();
  }
}
