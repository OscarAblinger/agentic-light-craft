package dev.ablinger.lightcraft.data.book.pocketdimension;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.client.gui.book.theme.GuiSprite;
import dev.ablinger.lightcraft.LightCraft;

/** Growing a pocket cube: the upgrade items and the admin command. */
public class PocketDimensionGrowthEntry extends EntryProvider {

  public static final String ID = "pocket_dimension_growth";

  public PocketDimensionGrowthEntry(CategoryProvider parent) {
    super(parent);
  }

  @Override
  protected void generatePages() {
    this.page("growth", () -> BookTextPageModel.create()
        .withTitle(this.context().pageTitle())
        .withText(this.context().pageText()));
    this.pageTitle("Growing Your Pocket Cube");
    this.pageText("While standing inside your pocket cube, use a Pocket Dimension Upgrade "
        + "item to grow it: the (7x7x7), (9x9x9), (11x11x11) or (15x15x15) upgrade expands "
        + "the interior accordingly. Upgrades are independent of each other, so the "
        + "(15x15x15) upgrade works straight from the base 5x5x5 cube. A successful upgrade "
        + "consumes one upgrade item - creative mode is exempt - and the new room keeps "
        + "everything you built. Using an upgrade for the same or a smaller size does "
        + "nothing.");
  }

  @Override
  protected String entryName() {
    return "Growing the Pocket Dimension";
  }

  @Override
  protected String entryDescription() {
    return "How to grow your private pocket cube.";
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
