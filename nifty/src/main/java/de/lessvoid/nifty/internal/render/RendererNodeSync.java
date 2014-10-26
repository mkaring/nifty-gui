package de.lessvoid.nifty.internal.render;

import java.util.List;

import de.lessvoid.nifty.api.NiftyNode;
import de.lessvoid.nifty.internal.InternalNiftyNode;
import de.lessvoid.nifty.internal.accessor.NiftyNodeAccessor;
import de.lessvoid.nifty.spi.NiftyRenderDevice;

/**
 * Synchronize a list of NiftyNodes to a list of RootRenderNode.
 *
 * @author void
 */
public class RendererNodeSync {
  // since we want to access the nodes private API we need a NiftyNodeAccessor
  private final NiftyNodeAccessor niftyNodeAccessor;

  // and since we need to create render resources like textures we'll keep an NiftyRenderDevice around as well
  private final NiftyRenderDevice renderDevice;

  /**
   * Create the RendererNodeSync using the NiftyRenderDevice given.
   * @param renderDevice the NiftyRenderDevice
   */
  public RendererNodeSync(final NiftyRenderDevice renderDevice) {
    this.niftyNodeAccessor = NiftyNodeAccessor.getDefault();
    this.renderDevice = renderDevice;
  }

  /**
   * Synchronize the list of given NiftyNode srcNodes into the list of RootRenderNode in dstNodes.
   * @param srcNodes the source nodes
   * @param dstNodes the destination nodes
   * @return true if anything changed and false if it is exactly the same as in the last frame
   */
  public boolean synchronize(final List<NiftyNode> srcNodes, final List<RenderNode> dstNodes) {
    boolean changed = false;

    for (int i=0; i<srcNodes.size(); i++) {
      InternalNiftyNode src = niftyNodeAccessor.getInternalNiftyNode(srcNodes.get(i));

      // skip nodes that have zero size
      if (src.getWidth() == 0 && src.getHeight() == 0) {
        continue;
      }

      RenderNode dst = findNode(dstNodes, src.getId());
      if (dst == null) {
        RenderNode childRenderNode = createRenderNode(src);
        dstNodes.add(childRenderNode);
        childRenderNode.setIndexInParent(dstNodes.size() - 1);

        changed = true;
        continue;
      }
      boolean syncChanged = syncRenderNodeBufferChildNodes(src, dst) != 0;
      changed = changed || syncChanged;
    }

    return changed;
  }

  private RenderNode findNode(final List<RenderNode> nodes, final int id) {
    for (int i=0; i<nodes.size(); i++) {
      RenderNode node = nodes.get(i);
      if (node.getNodeId() == id) {
        return node;
      }
    }
    return null;
  }

  private RenderNode createRenderNode(final InternalNiftyNode node) {
    RenderNode renderNode = new RenderNode(
        node.getId(),
        node.getLocalTransformation(),
        node.getWidth(),
        node.getHeight(),
        node.getCanvas().getCommands(),
        renderDevice.createTexture(node.getWidth(), node.getHeight(), true),
        node.getBlendMode(),
        node.getRenderOrder());

    for (int i=0; i<node.getChildren().size(); i++) {
      RenderNode childRenderNode = createRenderNode(node.getChildren().get(i));
      renderNode.addChildNode(childRenderNode);
      childRenderNode.setIndexInParent(i);
    }

    return renderNode;
  }

  private final int NEEDS_RENDER = 0x01;
  private final int NEEDS_CONTENT = 0x02;

  private int syncRenderNodeBufferChildNodes(final InternalNiftyNode src, final RenderNode dst) {
    boolean canvasChanged = canvasChanged(src, dst);
    boolean needsRender = needsRender(src, dst);

    boolean childCanvasChanged = false;
    boolean childNeedsRender = false;
    for (int i=0; i<src.getChildren().size(); i++) {
      int currentChildChanged = syncRenderNodeBufferChild(src.getChildren().get(i), dst);
      childCanvasChanged = childCanvasChanged || ((currentChildChanged & NEEDS_CONTENT) != 0);
      childNeedsRender = childNeedsRender || ((currentChildChanged & NEEDS_RENDER) != 0);
    }

    int result = 0;
    if (canvasChanged || childCanvasChanged) {
      dst.needsContentUpdate(src.getCanvas().getCommands());
      result |= NEEDS_CONTENT;
    }

    if (needsRender || childNeedsRender) {
      dst.setLocal(src.getLocalTransformation());
      dst.setWidth(src.getWidth());
      dst.setHeight(src.getHeight());
      dst.needsRender();
      result |= NEEDS_RENDER;
    }

    return result;
  }

  private boolean canvasChanged(final InternalNiftyNode src, final RenderNode dst) {
    if (src.getCanvas().isChanged()) {
      return true;
    }

    boolean widthChanged = dst.getWidth() != src.getWidth();
    boolean heightChanged = dst.getHeight() != src.getHeight();
    if (widthChanged || heightChanged) {
//      if (RedrawOnSizeChange) {
//        return true;
//      }
    }
    return false;
  }

  private boolean needsRender(final InternalNiftyNode src, final RenderNode dst) {
    boolean widthChanged = dst.getWidth() != src.getWidth();
    boolean heightChanged = dst.getHeight() != src.getHeight();
    boolean transformationChanged = src.isTransformationChanged();
    return (widthChanged || heightChanged || transformationChanged);
  }

  private int syncRenderNodeBufferChild(final InternalNiftyNode src, final RenderNode dstParent) {
    RenderNode dst = dstParent.findChildWithId(src.getId());
    if (dst == null) {
      dstParent.addChildNode(createRenderNode(src));
      return NEEDS_RENDER | NEEDS_CONTENT;
    }
    return syncRenderNodeBufferChildNodes(src, dst);
  }
}
