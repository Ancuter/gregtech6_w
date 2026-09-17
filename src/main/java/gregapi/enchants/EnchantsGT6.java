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

import gregapi.data.LH;
import gregapi.data.MD;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The one place in the mod registering this custom EnchantmentEntityEffect type and assembling the
 *  datapack entries for the four GT6 enchantments, since the old override-based dispatch is gone. */
public class EnchantsGT6 {

	/** Central DeferredRegister, the only place in the mod registering custom EnchantmentEntityEffect types. */
	private static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> EFFECT_TYPES =
		DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, MD.GAPI.mID);

	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Werewolf>> WEREWOLF_EFFECT =
		EFFECT_TYPES.register("werewolf", () -> EnchantmentEffect_Werewolf.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Slime>> SLIME_EFFECT =
		EFFECT_TYPES.register("slime", () -> EnchantmentEffect_Slime.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Ender>> ENDER_EFFECT =
		EFFECT_TYPES.register("ender", () -> EnchantmentEffect_Ender.CODEC);
	public static final DeferredHolder<MapCodec<? extends EnchantmentEntityEffect>, MapCodec<EnchantmentEffect_Radioactivity>> RADIOACTIVITY_EFFECT =
		EFFECT_TYPES.register("radioactivity", () -> EnchantmentEffect_Radioactivity.CODEC);

	/** Bootstraps the four GT6 enchantments into the datapack registry; weight/cost/anvilCost have no 1:1
	 *  1.7.10 source, so each gets the nearest legal value, harmless since none are reachable via the table. */
	public static void bootstrap(BootstrapContext<Enchantment> context) {
		HolderGetter<Item> items = context.lookup(Registries.ITEM);

		register(context, Enchantment_WerewolfDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Werewolf())
		);
		LH.add(descriptionId(Enchantment_WerewolfDamage.KEY), "Werebane");

		register(context, Enchantment_SlimeDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Slime())
		);
		LH.add(descriptionId(Enchantment_SlimeDamage.KEY), "Dissolving");

		register(context, Enchantment_EnderDamage.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
						2, 5,
						Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8),
						1,
						EquipmentSlotGroup.MAINHAND
					)
				)
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Ender())
		);
		LH.add(descriptionId(Enchantment_EnderDamage.KEY), "Disjunction");

		register(context, Enchantment_Radioactivity.KEY,
			Enchantment.enchantment(
					Enchantment.definition(
						items.getOrThrow(ItemTags.EQUIPPABLE_ENCHANTABLE),
						1, 5,
						Enchantment.constantCost(Integer.MAX_VALUE), Enchantment.constantCost(0),
						1,
						EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.ARMOR
					)
				)
				// Attacker-side effect: the original applied these enchantments to the attacker's weapon.
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM, new EnchantmentEffect_Radioactivity())
				// Victim-side effect: the original applied this enchantment to armor, self-irradiating the wearer.
				.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.VICTIM, EnchantmentTarget.VICTIM, new EnchantmentEffect_Radioactivity())
		);
		LH.add(descriptionId(Enchantment_Radioactivity.KEY), "Radioactivity");
	}

	/** Computes the translation key the engine's own Enchantment.Builder.build derives, so LH.add registers
	 *  English text under the exact key the engine looks up; the old flat 1.7.10 key is unreachable here. */
	private static String descriptionId(ResourceKey<Enchantment> key) {
		return Util.makeDescriptionId("enchantment", key.identifier());
	}

	private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
		context.register(key, builder.build(key.identifier()));
	}

	/** Central subscription point for effect types, called once beside GT6WorldgenFeature.register. */
	public static void register(IEventBus aModBus) {
		EFFECT_TYPES.register(aModBus);
	}
}
