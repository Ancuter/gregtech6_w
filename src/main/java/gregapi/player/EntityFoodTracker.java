/**
 * Copyright (c) 2023 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.player;

import gregapi.code.ArrayListNoNulls;
import gregapi.damage.DamageSources;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import static gregapi.data.CS.*;

/** The removed IExtendedEntityProperties system is replaced by neo's data-attachment API; this class is the
 *  stored data itself, so it registers as an AttachmentType via the central DeferredRegister instead. */
public class EntityFoodTracker {
	public static ArrayListNoNulls<EntityFoodTracker> TICK_LIST = new ArrayListNoNulls<>();

	public byte mAlcohol = 0, mCaffeine = 0, mDehydration = 0, mSugar = 0, mFat = 0, mRadiation = 0;
	public final LivingEntity mEntity;

	/** The attachment contract already scopes the write to this type's own key, so the old manual wrapper tag
	 *  is no longer needed; returning false means don't serialize, matching the original's empty-tag removal. */
	private static final IAttachmentSerializer<EntityFoodTracker> SERIALIZER = new IAttachmentSerializer<EntityFoodTracker>() {
		@Override
		public EntityFoodTracker read(IAttachmentHolder aHolder, ValueInput aInput) {
			EntityFoodTracker rTracker = new EntityFoodTracker((LivingEntity)aHolder);
			rTracker.loadNBTData(aInput);
			return rTracker;
		}
		@Override
		public boolean write(EntityFoodTracker aTracker, ValueOutput aOutput) {
			return aTracker.saveNBTData(aOutput);
		}
	};

	/** Central DeferredRegister, the only place GT6 registers entity attachment types in neo. */
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MD.GAPI.mID);

	/** Both the fresh-entity path and the loaded-from-disk path go through this same constructor, which
	 *  registers itself in the tick list, so either path adds the instance exactly once. */
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntityFoodTracker>> TYPE = ATTACHMENTS.register("food_tracker",
		() -> AttachmentType.<EntityFoodTracker>builder(aHolder -> new EntityFoodTracker((LivingEntity)aHolder)).serialize(SERIALIZER).build());

	public EntityFoodTracker(LivingEntity aEntity) {
		mEntity = aEntity;
		// neo's AttachmentType has no automatic post-construction hook like 1.7.10 had, so this is called
		// explicitly to make sure every tracker instance reaches the tick list exactly once.
		init(aEntity, aEntity.level());
	}

	/** See IAttachmentSerializer.write() above: false means do not serialize, matching the original's tag removal. */
	public boolean saveNBTData(ValueOutput aNBT) {
		boolean rAny = F;
		if (mAlcohol     != 0) {aNBT.putByte("a", mAlcohol    ); rAny = T;}
		if (mCaffeine    != 0) {aNBT.putByte("c", mCaffeine   ); rAny = T;}
		if (mSugar       != 0) {aNBT.putByte("s", mSugar      ); rAny = T;}
		if (mDehydration != 0) {aNBT.putByte("d", mDehydration); rAny = T;}
		if (mFat         != 0) {aNBT.putByte("f", mFat        ); rAny = T;}
		if (mRadiation   != 0) {aNBT.putByte("r", mRadiation  ); rAny = T;}
		return rAny;
	}

	public void loadNBTData(ValueInput aNBT) {
		mAlcohol     = aNBT.getByteOr("a", (byte)0);
		mCaffeine    = aNBT.getByteOr("c", (byte)0);
		mDehydration = aNBT.getByteOr("d", (byte)0);
		mSugar       = aNBT.getByteOr("s", (byte)0);
		mFat         = aNBT.getByteOr("f", (byte)0);
		mRadiation   = aNBT.getByteOr("r", (byte)0);
	}

	public void init(Entity aEntity, Level aWorld) {TICK_LIST.add(this);}
	public void changeAlcohol    (long aAmount) {mAlcohol     = UT.Code.bind7(mAlcohol     + aAmount);}
	public void changeCaffeine   (long aAmount) {mCaffeine    = UT.Code.bind7(mCaffeine    + aAmount);}
	public void changeDehydration(long aAmount) {mDehydration = UT.Code.bind7(mDehydration + aAmount);}
	public void changeSugar      (long aAmount) {mSugar       = UT.Code.bind7(mSugar       + aAmount);}
	public void changeFat        (long aAmount) {mFat         = UT.Code.bind7(mFat         + aAmount);}
	public void changeRadiation  (long aAmount) {mRadiation   = UT.Code.bind7(mRadiation   + aAmount);}

	public static void tick() {
		if (SERVER_TIME % 50 == 0) for (int i = 0; i < TICK_LIST.size(); i++) {
			EntityFoodTracker tTracker = TICK_LIST.get(i);
			// Attachment deserialization replaces the whole stored value after construction, so a fresh instance can
			// be replaced by a disk-loaded one before it ticks; without this check the orphaned instance keeps ticking stale.
			if (tTracker.mEntity.isRemoved() || get(tTracker.mEntity) != tTracker) {TICK_LIST.remove(i--); continue;}

			if (tTracker.mAlcohol >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.hurt(DamageSources.getAlcoholDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.STRENGTH, 300, 3, F);
			} else if (tTracker.mAlcohol >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.STRENGTH, 300, 2, F);
			} else if (tTracker.mAlcohol >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.STRENGTH, 300, 1, F);
			} else if (tTracker.mAlcohol >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.STRENGTH, 300, 0, F);
			}

			if (tTracker.mCaffeine >= 100) {
				if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
				tTracker.mEntity.hurt(DamageSources.getCaffeineDamage(), FOOD_OVERDOSE_DEATH?2:1);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HASTE, 300, 3, F);
			} else if (tTracker.mCaffeine >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HASTE, 300, 2, F);
			} else if (tTracker.mCaffeine >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 1200, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HASTE, 300, 1, F);
			} else if (tTracker.mCaffeine >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HASTE, 300, 0, F);
			}

			if (tTracker.mRadiation >= 100) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_WITHER, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 100, 2, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 2, F);
			} else if (tTracker.mRadiation >= 75) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 100, 1, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 1, F);
			} else if (tTracker.mRadiation >= 50) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.NAUSEA, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.HUNGER, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 100, 0, F);
				UT.Entities.applyPotion(tTracker.mEntity, MobEffects.WEAKNESS, 100, 0, F);
			} else if (tTracker.mRadiation >= 25) {
				UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_RADIATION >= 0 ? PotionsGT.ID_RADIATION : UT.Entities.POTID_POISON, 100, 0, F);
			}

			if (NUTRITION_SYSTEM) {
				if (tTracker.mFat >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getFatDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.RESISTANCE, 300, 3, F);
				} else if (tTracker.mFat >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.RESISTANCE, 300, 2, F);
				} else if (tTracker.mFat >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SLOWNESS, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.RESISTANCE, 300, 1, F);
				} else if (tTracker.mFat >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.RESISTANCE, 300, 0, F);
				}

				if (tTracker.mSugar >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getSugarDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 1200, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SPEED, 300, 3, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP_BOOST, 300, 3, F);
				} else if (tTracker.mSugar >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 1200, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SPEED, 300, 2, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP_BOOST, 300, 2, F);
				} else if (tTracker.mSugar >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.MINING_FATIGUE, 1200, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SPEED, 300, 1, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP_BOOST, 300, 1, F);
				} else if (tTracker.mSugar >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.SPEED, 300, 0, F);
					UT.Entities.applyPotion(tTracker.mEntity, MobEffects.JUMP_BOOST, 300, 0, F);
				}

				if (tTracker.mDehydration >= 100) {
					if (FOOD_OVERDOSE_DEATH || tTracker.mEntity.getHealth() >= 2)
					tTracker.mEntity.hurt(DamageSources.getDehydrationDamage(), FOOD_OVERDOSE_DEATH?2:1);
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 3, F);
				} else if (tTracker.mDehydration >= 75) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 2, F);
				} else if (tTracker.mDehydration >= 50) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 1, F);
				} else if (tTracker.mDehydration >= 25) {
					UT.Entities.applyPotion(tTracker.mEntity, PotionsGT.ID_DEHYDRATION >= 0 ? PotionsGT.ID_DEHYDRATION : UT.Entities.POTID_HUNGER, 1200, 0, F);
				}
			}

			if (SERVER_TIME % 100 == 0) {
				if (tTracker.mAlcohol     > 0) tTracker.mAlcohol--;
				if (tTracker.mCaffeine    > 0) tTracker.mCaffeine--;
				if (tTracker.mDehydration > 0) tTracker.mDehydration--;
				if (tTracker.mSugar       > 0) tTracker.mSugar--;
				if (tTracker.mFat         > 0) tTracker.mFat--;
				//if (tTracker.mRadiation > 0) tTracker.mRadiation--; // The only one that does not decrease, so you will have to deal with it until you either die or get a Radaway,
			}
		}
	}

	public static void add(LivingEntity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return;
		// On a freshly constructed entity the attachment map is empty, so getData() always falls through to the
		// default supplier and builds a new tracker; this is a create point, not a read point, on purpose.
		aEntity.getData(TYPE.get());
	}

	public static EntityFoodTracker get(Entity aEntity) {
		if (aEntity == null || aEntity.level().isClientSide()) return null;
		// Now statically typed to EntityFoodTracker by its own key, so the old instanceof check is gone; it returns
		// null rather than creating an entry, the same semantics as before for entities that never called add().
		return aEntity.getExistingDataOrNull(TYPE.get());
	}
}
