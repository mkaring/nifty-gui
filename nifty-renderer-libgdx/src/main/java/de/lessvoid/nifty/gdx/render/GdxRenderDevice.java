package de.lessvoid.nifty.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.io.IOException;

import de.lessvoid.nifty.render.BlendMode;
import de.lessvoid.nifty.spi.render.MouseCursor;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.render.RenderFont;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.resourceloader.NiftyResourceLoader;

/**
 * This render device uses libGDX to perform the render operations.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class GdxRenderDevice implements RenderDevice {
  /**
   * The shape renderer used by this class.
   */
  private ShapeRenderer shapeRenderer;

  /**
   * The asset manager that is used to load the required resources.
   */
  private final AssetManager assetManager;

  /**
   * The sprite batch used to render all the graphics of the GUI.
   */
  private final SpriteBatch guiBatch;

  public GdxRenderDevice(final AssetManager assetManager) {
    this.assetManager = assetManager;
    guiBatch = new SpriteBatch();
  }

  @Override
  public void setResourceLoader(final NiftyResourceLoader niftyResourceLoader) {
  }

  @Override
  public RenderImage createImage(final String filename, final boolean filterLinear) {
    return new GdxRenderImage(assetManager, filename, filterLinear);
  }

  @Override
  public RenderFont createFont(final String filename) {
    return new GdxRenderFont(assetManager, filename);
  }

  @Override
  public int getWidth() {
    return Gdx.graphics.getWidth();
  }

  @Override
  public int getHeight() {
    return Gdx.graphics.getHeight();
  }

  @Override
  public void beginFrame() {
    if (shapeRenderer == null) {
      shapeRenderer = new ShapeRenderer(2);
    }
    guiBatch.begin();
  }

  @Override
  public void endFrame() {
    if (activeMouseCursor != null) {
      activeMouseCursor.render(guiBatch);
    }
    guiBatch.end();
  }

  @Override
  public void clear() {
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public void setBlendMode(final BlendMode renderMode) {
    switch (renderMode) {
      case BLEND:
        Gdx.gl.glEnable(GL10.GL_BLEND);
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        break;
      case MULIPLY:
        Gdx.gl.glEnable(GL10.GL_BLEND);
        Gdx.gl.glColorMask(true, true, true, true);
        Gdx.gl.glBlendFunc(GL10.GL_ONE_MINUS_SRC_COLOR, GL10.GL_SRC_COLOR);
        break;
    }
  }

  @Override
  public void renderQuad(final int x, final int y, final int width, final int height, final Color color) {
    Gdx.gl10.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    shapeRenderer.rect(x, y, width, height);
  }

  /**
   * This is the first color that is used as temporary color during render operations. The usage of this color does
   * never exceed a single function run.
   */
  private final com.badlogic.gdx.graphics.Color tmpColor1 = new com.badlogic.gdx.graphics.Color();

  /**
   * This is the second color that is used as temporary color during render operations. The usage of this color does
   * never exceed a single function run.
   */
  private final com.badlogic.gdx.graphics.Color tmpColor2 = new com.badlogic.gdx.graphics.Color();

  /**
   * This is the third color that is used as temporary color during render operations. The usage of this color does
   * never exceed a single function run.
   */
  private final com.badlogic.gdx.graphics.Color tmpColor3 = new com.badlogic.gdx.graphics.Color();

  /**
   * This is the fourth color that is used as temporary color during render operations. The usage of this color does
   * never exceed a single function run.
   */
  private final com.badlogic.gdx.graphics.Color tmpColor4 = new com.badlogic.gdx.graphics.Color();

  @Override
  public void renderQuad(
      final int x,
      final int y,
      final int width,
      final int height,
      final Color topLeft,
      final Color topRight,
      final Color bottomRight,
      final Color bottomLeft) {
    copyColorToGdx(bottomLeft, tmpColor1);
    copyColorToGdx(bottomRight, tmpColor2);
    copyColorToGdx(topRight, tmpColor3);
    copyColorToGdx(topLeft, tmpColor4);
    shapeRenderer.rect(x, y, width, height, tmpColor1, tmpColor2, tmpColor3, tmpColor4);
  }

  /**
   * This function is used to copy the color from a Nifty-GUI color instance to a libGDX color instance.
   *
   * @param source the Nifty-GUI color
   * @param target the libGDX color
   */
  private static void copyColorToGdx(final Color source, final com.badlogic.gdx.graphics.Color target) {
    target.set(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha());
  }

  @Override
  public void renderImage(
      final RenderImage image,
      final int x,
      final int y,
      final int width,
      final int height,
      final Color color,
      final float imageScale) {
    if (image instanceof GdxRenderImage) {
      final GdxRenderImage gdxImage = (GdxRenderImage) image;
      copyColorToGdx(color, tmpColor1);
      gdxImage.render(guiBatch, x, y, width, height, tmpColor1, imageScale);
    }
  }

  @Override
  public void renderImage(
      final RenderImage image,
      final int x,
      final int y,
      final int w,
      final int h,
      final int srcX,
      final int srcY,
      final int srcW,
      final int srcH,
      final Color color,
      final float scale,
      final int centerX,
      final int centerY) {
    if (image instanceof GdxRenderImage) {
      final GdxRenderImage gdxImage = (GdxRenderImage) image;
      copyColorToGdx(color, tmpColor1);
      gdxImage.render(guiBatch, x, y, w, h, srcX, srcY, srcW, srcH, tmpColor1, scale, centerX, centerY);
    }
  }

  @Override
  public void renderFont(
      final RenderFont font,
      final String text,
      final int x,
      final int y,
      final Color fontColor,
      final float sizeX,
      final float sizeY) {
    if (font instanceof GdxRenderFont) {
      copyColorToGdx(fontColor, tmpColor1);
      ((GdxRenderFont) font).render(guiBatch, text, x, y, tmpColor1, sizeX, sizeY);
    }
  }

  @Override
  public void enableClip(final int x0, final int y0, final int x1, final int y1) {
    Gdx.gl.glEnable(GL10.GL_SCISSOR_TEST);
    Gdx.gl.glScissor(x0, y0, x1 - x0, y1 - y0);
  }

  @Override
  public void disableClip() {
    Gdx.gl.glDisable(GL10.GL_SCISSOR_TEST);
  }

  @Override
  public MouseCursor createMouseCursor(
      final String filename,
      final int hotspotX,
      final int hotspotY) throws IOException {
    return new GdxMouseCursor(assetManager, filename, hotspotX, hotspotY);
  }

  /**
   * The currently active mouse cursor that is rendered upon the end of each frame.
   */
  private GdxMouseCursor activeMouseCursor;

  @Override
  public void enableMouseCursor(final MouseCursor mouseCursor) {
    if (mouseCursor instanceof GdxMouseCursor) {
      activeMouseCursor = (GdxMouseCursor) mouseCursor;
    }
  }

  @Override
  public void disableMouseCursor() {
    activeMouseCursor = null;
  }
}
