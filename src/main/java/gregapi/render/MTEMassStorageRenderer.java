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

package gregapi.render;

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import static gregapi.data.CS.*;

/** Mass-storage's special renderer is pulled out for the same reason and technique as {@link MTEChestRenderer}. */
public class MTEMassStorageRenderer implements BlockEntityRenderer<MultiTileEntityMassStorage, MTEMassStorageRenderer.MTEMassStorageRenderState> {
	public static MTEMassStorageRenderer INSTANCE = new MTEMassStorageRenderer();

	/** Bridge for the client-only onRegistrationFirstClient call. */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, INSTANCE);
	}

	/** Per-frame render state, extracted on the main thread; submit only reads it. */
	public static class MTEMassStorageRenderState extends BlockEntityRenderState {
		public net.minecraft.client.renderer.item.ItemStackRenderState mItem; public byte mStorageFacing;
	}

	@Override
	public MTEMassStorageRenderState createRenderState() {
		return new MTEMassStorageRenderState();
	}

	@Override
	public void extractRenderState(MultiTileEntityMassStorage aStorage, MTEMassStorageRenderState aState, float aPartialTick, net.minecraft.world.phys.Vec3 aCameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aStorage, aState, aPartialTick, aCameraPos, aBreakProgress);
		aState.mItem = null;
		if (!aStorage.slotHas(1) || !aStorage.isFaceVisible()) return;
		aState.mStorageFacing = aStorage.mFacing; // For item-form, facing is already set by the central applyItemFacing that calibrates the display item's orientation.
		// GUI display context (not FIXED) uses the inventory icon transform, matching 1.7.10's JEI/creative look.
		aState.mItem = new net.minecraft.client.renderer.item.ItemStackRenderState();
		net.minecraft.client.Minecraft.getInstance().getItemModelResolver().updateForTopItem(aState.mItem, aStorage.slot(1), net.minecraft.world.item.ItemDisplayContext.GUI, aStorage.getLevel(), null, 0);
	}

	@Override
	public void submit(MTEMassStorageRenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
		if (aState.mItem == null) return;
		byte tFacing = aState.mStorageFacing;
		// 1.7.10 drew the GUI item from its corner, so the translation point was offset to compensate; neo's FIXED
		// context draws the model already centered, so translating straight to that same 1.7.10 center point is needed instead.
		aPoseStack.pushPose();
		aPoseStack.translate(0.5 + OFFX[tFacing]*0.502, 0.375, 0.5 + OFFZ[tFacing]*0.502);
		// 1.7.10's y-down GUI render needed a compensating 180-degree Z rotation that neo's y-up model doesn't; the
		// correct per-side angle from +Z to the display face is -toYRot(), not the old formula that mirrored north/south.
		net.minecraft.core.Direction tDir = net.minecraft.core.Direction.from3DDataValue(tFacing);
		if (tDir.getAxis().isHorizontal()) {
			aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-tDir.toYRot()));
		} else {
			aPoseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90 * tDir.getAxisDirection().getStep()));
		}
		// 1.7.10's external display chain mirrored blocks and flat items differently along Z; the port dropped that sign
		// for flat items only, mirroring icons; it's restored as a Y-axis rotation, not a scale flip, so winding stays intact.
		if (MASSSTORAGE_DISPLAY_YAW != 0) aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(MASSSTORAGE_DISPLAY_YAW));
		aPoseStack.scale(0.5f, 0.5f, 0.0001f);
		aState.mItem.submit(aPoseStack, aNodes, 0xF000F0 /* fullbright 240/240, matching the original setLightmapTextureCoords. */, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
		aPoseStack.popPose();
	}
}
