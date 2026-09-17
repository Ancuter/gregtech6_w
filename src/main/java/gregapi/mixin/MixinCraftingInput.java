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

package gregapi.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Recipes keyed on empty-cell position lost that signal once the engine started trimming the grid to its
 *  occupied bounds; this mixin captures the full grid at the one choke point crafting assembly passes through. */
@Mixin(CraftingInput.class)
public class MixinCraftingInput {
	@Inject(method = "ofPositioned", at = @At("RETURN"))
	private static void gt6$rememberFullGrid(int aWidth, int aHeight, List<ItemStack> aItems, CallbackInfoReturnable<CraftingInput.Positioned> aCallback) {
		CraftingInput.Positioned tPositioned = aCallback.getReturnValue();
		if (tPositioned != null) gregapi.util.CR.rememberFullGrid(tPositioned.input(), aWidth, aHeight, aItems);
	}
}
