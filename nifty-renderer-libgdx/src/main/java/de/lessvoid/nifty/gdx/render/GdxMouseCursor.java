package de.lessvoid.nifty.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.IOException;

import de.lessvoid.nifty.spi.render.MouseCursor;

/**
 * This is the mouse cursor implementation for libGDX mouse cursors.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public final class GdxMouseCursor implements MouseCursor {
  private final Sprite sprite;
  private final AssetManager assetManager;
  private final String cursorFile;

  public GdxMouseCursor(
      final AssetManager assetManager, final String cursor, final int hotspotX, final int hotspotY) throws IOException {
    this.assetManager = assetManager;
    cursorFile = cursor;
    try {
      if (!assetManager.isLoaded(cursor, Texture.class)) {
        assetManager.load(cursor, Texture.class);
        assetManager.finishLoading();
      }
      final Texture texture = assetManager.get(cursor, Texture.class);
      sprite = new Sprite(texture);
      sprite.setOrigin(hotspotX, hotspotY);
    } catch (final GdxRuntimeException ex) {
      throw new IOException(ex);
    }
  }

  public void render(final SpriteBatch batch) {
    final int mouseX = Gdx.input.getX();
    final int mouseY = Gdx.input.getY();

    batch.begin();
    sprite.setPosition(mouseX, mouseY);
    sprite.draw(batch);
    batch.end();
  }

  @Override
  public void dispose() {
    assetManager.unload(cursorFile);
  }
}
