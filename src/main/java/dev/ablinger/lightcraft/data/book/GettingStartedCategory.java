package dev.ablinger.lightcraft.data.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryLayout;
import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import dev.ablinger.lightcraft.LightCraft;
import dev.ablinger.lightcraft.data.book.gettingstarted.WelcomeEntry;

/** The entry point of the Light Book: what the mod is and where to start. */
public class GettingStartedCategory extends CategoryProvider {

  public static final String ID = "getting_started";

  public GettingStartedCategory(SingleBookSubProvider parent) {
    super(parent);
  }

  @Override
  protected void configureLayout(CategoryLayout layout) {
    layout.entry(WelcomeEntry.ID).at(0, 0);
  }

  @Override
  protected void generateEntries() {
    this.add(new WelcomeEntry(this).generate());
  }

  @Override
  protected String categoryName() {
    return "Getting Started";
  }

  @Override
  protected BookIconModel categoryIcon() {
    return BookIconModel.create(LightCraft.LIGHT_BOOK.get());
  }

  @Override
  public String categoryId() {
    return ID;
  }
}
