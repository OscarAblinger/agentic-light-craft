package dev.ablinger.lightcraft.data.book.pocketdimension;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.client.gui.book.theme.GuiSprite;
import dev.ablinger.lightcraft.LightCraft;

/** The pocket dimension itself: what it is and how the player uses it. */
public class PocketDimensionEntry extends EntryProvider {

  public static final String ID = "pocket_dimension";

  public PocketDimensionEntry(CategoryProvider parent) {
    super(parent);
  }

  @Override
  protected void generatePages() {
    this.page("overview", () -> BookTextPageModel.create()
        .withTitle(this.context().pageTitle())
        .withText(this.context().pageText()));
    this.pageTitle("The Pocket Dimension");
    this.pageText("Every player owns a private pocket cube: a small quartz room that "
        + "is theirs alone. The base pocket cube holds a 5x5x5 interior wrapped in a "
        + "one-block-thick indestructible quartz shell, for an outer cube of 7x7x7, and "
        + "a bedrock-textured exit block sits at the center of the floor, where the "
        + "player always spawns. All monsters are blocked from spawning in the pocket "
        + "dimension, so the room stays a safe, quiet place to build.");

    this.page("using", () -> BookTextPageModel.create()
        .withTitle(this.context().pageTitle())
        .withText(this.context().pageText()));
    this.pageTitle("Entering and Leaving");
    this.pageText("Use the Pocket Dimension item to teleport into your own cube; the item "
        + "is never consumed. Use the item again - or right-click the bedrock exit block "
        + "inside the cube - to teleport back to where you entered, falling back to your "
        + "respawn point if that spot is no longer free.");
  }

  @Override
  protected String entryName() {
    return "Pocket Dimension";
  }

  @Override
  protected String entryDescription() {
    return "Your private pocket cube and how to use it.";
  }

  @Override
  protected GuiSprite entryBackground() {
    return EntryBackground.DEFAULT;
  }

  @Override
  protected BookIconModel entryIcon() {
    return BookIconModel.create(LightCraft.POCKET_DIMENSION_ITEM.get());
  }

  @Override
  protected String entryId() {
    return ID;
  }
}
