package de.lessvoid.nifty.node;

import de.lessvoid.nifty.spi.node.NiftyNodeAccessor;
import de.lessvoid.nifty.spi.node.NiftyNodeAccessorService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public final class DefaultNiftyNodeAccessorService implements NiftyNodeAccessorService {
  @Override
  public Iterator<NiftyNodeAccessor> iterator() {
    List<NiftyNodeAccessor> nodes = new ArrayList<>();
    nodes.add(new NiftyRootNodeAccessor());
    nodes.add(new NiftyContentNodeAccessor());
    nodes.add(new NiftyBackgroundColorNodeAccessor());
    nodes.add(new NiftyTransformationNodeAccessor());

    // layout nodes
    nodes.add(new FixedSizeLayoutNodeAccessor());
    nodes.add(new PaddingLayoutNodeAccessor());
    nodes.add(new StackLayoutNodeAccessor());
    nodes.add(new UniformStackLayoutNodeAccessor());

    return nodes.iterator();
  }
}
