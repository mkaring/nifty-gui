package de.lessvoid.nifty.gdx.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import de.lessvoid.nifty.spi.render.RenderFont;

/**
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public final class GdxRenderFont implements RenderFont {
  private final AssetManager assetManager;
  private final String fontFileName;
  private final BitmapFont font;

  public GdxRenderFont(final AssetManager assetManager, final String fileName) {
    this.assetManager = assetManager;
    fontFileName = fileName;
    if (!assetManager.isLoaded(fileName, BitmapFont.class)) {
      assetManager.load(fileName, BitmapFont.class);
      assetManager.finishLoading();
    }
    font = assetManager.get(fileName, BitmapFont.class);
  }

  @Override
  public int getWidth(final String text) {
    return Math.round(font.getBounds(text).width);
  }

  @Override
  public int getWidth(final String text, final float size) {
    return Math.round(font.getBounds(text).width * size);
  }

  @Override
  public int getHeight() {
    return Math.round(font.getLineHeight());
  }

  @Override
  public int getCharacterAdvance(final char currentCharacter, final char nextCharacter, final float size) {
    return Math.round(font.getData().getGlyph(currentCharacter).getKerning(nextCharacter) * size);
  }

  @Override
  public void dispose() {
    assetManager.unload(fontFileName);
  }

  public void render(
      final SpriteBatch batch,
      final CharSequence text,
      final int x,
      final int y,
      final Color fontColor,
      final float sizeX,
      final float sizeY) {
    batch.setColor(fontColor);
    font.draw(batch, text, x, y);
  }
}
