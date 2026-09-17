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

package gregapi.block.prefixblock;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Ore material is stored in one compact chunk-wide position-to-material map instead of a tile entity per block,
 *  since a 32767-value property would blow up the engine's blockstate tables; the value is the same material id as before. */
public final class PrefixBlockOreMap {
	/** The +2048 offset covers every height range the engine allows, regardless of a given dimension's minY. */
	public static int key(int aX, int aY, int aZ) {return ((aY + 2048) << 8) | ((aZ & 15) << 4) | (aX & 15);}

	private final Int2ShortOpenHashMap mMap;

	public PrefixBlockOreMap() {mMap = new Int2ShortOpenHashMap(); mMap.defaultReturnValue((short)0);}
	private PrefixBlockOreMap(Int2ShortOpenHashMap aMap) {mMap = aMap; mMap.defaultReturnValue((short)0);}

	/** 0 means no entry at this position, matching the old "no tile entity" meaning exactly. */
	public short get(int aX, int aY, int aZ) {return mMap.get(key(aX, aY, aZ));}
	public void set(int aX, int aY, int aZ, short aMeta) {if (aMeta == 0) mMap.remove(key(aX, aY, aZ)); else mMap.put(key(aX, aY, aZ), aMeta);}
	public void remove(int aX, int aY, int aZ) {mMap.remove(key(aX, aY, aZ));}
	public boolean isEmpty() {return mMap.isEmpty();}
	public int size() {return mMap.size();}

	// Persists as a list of longs, each packing the key and material together, since the key fits in 20 bits and the material
	// in 16.
	private static final Codec<PrefixBlockOreMap> ENTRIES_CODEC = Codec.LONG.listOf().xmap(aList -> {
		Int2ShortOpenHashMap tMap = new Int2ShortOpenHashMap(aList.size());
		for (long tEntry : aList) tMap.put((int)(tEntry >>> 16), (short)(tEntry & 0xFFFFL));
		return new PrefixBlockOreMap(tMap);
	}, aMap -> {
		List<Long> rList = new ArrayList<>(aMap.mMap.size());
		for (it.unimi.dsi.fastutil.ints.Int2ShortMap.Entry tEntry : aMap.mMap.int2ShortEntrySet())
			rList.add(((long)tEntry.getIntKey() << 16) | (tEntry.getShortValue() & 0xFFFFL));
		return rList;
	});
	public static final MapCodec<PrefixBlockOreMap> CODEC = ENTRIES_CODEC.fieldOf("gt6_ore");

	public static final StreamCodec<RegistryFriendlyByteBuf, PrefixBlockOreMap> STREAM_CODEC = StreamCodec.of((aBuf, aMap) -> {
		aBuf.writeVarInt(aMap.mMap.size());
		for (it.unimi.dsi.fastutil.ints.Int2ShortMap.Entry tEntry : aMap.mMap.int2ShortEntrySet()) {
			aBuf.writeVarInt(tEntry.getIntKey());
			aBuf.writeShort(tEntry.getShortValue());
		}
	}, aBuf -> {
		int tSize = aBuf.readVarInt();
		Int2ShortOpenHashMap tMap = new Int2ShortOpenHashMap(tSize);
		for (int i = 0; i < tSize; i++) tMap.put(aBuf.readVarInt(), aBuf.readShort());
		return new PrefixBlockOreMap(tMap);
	});

	/** The single central DeferredRegister for every GT6 attachment type. */
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, gregapi.data.MD.GAPI.mID);
	public static final java.util.function.Supplier<AttachmentType<PrefixBlockOreMap>> TYPE = ATTACHMENTS.register("ore_map",
		() -> AttachmentType.builder(() -> new PrefixBlockOreMap()).serialize(CODEC, aMap -> !aMap.isEmpty()).sync(STREAM_CODEC).build());

	public static void register(IEventBus aModBus) {ATTACHMENTS.register(aModBus);}
}
