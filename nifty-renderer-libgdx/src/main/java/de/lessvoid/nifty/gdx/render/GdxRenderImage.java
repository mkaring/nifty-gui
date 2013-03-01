package de.lessvoid.nifty.gdx.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import de.lessvoid.nifty.spi.render.RenderImage;

/**
 * This is the render image implementation that uses libGDX to display the graphics.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public class GdxRenderImage implements RenderImage {
  private final Sprite image;
  private final AssetManager assetManager;
  private final String file;

  public GdxRenderImage(final AssetManager assetManager, final String file, final boolean filterLinear) {
    this.assetManager = assetManager;
    this.file = file;

    if (!assetManager.isLoaded(file, Texture.class)) {
      assetManager.load(file, Texture.class);
      assetManager.finishLoading();
    }
    final Texture texture = assetManager.get(file, Texture.class);
    if (filterLinear) {
      texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    } else {
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }
    image = new Sprite(texture);
  }

  @Override
  public int getWidth() {
    return Math.round(image.getWidth());
  }

  @Override
  public int getHeight() {
    return Math.round(image.getHeight());
  }

  @Override
  public void dispose() {
    assetManager.unload(file);
  }

  public void render(
      final SpriteBatch guiBatch,
      final int x,
      final int y,
      final int width,
      final int height,
      final Color color,
      final float imageScale) {
    image.setOrigin(image.getWidth() / 2, image.getHeight() / 2);
    image.setBounds(0, 0, image.getWidth(), image.getHeight());
    image.setPosition(x, y);
    image.setScale(imageScale);
    image.setColor(color);
    image.setSize(width, height);
    image.draw(guiBatch);
  }

  public void render(
      final SpriteBatch guiBatch,
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
    image.setOrigin(centerX, centerY);
    image.setBounds(srcX, srcY, srcW, srcH);
    image.setPosition(x, y);
    image.setScale(scale);
    image.setColor(color);
    image.setSize(w, h);
    image.draw(guiBatch);
  }
}
