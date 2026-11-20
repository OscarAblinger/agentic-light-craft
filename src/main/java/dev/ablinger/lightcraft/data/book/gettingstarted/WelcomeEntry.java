package dev.ablinger.lightcraft.data.book.gettingstarted;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.client.gui.book.theme.GuiSprite;
import dev.ablinger.lightcraft.LightCraft;

/** First page of the Light Book. Placeholder for the planned documentation structure. */
public class WelcomeEntry extends EntryProvider {

  public static final String ID = "welcome";

  public WelcomeEntry(CategoryProvider parent) {
    super(parent);
  }

  @Override
  protected void generatePages() {
    this.page("intro", () -> BookTextPageModel.create()
        .withTitle(this.context().pageTitle())
        .withText(this.context().pageText()));

    this.pageTitle("Welcome to LightCraft");
    this.pageText("This is the Light Book, LightCraft's in-game documentation. "
        + "Use the categories to browse what this mod adds and how its systems fit together.");
  }

  @Override
  protected String entryName() {
    return "Welcome";
  }

  @Override
  protected String entryDescription() {
    return "Start here for an overview of the Light Book.";
  }

  @Override
  protected GuiSprite entryBackground() {
    return EntryBackground.DEFAULT;
  }

  @Override
  protected BookIconModel entryIcon() {
    return BookIconModel.create(LightCraft.LIGHT_BOOK.get());
  }

  @Override
  protected String entryId() {
    return ID;
  }
}
