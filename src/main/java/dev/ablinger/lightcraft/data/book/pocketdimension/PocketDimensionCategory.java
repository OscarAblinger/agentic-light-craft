package dev.ablinger.lightcraft.data.book.pocketdimension;

import com.klikli_dev.modonomicon.api.datagen.CategoryLayout;
import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import dev.ablinger.lightcraft.LightCraft;
import dev.ablinger.lightcraft.data.book.pocketdimension.PocketDimensionEntry;
import dev.ablinger.lightcraft.data.book.pocketdimension.PocketDimensionGrowthEntry;

/**
 * The Pocket Dimension section of the Light Book: what the private pocket
 * cube is, how to use it and how to grow it.
 */
public class PocketDimensionCategory extends CategoryProvider {

  public static final String ID = "pocket_dimension";

  public PocketDimensionCategory(SingleBookSubProvider parent) {
    super(parent);
  }

  @Override
  protected void configureLayout(CategoryLayout layout) {
    layout.entry(PocketDimensionEntry.ID).at(0, 0);
    layout.entry(PocketDimensionGrowthEntry.ID).at(2, 0);
  }

  @Override
  protected void generateEntries() {
    this.add(new PocketDimensionEntry(this).generate());
    this.add(new PocketDimensionGrowthEntry(this).generate());
  }

  @Override
  protected String categoryName() {
    return "Pocket Dimension";
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
