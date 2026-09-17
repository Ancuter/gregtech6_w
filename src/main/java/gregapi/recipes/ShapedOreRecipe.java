/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.recipes;

import gregapi.oredict.OreDictionary;
import gregapi.util.ST;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/** Replacement for Forge's ShapedOreRecipe: pattern parsing and shifted/mirrored grid matching, using
 *  neo's already-trimmed CraftingInput, GT6's own stack equality, and the ore dictionary for ore names. */
public class ShapedOreRecipe implements ICraftingRecipeGT {
	protected final ItemStack mOutput;
	/** Row-major cells; each is null (empty), a single ItemStack, or a List<ItemStack> of alternatives. */
	protected final Object[] mInput;
	protected final int mWidth, mHeight;
	protected boolean mMirrored = F;
	/** Marks this as an ore-recipe substitute for a vanilla datapack recipe rather than a native GT6 one;
	 *  mSourceId is the datapack recipe's own key, used to suppress it when this replacement applies. */
	public boolean mVanillaReplacement = F;
	public net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> mSourceId = null;

	public ShapedOreRecipe(ItemStack aResult, Object... aRecipe) {
		mOutput = ST.copy(aResult);
		int tIdx = 0;

		// Forge's format: a leading Boolean means mirrored, followed by the actual recipe array.
		if (aRecipe.length > 0 && aRecipe[0] instanceof Boolean) {
			mMirrored = (Boolean)aRecipe[0];
			if (aRecipe.length > 1 && aRecipe[1] instanceof Object[]) aRecipe = (Object[])aRecipe[1]; else tIdx = 1;
		}

		// Pattern is either an array of row strings or a flat sequence of strings.
		StringBuilder tShape = new StringBuilder();
		int tWidth = 0, tHeight = 0;
		if (aRecipe[tIdx] instanceof String[]) {
			for (String tRow : (String[])aRecipe[tIdx++]) {tWidth = tRow.length(); tShape.append(tRow); tHeight++;}
		} else {
			while (tIdx < aRecipe.length && aRecipe[tIdx] instanceof String) {String tRow = (String)aRecipe[tIdx++]; tWidth = tRow.length(); tShape.append(tRow); tHeight++;}
		}
		// Builds the symbol-to-ingredient map from the remaining (Character, ingredient) pairs.
		Map<Character, Object> tMap = new HashMap<>();
		for (; tIdx < aRecipe.length; tIdx += 2) {
			Character tChar = (Character)aRecipe[tIdx];
			Object tIn = aRecipe[tIdx+1];
			if (tIn instanceof ItemStack) tMap.put(tChar, ST.copy((ItemStack)tIn));
			else if (tIn instanceof List) tMap.put(tChar, tIn);
			else if (tIn instanceof String) tMap.put(tChar, OreDictionary.getOres((String)tIn));
			// Forge turned an Item/Block cell into meta 0, not a wildcard; a bare ItemStack here would otherwise
			// match any durability and let damaged armor pass a recipe meant to require an intact one.
			else if (tIn instanceof ItemLike) tMap.put(tChar, ST.make(((ItemLike)tIn).asItem(), 1, 0));
			else throw new IllegalArgumentException("Invalid shaped ore recipe ingredient: " + tIn);
		}

		Object[] tCells = new Object[tWidth * tHeight];
		char[] tChars = tShape.toString().toCharArray();
		for (int i = 0; i < tChars.length && i < tCells.length; i++) tCells[i] = tMap.get(tChars[i]); // A space with no map entry means an empty cell.

		// 1.7.10 slid the pattern to every offset against the full grid; neo trims the grid and drops the offset,
		// so the same effect is reproduced by trimming the declared pattern's own empty border instead.
		int tLeft = tWidth, tRight = -1, tTop = tHeight, tBottom = -1;
		for (int y = 0; y < tHeight; y++) for (int x = 0; x < tWidth; x++) if (tCells[x + y*tWidth] != null) {
			if (x < tLeft  ) tLeft   = x;
			if (x > tRight ) tRight  = x;
			if (y < tTop   ) tTop    = y;
			if (y > tBottom) tBottom = y;
		}
		if (tRight < 0 || (tRight-tLeft+1 == tWidth && tBottom-tTop+1 == tHeight)) {
			// Pattern is fully empty or already has no border; left as declared.
			mWidth = tWidth; mHeight = tHeight; mInput = tCells;
		} else {
			int tNewWidth = tRight-tLeft+1, tNewHeight = tBottom-tTop+1;
			Object[] tCropped = new Object[tNewWidth * tNewHeight];
			for (int y = 0; y < tNewHeight; y++) for (int x = 0; x < tNewWidth; x++) tCropped[x + y*tNewWidth] = tCells[(x+tLeft) + (y+tTop)*tWidth];
			mWidth = tNewWidth; mHeight = tNewHeight; mInput = tCropped;
		}
	}

	@Override
	public boolean matches(CraftingInput aGrid, Level aWorld) {
		if (aGrid.width() != mWidth || aGrid.height() != mHeight) return F;
		return checkMatch(aGrid, F) || (mMirrored && checkMatch(aGrid, T));
	}

	protected boolean checkMatch(CraftingInput aGrid, boolean aMirror) {
		for (int y = 0; y < mHeight; y++) for (int x = 0; x < mWidth; x++) {
			Object tTarget = mInput[(aMirror ? (mWidth-1-x) : x) + y*mWidth];
			ItemStack tActual = aGrid.getItem(x, y);
			if (tTarget == null) {if (!tActual.isEmpty()) return F;}
			else if (!ShapelessOreRecipe.ingredientMatches(tActual, tTarget)) return F;
		}
		return T;
	}

	@Override
	public ItemStack getCraftingResult(CraftingInput aGrid) {return ST.copy(mOutput);}

	/** Forge's setMirrored: allow a mirrored match. Returns itself for chaining. */
	public ShapedOreRecipe setMirrored(boolean aMirrored) {mMirrored = aMirrored; return this;}

	/** @return the cell inputs (null/ItemStack/List<ItemStack>), matching Forge's Object[] getInput(). */
	public Object[] getInput() {return mInput;}

	/** Grid width is needed for JEI's layout; a read-only getter, doesn't widen the crafting contract. */
	public int getWidth() {return mWidth;}
	/** Grid height is needed for JEI's layout; a read-only getter, doesn't widen the crafting contract. */
	public int getHeight() {return mHeight;}

	@Override public int getRecipeSize() {return mWidth * mHeight;}
	@Override public ItemStack getRecipeOutput() {return mOutput;}
	@Override public boolean isRemovableByGT() {return T;}
	@Override public boolean isAutocraftableByGT() {return T;}
}
