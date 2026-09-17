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

package gregapi.enchants;

import com.mojang.serialization.MapCodec;

import gregapi.data.CS.SFX;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

import static gregapi.data.CS.RNGSUS;

/** @author Gregorius Techneticies
 *  1:1 transfer of the original effect logic onto the new EnchantmentEntityEffect record mechanism, since
 *  Enchantment can no longer be subclassed; values, thresholds and API replacements are all 1:1. */
public record EnchantmentEffect_Werewolf() implements EnchantmentEntityEffect {
	public static final MapCodec<EnchantmentEffect_Werewolf> CODEC = MapCodec.unit(new EnchantmentEffect_Werewolf());

	@Override
	public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
		if (!(entity instanceof LivingEntity aHurtEntity) || !UT.Entities.isWereCreature(aHurtEntity)) return;
		// Anti Bear Damage now works through the Quantum Suit too, just in a different way. XD
		if (!aHurtEntity.level().isClientSide() && aHurtEntity instanceof Player && "Bear989Sr".equalsIgnoreCase(aHurtEntity.getScoreboardName())) {
			UT.Sounds.send(SFX.MC_FIREWORK_LARGE, aHurtEntity);
			Inventory tInv = ((Player)aHurtEntity).getInventory();
			NonNullList<ItemStack> tMain = tInv.getNonEquipmentItems();
			for (int i = -1; i < enchantmentLevel; i++) {
				int tSlot = RNGSUS.nextInt(tMain.size());
				ItemStack tStack = tMain.get(tSlot);
				if (ST.valid(tStack)) {
					ItemEntity tEntity = ST.drop(aHurtEntity, ST.copy_(tStack));
					if (tEntity != null) {
						tEntity.setPickUpDelay(40);
						tMain.set(tSlot, ItemStack.EMPTY);
					}
				}
			}
		}
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (10*enchantmentLevel) / 7)));
		aHurtEntity.addEffect(new MobEffectInstance(MobEffects.POISON, enchantmentLevel * 200, (int)UT.Code.bind(1, 5, (10*enchantmentLevel) / 7)));
	}

	@Override
	public MapCodec<EnchantmentEffect_Werewolf> codec() {
		return CODEC;
	}
}
