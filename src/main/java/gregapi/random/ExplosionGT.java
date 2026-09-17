/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.random;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/** neo's Explosion became a pure interface, but the engine's event hooks require the concrete ServerExplosion
 *  type, so this extends that class instead; every 1.7.10 field is reproduced since the parent keeps them private. */
public class ExplosionGT extends ServerExplosion {
	public static ExplosionGT explode(Level aWorld, Entity aEntity, double aX, double aY, double aZ, float aPower, boolean aFlaming, boolean aSmoking) {
		ExplosionGT tExplosion = new ExplosionGT(aWorld, aEntity, aX, aY, aZ, aPower);
		tExplosion.isFlaming = aFlaming;
		tExplosion.isSmoking = aSmoking;
		if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(aWorld, tExplosion)) return tExplosion;
		tExplosion.doExplosionA();
		if (aWorld instanceof ServerLevel) {
			tExplosion.doExplosionB(F);
			if (!aSmoking) tExplosion.affectedBlockPositions.clear();
			tExplosion.sendExplosionPacket();
		} else {
			tExplosion.doExplosionB(T);
		}
		return tExplosion;
	}

	// Dispatches audio/visual explosion effects and client knockback via packet.
	public void sendExplosionPacket() {
		if (!(mWorld instanceof ServerLevel)) return;
		int tBlockCount = affectedBlockPositions.size();
		ParticleOptions tParticle = explosionSize >= 2 && isSmoking ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION;
		Holder<SoundEvent> tSound = SoundEvents.GENERIC_EXPLODE;
		WeightedList<ExplosionParticleInfo> tBlockParticles = WeightedList.of();
		Vec3 tCenter = new Vec3(explosionX, explosionY, explosionZ);
		@SuppressWarnings("rawtypes")
		Iterator tIterator = mWorld.players().iterator();
		while (tIterator.hasNext()) {
			Player tPlayer = (Player)tIterator.next();
			if (tPlayer.distanceToSqr(explosionX, explosionY, explosionZ) < 4096) {
				((ServerPlayer)tPlayer).connection.send(new ClientboundExplodePacket(tCenter, explosionSize, tBlockCount, Optional.ofNullable((Vec3)func_77277_b().get(tPlayer)), tParticle, tSound, tBlockParticles));
			}
		}
	}

	public ExplosionGT(Level aWorld, Entity aEntity, double aX, double aY, double aZ, float aPower) {
		// neo explosions are server-authoritative by design, synced to the client by packet; the server-side cast
		// is safe since every GT6 explosion caller is already server-side.
		super((ServerLevel)aWorld, aEntity, null, null, new Vec3(aX, aY, aZ), aPower, F, Explosion.BlockInteraction.DESTROY);
		mWorld = aWorld;
		explosionX = aX; explosionY = aY; explosionZ = aZ;
		explosionSize = aPower;
		exploder = aEntity;
		isSmoking = true;
	}

	// Made protected (was private) so GT6 subclasses like DynamiteExplosion can reuse this instead of duplicating it.
	protected Level mWorld;
	protected final double explosionX, explosionY, explosionZ;
	protected final float explosionSize;
	protected final Entity exploder;
	protected boolean isFlaming = false, isSmoking = true;
	protected final List<BlockPos> affectedBlockPositions = new ArrayList<>();
	@SuppressWarnings("rawtypes")
	private Map field_77288_k = new HashMap<>();

	@SuppressWarnings("unchecked")
	public void doExplosionA() {
		float tSize = explosionSize;
		HashSet<BlockPos> tPositions = new HashSet<>();
		for (int i = 0; i < 16; ++i) for (int j = 0; j < 16; ++j) for (int k = 0; k < 16; ++k) {
			if (i == 0 || i == 15 || j == 0 || j == 15 || k == 0 || k == 15) {
				double tIncX = i / 7.5F - 1, tIncY = j / 7.5F - 1, tIncZ = k / 7.5F - 1;
				double tDist = Math.sqrt(tIncX * tIncX + tIncY * tIncY + tIncZ * tIncZ);
				tIncX /= tDist; tIncY /= tDist; tIncZ /= tDist;
				float tPow = tSize * (0.7F + mWorld.getRandom().nextFloat() * 0.6F);
				double tX = explosionX, tY = explosionY, tZ = explosionZ;
				for (float tMul = 0.3F; tPow > 0; tPow -= tMul * 0.75F) {
					int tFloorX = UT.Code.roundDown(tX), tFloorY = UT.Code.roundDown(tY), tFloorZ = UT.Code.roundDown(tZ);
					BlockPos tPos = new BlockPos(tFloorX, tFloorY, tFloorZ);
					Block tBlock = WD.block(mWorld, tFloorX, tFloorY, tFloorZ);
					BlockState tState = gregapi.util.WD.state(mWorld, tPos);
					if (WD.getMaterial(tBlock) != Material.air) {
						float tBaseResistance = tBlock.getExplosionResistance(tState, mWorld, tPos, this);
						float f3 = exploder != null ? exploder.getBlockExplosionResistance(this, mWorld, tPos, tState, tState.getFluidState(), tBaseResistance) : tBaseResistance;
						tPow -= (f3 + 0.3F) * tMul;
					}
					if (tPow > 0 && (exploder == null || exploder.shouldBlockExplode(this, mWorld, tPos, tState, tPow))) {
						tPositions.add(tPos);
					}
					tX += tIncX * tMul; tY += tIncY * tMul; tZ += tIncZ * tMul;
				}
			}
		}
		affectedBlockPositions.addAll(tPositions);
		tSize *= 2;
		@SuppressWarnings("rawtypes")
		List tEntities = mWorld.getEntities(exploder, new AABB(UT.Code.roundDown(explosionX - tSize - 1), UT.Code.roundDown(explosionY - tSize - 1), UT.Code.roundDown(explosionZ - tSize - 1), UT.Code.roundDown(explosionX + tSize + 1), UT.Code.roundDown(explosionY + tSize + 1), UT.Code.roundDown(explosionZ + tSize + 1)));
		net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(mWorld, this, tEntities, affectedBlockPositions);
		Vec3 tVec3 = new Vec3(explosionX, explosionY, explosionZ);
		for (int i1 = 0; i1 < tEntities.size(); ++i1) {
			Entity tEntity = (Entity)tEntities.get(i1);
			double tEntityDist = Math.sqrt(tEntity.distanceToSqr(explosionX, explosionY, explosionZ)) / tSize;
			if (tEntityDist <= 1 && !(tEntity instanceof WitherBoss || tEntity instanceof EnderDragon || tEntity instanceof EnderDragonPart || tEntity.getClass().getName().toLowerCase().contains("boss"))) {
				double tKnockX = tEntity.getX() - explosionX, tKnockY = tEntity.getY() + tEntity.getEyeHeight() - explosionY, tKnockZ = tEntity.getZ() - explosionZ;
				double tDist = Mth.sqrt((float)(tKnockX * tKnockX + tKnockY * tKnockY + tKnockZ * tKnockZ));
				if (tDist > 0) {
					tKnockX /= tDist;
					tKnockY /= tDist;
					tKnockZ /= tDist;
					double tKnockback = (1 - tEntityDist) * getSeenPercent(tVec3, tEntity);
					tEntity.hurt(mWorld.damageSources().explosion(this), ((int)((tKnockback * tKnockback + tKnockback) * 4 * tSize + 1)) * TFC_DAMAGE_MULTIPLIER);
					double tKnockbackResistance = tEntity instanceof LivingEntity ? ((LivingEntity)tEntity).getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) : 0.0;
					double tBlastProtection = tKnockback * (1.0 - tKnockbackResistance);
					Vec3 tOldMotion = tEntity.getDeltaMovement();
					tEntity.setDeltaMovement(tOldMotion.x + tKnockX * tBlastProtection, tOldMotion.y + tKnockY * tBlastProtection, tOldMotion.z + tKnockZ * tBlastProtection);

					if (tEntity instanceof Player) field_77288_k.put(tEntity, new Vec3(tKnockX * tKnockback, tKnockY * tKnockback, tKnockZ * tKnockback));
				}
			}
		}
	}

	public void doExplosionB(boolean aEffects) {
		// Explosion sound and particles now reach the client through the packet above, which the client renders
		// itself; the old manual string-keyed sound/particle calls are redundant with it, not a stub.
		if (isSmoking) {
			@SuppressWarnings("rawtypes")
			Iterator tIterator = affectedBlockPositions.iterator();
			while (tIterator.hasNext()) {
				final BlockPos tPos = (BlockPos)tIterator.next();
				final Block tBlock = WD.block(mWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				if (aEffects) {
					double d0 = (tPos.getX() + mWorld.getRandom().nextFloat());
					double d1 = (tPos.getY() + mWorld.getRandom().nextFloat());
					double d2 = (tPos.getZ() + mWorld.getRandom().nextFloat());
					double d3 = d0 - explosionX;
					double d4 = d1 - explosionY;
					double d5 = d2 - explosionZ;
					double d6 = Mth.sqrt((float)(d3 * d3 + d4 * d4 + d5 * d5));
					d3 /= d6;
					d4 /= d6;
					d5 /= d6;
					double d7 = 0.5D / (d6 / explosionSize + 0.1D);
					d7 *= (mWorld.getRandom().nextFloat() * mWorld.getRandom().nextFloat() + 0.3F);
					d3 *= d7;
					d4 *= d7;
					d5 *= d7;
					// Per-block particles are already covered by the explosion packet noted on the method above; not a stub.
				}
				if (WD.getMaterial(tBlock) != Material.air) {
					BlockState tState = gregapi.util.WD.state(mWorld, tPos);
					// Block drops from the explosion now go through the engine's loot-table path at the same chance the
					// original used; the per-stack loot model does not distribute multi-item drops identically, an engine-forced difference.
					if (tBlock.canDropFromExplosion(tState, mWorld, tPos, this) && mWorld.getRandom().nextFloat() < 1 / explosionSize) Block.dropResources(tState, mWorld, tPos, mWorld.getBlockEntity(tPos));
					if (mWorld instanceof ServerLevel tServerLevel) tBlock.onBlockExploded(tState, tServerLevel, tPos, this);
				}
			}
		}
		if (isFlaming) {
			@SuppressWarnings("rawtypes")
			Iterator tIterator = affectedBlockPositions.iterator();
			while (tIterator.hasNext()) {
				final BlockPos tPos = (BlockPos)tIterator.next();
				final Block tBlock = WD.block(mWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				final BlockState tAboveState = gregapi.util.WD.state(mWorld, new BlockPos(tPos.getX(), tPos.getY() - 1, tPos.getZ()));
				if (WD.getMaterial(tBlock) == Material.air && tAboveState.isSolidRender() && RNGSUS.nextInt(3) == 0) {
					WD.set(mWorld, tPos.getX(), tPos.getY(), tPos.getZ(), Blocks.FIRE, 0, 3);
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public Map func_77277_b() {return field_77288_k;}
	public LivingEntity getExplosivePlacedBy() {return exploder == null ? null : (exploder instanceof PrimedTnt ? ((PrimedTnt)exploder).getOwner() : (exploder instanceof LivingEntity ? (LivingEntity)exploder : null));}
}
