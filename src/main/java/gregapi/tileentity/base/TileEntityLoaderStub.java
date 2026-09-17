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

package gregapi.tileentity.base;

import com.mojang.serialization.MapCodec;

import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import static gregapi.data.CS.*;

/** A concrete stub neo creates on world-load for GT6 machines whose class comes from saved NBT, unavailable
 *  to BlockEntityType.create; it only captures raw NBT, and the registry replaces it with the real MTE on ChunkEvent.Load. */
public class TileEntityLoaderStub extends TileEntityBase01Root {
	public CompoundTag mLoadedNBT = null;

	public TileEntityLoaderStub(BlockPos aPos, BlockState aState) {super(F, aPos, aState);}

	@Override public String getTileEntityName() {return "gt.te.loader";}

	// Captures raw NBT without running readFromNBT, since the stub doesn't know the concrete MTE class; reconstruction
	// happens on ChunkEvent.Load.
	@Override protected void loadAdditional(ValueInput input) {
		mLoadedNBT = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(UT.NBT::make);
	}

	/** A chunk can save before the stub's reconstruction catches up, and writing only id/x/y/z through the normal
	 *  GT6 NBT bridge would erase all MTE data forever; the stub instead round-trips its captured NBT unchanged. */
	@Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		if (mLoadedNBT == null) {super.saveAdditional(output); return;}
		output.store(mLoadedNBT);
	}

	/** Same transparent-carrier trick, for the network: a stub often still occupies the position when the chunk
	 *  packet is built, so it must forward the identity it captured, or the client can never reconstruct the real MTE. */
	@Override protected void writeMTEIdentity(CompoundTag aNBT) {
		if (mLoadedNBT == null) return;
		if (mLoadedNBT.contains(NBT_MTE_REG)) aNBT.putShort(NBT_MTE_REG, mLoadedNBT.getShort(NBT_MTE_REG).orElse((short)0));
		if (mLoadedNBT.contains(NBT_MTE_ID )) aNBT.putShort(NBT_MTE_ID , mLoadedNBT.getShort(NBT_MTE_ID ).orElse((short)0));
		// Registry name comes from captured NBT if present, else derived from the number here on the server, since the client
		// has its own separate item numbering.
		String tName = mLoadedNBT.getString(NBT_MTE_REGNAME).orElse("");
		if (!tName.isEmpty()) aNBT.putString(NBT_MTE_REGNAME, tName);
		else if (mLoadedNBT.contains(NBT_MTE_REG)) gregapi.block.multitileentity.MultiTileEntityRegistry.writeRegistryName(aNBT, mLoadedNBT.getShort(NBT_MTE_REG).orElse((short)0));
	}
}
