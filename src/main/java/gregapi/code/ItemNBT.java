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

package gregapi.code;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** @author Gregorius Techneticies
 *  Central ItemStack<->NBT bridge: the old mutable stack-level tag is now immutable DataComponents.CUSTOM_DATA,
 *  so code that mutates a tag from {@link #get(ItemStack)} must write it back via {@link #set} or lose it. */
public final class ItemNBT {
	private ItemNBT() {/**/}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CustomData tData = aStack.get(DataComponents.CUSTOM_DATA);
		return tData == null || tData.isEmpty() ? null : tData.copyTag();
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		if (aNBT == null || aNBT.isEmpty()) {
			aStack.remove(DataComponents.CUSTOM_DATA);
		} else {
			aStack.set(DataComponents.CUSTOM_DATA, CustomData.of(aNBT));
		}
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		CustomData tData = aStack.get(DataComponents.CUSTOM_DATA);
		return tData != null && !tData.isEmpty();
	}
}
