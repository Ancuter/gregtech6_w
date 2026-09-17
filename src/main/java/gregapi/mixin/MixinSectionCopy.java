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

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.ImmutableMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** The per-section render snapshot copied the whole chunk's block entities into every section; harmless for
 *  vanilla, but GT6 stores material per block entity, multiplying into hundreds of MB of live garbage. */
// The target class is package-private, so it is addressed by name, as usual for such classes.
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCopy")
public abstract class MixinSectionCopy {

	@Redirect(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"))
	private ImmutableMap<BlockPos, BlockEntity> gt6$copyOnlyOwnSection(Map<? extends BlockPos, ? extends BlockEntity> aWholeChunk, LevelChunk aChunk, int aSectionIndex) {
		if (aWholeChunk.isEmpty()) return ImmutableMap.of();
		// An out-of-world section index has no block entities by definition; the engine never takes a section there either.
		if (aSectionIndex < 0 || aSectionIndex >= aChunk.getSectionsCount()) return ImmutableMap.of();
		int tMinY = aChunk.getSectionYFromSectionIndex(aSectionIndex) << 4, tMaxY = tMinY + 15;
		ImmutableMap.Builder<BlockPos, BlockEntity> rOut = ImmutableMap.builder();
		for (Map.Entry<? extends BlockPos, ? extends BlockEntity> tEntry : aWholeChunk.entrySet()) {
			int tY = tEntry.getKey().getY();
			if (tY >= tMinY && tY <= tMaxY) rOut.put(tEntry.getKey(), tEntry.getValue());
		}
		return rOut.build();
	}
}
