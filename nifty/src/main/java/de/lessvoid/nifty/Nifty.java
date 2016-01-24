/*
 * Copyright (c) 2015, Nifty GUI Community 
 * All rights reserved. 
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.lessvoid.nifty;

import de.lessvoid.nifty.input.NiftyInputConsumer;
import de.lessvoid.nifty.input.NiftyKeyboardEvent;
import de.lessvoid.nifty.input.NiftyPointerEvent;
import de.lessvoid.nifty.node.NiftyReferenceNode;
import de.lessvoid.nifty.node.NiftyRootNode;
import de.lessvoid.nifty.spi.NiftyInputDevice;
import de.lessvoid.nifty.spi.NiftyRenderDevice;
import de.lessvoid.nifty.spi.NiftyRenderDevice.FilterMode;
import de.lessvoid.nifty.spi.NiftyRenderDevice.PreMultipliedAlphaMode;
import de.lessvoid.nifty.spi.TimeProvider;
import de.lessvoid.nifty.spi.node.NiftyLayoutNodeImpl;
import de.lessvoid.nifty.spi.node.NiftyLayoutReceiver;
import de.lessvoid.nifty.spi.node.NiftyNode;
import de.lessvoid.nifty.spi.node.NiftyNodeImpl;
import de.lessvoid.nifty.types.NiftyColor;
import de.lessvoid.niftyinternal.*;
import de.lessvoid.niftyinternal.accessor.NiftyAccessor;
import de.lessvoid.niftyinternal.animate.IntervalAnimator;
import de.lessvoid.niftyinternal.common.Statistics;
import de.lessvoid.niftyinternal.common.StatisticsRendererFPS;
import de.lessvoid.niftyinternal.render.NiftyRenderer;
import de.lessvoid.niftyinternal.render.standard.StandardNiftyRenderer;
import de.lessvoid.niftyinternal.render.font.FontRenderer;
import de.lessvoid.niftyinternal.tree.InternalNiftyTree;
import org.jglfont.JGLFontFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Logger;

import static de.lessvoid.niftyinternal.tree.NiftyTreeNodeConverters.toNiftyNode;
import static de.lessvoid.niftyinternal.tree.NiftyTreeNodeConverters.toNiftyNodeClass;
import static de.lessvoid.niftyinternal.tree.NiftyTreeNodePredicates.nodeClass;
import static de.lessvoid.niftyinternal.tree.NiftyTreeNodePredicates.nodeImplAny;

/**
 * The main control class of all things Nifty.
 * @author void
 */
public class Nifty {
  private final static Logger logger = Logger.getLogger(Nifty.class.getName());

  // The resource loader.
  private final NiftyResourceLoader resourceLoader = new NiftyResourceLoader();

  // The one and only NiftyStatistics instance.
  private final NiftyStatistics statistics;
  private final Statistics stats;

  // The NiftyRenderDevice we'll forward all render calls to.
  private final NiftyRenderDevice renderDevice;

  // The TimeProvider to use.
  private final TimeProvider timeProvider;

  // The list of nodes that are able to receive input events
  private final List<NiftyNode> nodesToReceiveEvents = new ArrayList<>();

  // the class performing the conversion from the NiftyTree to actually output on screen
  private final NiftyRenderer renderer;

  // the class that interfaces us to input events (mouse, touch, keyboard)
  private NiftyInputDevice inputDevice;

  // the FontFactory
  private final JGLFontFactory fontFactory;

  // whenever we need to build a string we'll re-use this instance instead of creating new instances all the time
  private final StringBuilder str = new StringBuilder();

  // the EventBus this Nifty instance will use
  private final InternalNiftyEventBus eventBus = new InternalNiftyEventBus();

  // in case someone presses and holds a pointer on a node this node will capture all pointer events unless the pointer
  // is released again. the node that captured the pointer events will be stored in this member variable. if it is set
  // all pointer events will be send to that node unless the pointer is released again.
  //private NiftyNode nodeThatCapturedPointerEvents;

  // The main data structure to keep the Nifty scene graph
  private final InternalNiftyTree tree;

  // We keep NiftyReferenceNodes in this map for fast lookup by the reference
  private final Map<String, NiftyReferenceNode> referenceNodeLookup = new HashMap<>();

  //The layout handler for Nifty.
  private final NiftyLayout layout;

  // node impl class mapping
  private final InternalNiftyNodeAccessorRegistry nodeAccessorRegistry;

  // IntervalAnimator will execute given NiftyCallbacks at given intervals
  private List<IntervalAnimator> animators = new ArrayList<>();

  // configuration
  private NiftyConfigurationImpl configuration = new NiftyConfigurationImpl();

  // ready for a whole new can of worms?
  private Executor executor = Executors.newSingleThreadExecutor();

  /**
   * Create a new Nifty instance.
   * @param newRenderDevice the NiftyRenderDevice this instance will be using
   * @param newTimeProvider the TimeProvider implementation to use
   */
  public Nifty(
      final NiftyRenderDevice newRenderDevice,
      final NiftyInputDevice newInputDevice,
      final TimeProvider newTimeProvider) {
    nodeAccessorRegistry = new InternalNiftyNodeAccessorRegistry();

    renderDevice = newRenderDevice;
    renderDevice.setResourceLoader(resourceLoader);

    inputDevice = newInputDevice;
    inputDevice.setResourceLoader(resourceLoader);

    timeProvider = newTimeProvider;

    statistics = new NiftyStatistics(new Statistics(timeProvider));
    stats = statistics.getImpl();
    renderer = new StandardNiftyRenderer(stats, renderDevice, configuration);
    fontFactory = new JGLFontFactory(new FontRenderer(newRenderDevice));

    NiftyNodeImpl<NiftyRootNode> rootNodeImpl = niftyNodeImpl(new NiftyRootNode());
    tree = new InternalNiftyTree(rootNodeImpl);
    layout = new NiftyLayout(this, tree);

    if (rootNodeImpl instanceof NiftyLayoutNodeImpl) {
      ((NiftyLayoutNodeImpl) rootNodeImpl).onAttach(layout);
    }
  }

  /**
   * Set the NiftyStatisticsMode to display the statistics.
   * @param mode the new NiftyStatisticsMode
   */
  public void showStatistics(final NiftyStatisticsMode mode) {
    switch (mode) {
      case ShowFPS:
        new StatisticsRendererFPS(this);
        break;
    }
  }

  /**
   * Check all @Handler annotations at the given listener Object and subscribe all of them.
   * @param listener the Object to check for annotations
   */
  public void subscribe(final Object listener) {
    eventBus.subscribe(listener);
  }

  /**
   * Update.
   */
  public void update() {
    stats.startFrame();

    layout.update();

    stats.startInputProcessing();
    processInputEvents(collectInputReceivers());
    stats.stopInputProcessing();

    for (int i=0; i<animators.size(); i++) {
      animators.get(i).update();
    }

    stats.startUpdate();
    /* FIXME
    for (int i=0; i<rootNodes.size(); i++) {
      rootNodes.get(i).getImpl().update();
    }
    */
    stats.stopUpdate();
  }

  /**
   * Render the Nifty scene.
   *
   * @return true if the frame changed and false if the content is still the same
   */
  public boolean render() {
    stats.startRender();
    boolean frameChanged = renderer.render(tree);
    stats.stopRender();
    stats.endFrame();
    return frameChanged;
  }

  /**
   * Calls back the given callback after delay ms every interval ms. This will call NiftyCallback on another thread
   * so be careful what you do there. Nifty 2.0 is not yet thread safe!
   *
   * @param delay time to wait in ms
   * @param interval interval to call the callback in ms
   * @param callback the actual callback
   */
  public void startAnimatedThreaded(final long delay, final long interval, final NiftyCallback<Float> callback) {
    animators.add(new IntervalAnimator(getTimeProvider(), delay, interval, new NiftyCallback<Float>() {
      @Override
      public void execute(final Float aFloat) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            callback.execute(aFloat);
          }
        });
      }
    }));
  }

  public void startAnimated(final long delay, final long interval, final NiftyCallback<Float> callback) {
    animators.add(new IntervalAnimator(getTimeProvider(), delay, interval, callback));
  }

  public <N extends NiftyNode> void startAnimated(final long delay, final long interval, final N node, final NiftyNodeCallback<Float, N> callback) {
    startAnimated(delay, interval, new NiftyCallback<Float>() {
      @Override
      public void execute(final Float aFloat) {
        callback.execute(aFloat, node);
      }
    });
  }

  /**
   * Create a new NiftyImage.
   * @param filename the filename to load
   *
   * @return a new NiftyImage
   */
  public NiftyImage createNiftyImage(final String filename) {
    // TODO consider to make the FilterMode and especially PreMultipliedAlphaMode availabe to the user
    return NiftyImage.newInstance(InternalNiftyImage.newImage(renderDevice.loadTexture(
        filename,
        FilterMode.Linear,
        PreMultipliedAlphaMode.PreMultiplyAlpha)));
  }

  /**
   * Get the width of the current screen mode.
   * @return width of the current screen
   */
  public int getScreenWidth() {
    return renderDevice.getDisplayWidth();
  }

  /**
   * Get the height of the current screen mode.
   * @return height of the current screen
   */
  public int getScreenHeight() {
    return renderDevice.getDisplayHeight();
  }

  /**
   * Output the state of all root nodes (and the whole tree below) to a String. This is meant to aid in debugging.
   * DON'T RELY ON ANY INFORMATION IN HERE SINCE THIS CAN BE CHANGED IN FUTURE RELEASES!
   *
   * @return String that contains the debugging info for all root nodes
   */
  public String getSceneInfoLog() {
    return getSceneInfoLog("(?s).*");
  }

  /**
   * Output the state of all Nifty to a String. This is meant to aid in debugging.
   * DON'T RELY ON ANY INFORMATION IN HERE SINCE THIS CAN BE CHANGED IN FUTURE RELEASES!
   *
   * @param filter regexp to filter the output (Example: "position" will only output position info)
   * @return String that contains the debugging info for all root nodes
   */
  public String getSceneInfoLog(final String filter) {
    return "Nifty scene info log\n" + tree.toString();
  }

  /**
   * Get the NiftyStatistics instance where you can request a lot of statistics about Nifty.
   * @return the NiftyStatistics instance
   */
  public NiftyStatistics getStatistics() {
    return statistics;
  }

  /**
   * Get the TimeProvider of this Nifty instance.
   * @return the TimeProvider
   */
  public TimeProvider getTimeProvider() {
    return timeProvider;
  }

  /**
   * Call this to let Nifty clear the screen when it renders the GUI. This might be useful when the only thing you're
   * currently rendering is the GUI. If you render the GUI as an overlay you better not enable that :)
   */
  public void clearScreenBeforeRender() {
    renderDevice.clearScreenBeforeRender(true);
  }

  /**
   * Load a NiftyFont with the given name.
   *
   * @param name the name of the NiftyFont
   * @return NiftyFont
   * @throws IOException
   */
  public NiftyFont createFont(final String name) throws IOException {
    if (name == null) {
      return null;
    }
    return new NiftyFont(fontFactory.loadFont(resourceLoader.getResourceAsStream(name), name, 12), name);
  }

  /////////////////////////////////////////////////////////////////////////////
  // NiftyTree
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Add the given NiftyNode to the root node.
   *
   * @param child the child to add as well
   * @return this
   */
  public NiftyNodeBuilder addNode(final NiftyNode child) {
    return addNode(tree.getRootNode().getNiftyNode(), child);
  }

  /**
   * Add the given child(s) NiftyNode(s) to the given parent NiftyNode.
   *
   * @param parent the NiftyNode parent to add the child to
   * @param child the child NiftyNode to add to the parent
   * @return this
   */
  public NiftyNodeBuilder addNode(
      @Nonnull final NiftyNode parent,
      @Nonnull final NiftyNode child) {
    NiftyNodeImpl<? extends NiftyNode> childImpl = niftyNodeImpl(child);
    processNodeAdding(childImpl);
    tree.addChild(niftyNodeImpl(parent), childImpl);

    if (child instanceof NiftyReferenceNode) {
      NiftyReferenceNode referenceNode = (NiftyReferenceNode) child;
      referenceNodeLookup.put(referenceNode.getId(), referenceNode);
    }

    return new NiftyNodeBuilder(this, parent, child);
  }

  /**
   * Remove the NiftyNode from the tree.
   *
   * @param niftyNode the NiftyNode to remove
   */
  public void remove(@Nonnull final NiftyNode niftyNode) {
    NiftyNodeImpl<? extends NiftyNode> nodeImpl = niftyNodeImpl(niftyNode);
    tree.remove(nodeImpl);
    processNodeRemoving(nodeImpl);
  }

  /**
   * Return a depth first Iterator for all NiftyNodes in this tree.
   * @return the Iterator
   */
  public Iterable<? extends NiftyNode> childNodes() {
    return tree.childNodes(nodeImplAny(), toNiftyNode());
  }

  /**
   * Return a depth first Iterator for all child nodes of the given parent node.
   * @return the Iterator
   */
  public Iterable<NiftyNode> childNodes(@Nonnull final NiftyNode startNode) {
    return tree.childNodes(nodeImplAny(), toNiftyNode(), niftyNodeImpl(startNode));
  }

  /**
   * Return a depth first Iterator for all NiftyNodes in this tree that are instances of the given class.
   * @param clazz only return entries if they are instances of this clazz
   * @return the Iterator
   */
  public <X extends NiftyNode> Iterable<X> filteredChildNodes(@Nonnull final Class<X> clazz) {
    return tree.childNodes(nodeClass(clazz), toNiftyNodeClass(clazz));
  }

  /**
   * Return a depth first Iterator for all child nodes of the given startNode.
   * @param clazz only return entries if they are instances of this clazz
   * @param startNode the start node
   * @return the Iterator
   */
  public <X extends NiftyNode> Iterable<X> filteredChildNodes(@Nonnull final Class<X> clazz,
                                                              @Nonnull final NiftyNode startNode) {
    return tree.childNodes(nodeClass(clazz), toNiftyNodeClass(clazz), niftyNodeImpl(startNode));
  }

  /**
   * Return a NiftyReferenceNode for the given id.
   *
   * @param id the id of the reference node
   * @return the NiftyReferenceNode that corresponds to the given id or null if the node doesn't exist
   */
  public NiftyReferenceNode getNiftyReferenceNode(final String id) {
    return referenceNodeLookup.get(id);
  }

  /**
   * Return the parent NiftyNode of the niftyNode given.
   *
   * @param niftyNode the NiftyNode to determine the parent node from
   * @param <T> NiftyNode
   * @return the parent NiftyNode
   */
  public <T extends NiftyNode> NiftyNode getParent(final T niftyNode) {
    NiftyNodeImpl<T> impl = niftyNodeImpl(niftyNode);
    NiftyNodeImpl parent = tree.getParent(impl);
    if (parent == null) {
      return null;
    }
    return parent.getNiftyNode();
  }

  @Nonnull
  private <T extends NiftyNode> NiftyNodeImpl<T> niftyNodeImpl(final T child) {
    return nodeAccessorRegistry.getImpl(child);
  }

  /////////////////////////////////////////////////////////////////////////////
  // NiftyConfiguration
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Set showRenderBuckets to true to overlay a random color quad above each
   * updated RenderBucket.
   * @param showRenderBuckets the value to set showRenderBuckets to
   */
  public void setShowRenderBuckets(final boolean showRenderBuckets) {
    configuration.setShowRenderBuckets(showRenderBuckets);
  }

  /**
   * Set showRenderNodes to true to overlay a quad in the color set
   * to the showRenderNodeOverlayColor.
   * @param showRenderNodes the value to set showRenderNodes to
   */
  public void setShowRenderNodes(final boolean showRenderNodes) {
    configuration.setShowRenderNodes(showRenderNodes);
  }

  /**
   * Set the showRenderNodeOverlayColor - the color used to overlay
   * each renderNode when showRenderNodes is set to true.
   * @param color the color to render showRenderNodes overlays with
   */
  public void setShowRenderNodeOverlayColor(final NiftyColor color) {
    configuration.setShowRenderNodeOverlayColor(color);
  }

  /////////////////////////////////////////////////////////////////////////////
  // Private methods
  /////////////////////////////////////////////////////////////////////////////

  private List<NiftyNode> collectInputReceivers() {
    /* FIXME
    nodesToReceiveEvents.clear();
    for (int i=0; i<rootNodes.size(); i++) {
      InternalNiftyNode impl = rootNodes.get(i).getImpl();
      if (!impl.isNiftyPrivateNode()) {
        impl.addInputNodes(nodesToReceiveEvents);
      }
    }
    */
    return sortInputReceivers(nodesToReceiveEvents);
  }

  // sorts in place (the source list) and returns the sorted source list
  private List<NiftyNode> sortInputReceivers(final List<NiftyNode> source) {
// FIXME    Collections.sort(source, Collections.reverseOrder(inputEventReceiversComparator));
    return source;
  }

  private void logInputReceivers(final List<NiftyNode> source) {
    str.setLength(0);
    str.append("inputReceivers: ");
    for (int j=0; j<source.size(); j++) {
      str.append("[");
      // FIXME str.append(source.get(j).getImpl().getId());
      str.append("]");
      str.append(" ");
    }
    logger.fine(str.toString());
  }

  private void processInputEvents(final List<NiftyNode> inputReceivers) {
    inputDevice.forwardEvents(new NiftyInputConsumer() {
      @Override
      public boolean processPointerEvent(final NiftyPointerEvent... pointerEvents) {
        logInputReceivers(inputReceivers);
/*  FIXME
        for (int i=0; i<pointerEvents.length; i++) {
          if (nodeThatCapturedPointerEvents != null) {
            if (nodeThatCapturedPointerEvents.getImpl().capturedPointerEvent(pointerEvents[i])) {
              nodeThatCapturedPointerEvents = null;
            }
          } else {
            for (int j=0; j<inputReceivers.size(); j++) {
              InternalNiftyNode impl = inputReceivers.get(j).getImpl();
              if (impl.pointerEvent(pointerEvents[i])) {
                nodeThatCapturedPointerEvents = inputReceivers.get(j);
                break;
              }
            }
          }
        }
        */
        return false;
      }

      @Override
      public boolean processKeyboardEvent(final NiftyKeyboardEvent keyEvent) {
        return false;
      }
    });
  }

  /**
   * Prepare the node before adding them to the Nifty Node Tree. Any kind of initialization the specific node type
   * requires is done here.
   *
   * @param newNodeImpl the node implementation that is added to the tree
   */
  private void processNodeAdding(@Nonnull final NiftyNodeImpl<? extends NiftyNode> newNodeImpl) {
    if (newNodeImpl instanceof NiftyLayoutNodeImpl) {
      ((NiftyLayoutNodeImpl) newNodeImpl).onAttach(layout);
    }
    if (newNodeImpl instanceof NiftyLayoutReceiver) {
      layout.reportNewReceivers((NiftyLayoutReceiver<?>) newNodeImpl);
    }
  }

  /**
   * Inform the node that it was removed from the tree if required. This function is called directly after the node
   * is removed from the tree.
   *
   * @param newNodeImpl the node implementation that was removed from the tree
   */
  private void processNodeRemoving(@Nonnull final NiftyNodeImpl<? extends NiftyNode> newNodeImpl) {
    if (newNodeImpl instanceof NiftyLayoutNodeImpl) {
      ((NiftyLayoutNodeImpl) newNodeImpl).onDetach(layout);
    }
  }

  // Friend methods

  @Nonnull
  NiftyRenderDevice getRenderDevice() {
    return renderDevice;
  }

  @Nonnull
  InternalNiftyEventBus getEventBus() {
    return eventBus;
  }

  @Nonnull
  InternalNiftyTree getInternalNiftyTree() {
    return tree;
  }

  // Internal methods

  static {
    NiftyAccessor.DEFAULT = new InternalNiftyAccessorImpl();
  }
}
