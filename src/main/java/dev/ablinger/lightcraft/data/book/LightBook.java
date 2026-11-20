package dev.ablinger.lightcraft.data.book;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import dev.ablinger.lightcraft.LightCraft;
import dev.ablinger.lightcraft.data.book.pocketdimension.PocketDimensionCategory;

/**
 * Datagen definition of the Light Book, LightCraft's in-game documentation.
 *
 * <p>The book id matches {@link LightCraft#LIGHT_BOOK_ID}, which the custom
 * {@code light_book} item uses to open it. Only documentation content belongs in
 * this package; gameplay definitions stay where they are.</p>
 */
public class LightBook extends SingleBookSubProvider {

  public static final String ID = "light_book";

  public LightBook() {
    super(ID, LightCraft.MOD_ID);
  }

  @Override
  protected BookModel additionalSetup(BookModel book) {
    // LightCraft registers its own book item, so Modonomicon must not create a second one.
    return book.withGenerateBookItem(false)
        .withCustomBookItem(LightCraft.LIGHT_BOOK_ID);
  }

  @Override
  protected void registerDefaultMacros() {
    // No shared macros yet.
  }

  @Override
  protected void generateCategories() {
    this.add(new GettingStartedCategory(this).generate());
    this.add(new PocketDimensionCategory(this).generate());
  }

  @Override
  protected String bookName() {
    return "Light Book";
  }

  @Override
  protected String bookTooltip() {
    return "LightCraft's in-game guide.";
  }
}
