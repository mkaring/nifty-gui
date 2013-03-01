package de.lessvoid.nifty.gdx.input.events;

import com.badlogic.gdx.InputProcessor;

import de.lessvoid.nifty.NiftyInputConsumer;

/**
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public interface GdxInputEvent {
  boolean sendToNifty(NiftyInputConsumer consumer);

  void sendToGdx(InputProcessor processor);

  void freeEvent();
}
