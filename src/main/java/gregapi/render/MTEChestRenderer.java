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

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import static gregapi.data.CS.*;

/** The chest's special renderer moved out of the common MultiTileEntityChest class, since client-only types
 *  embedded in a common class broke class linking on a dedicated server and aborted MTE registration entirely. */
public class MTEChestRenderer implements BlockEntityRenderer<MultiTileEntityChest, MTEChestRenderer.MTEChestRenderState> {

	private static MTEChestRenderer RENDERER;

	/** Bridge for the client-only onRegistrationFirstClient call, replacing ClientRegistry.bindTileEntitySpecialRenderer
	 *  with the unified GT6 BER's class dispatch. */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, RENDERER = new MTEChestRenderer());
	}

	/** Bridge for onRegistrationClient: registers the .colored/.plain texture pair. */
	public static void bindTexture(String aTextureName, String aRegistryNameInternal) {
		RENDERER.mResources.put(aTextureName, new Identifier[] {Identifier.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".colored.png"), Identifier.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".plain.png")});
	}

	private static final MultiTileEntityModelChest sModel = new MultiTileEntityModelChest();
	public final Map<String, Identifier[]> mResources = new HashMap<>();

	/** Per-frame state of the chest's special render, extracted on the main thread; submit only reads it. */
	public static class MTEChestRenderState extends BlockEntityRenderState {
		public float mLidAngleRad; public byte mChestFacing; public int mChestRGBa; public Identifier[] mChestTextures;
	}

	@Override
	public MTEChestRenderState createRenderState() {
		return new MTEChestRenderState();
	}

	@Override
	public void extractRenderState(MultiTileEntityChest aChest, MTEChestRenderState aState, float aPartialTick, net.minecraft.world.phys.Vec3 aCameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
		BlockEntityRenderer.super.extractRenderState(aChest, aState, aPartialTick, aCameraPos, aBreakProgress);
		// Lid-angle interpolation and cubic easing curve, verbatim from 1.7.10's renderTileEntityAt.
		double tLidAngle = 1 - (aChest.oLidAngle + (aChest.mLidAngle - aChest.oLidAngle) * aPartialTick); tLidAngle = -(((1 - tLidAngle*tLidAngle*tLidAngle) * Math.PI) / 2);
		aState.mLidAngleRad = (float)tLidAngle;
		aState.mChestFacing = aChest.mFacing; // For item-form, facing is set by applyItemFacing so the detached chest displays its lock toward the camera.
		aState.mChestRGBa = aChest.mRGBa;
		aState.mChestTextures = mResources.get(aChest.mTextureName);
	}

	@Override
	public void submit(MTEChestRenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
		Identifier[] tLocation = aState.mChestTextures;
		if (tLocation == null || tLocation.length < 2) return;
		// Matrices are 1:1 with 1.7.10, since the model and textures were authored in its upside-down coordinate system.
		aPoseStack.pushPose();
		aPoseStack.translate(0, 1, 1);
		aPoseStack.scale(1, -1, -1);
		aPoseStack.translate(0.5f, 0.5f, 0.5f);
		aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(COMPASS_FROM_SIDE[aState.mChestFacing] * 90 - 180));
		aPoseStack.translate(-0.5f, -0.5f, -0.5f);
		short[] tRGBa = UT.Code.getRGBaArray(aState.mChestRGBa);
		// Pass 1 draws .colored.png tinted by mRGBa; pass 2 draws .plain.png untinted, as neo's entityCutout.
		sModel.submit(aNodes, aPoseStack, tLocation[0], aState.mLidAngleRad, aState.lightCoords, net.minecraft.util.ARGB.color(255, tRGBa[0], tRGBa[1], tRGBa[2]));
		sModel.submit(aNodes, aPoseStack, tLocation[1], aState.mLidAngleRad, aState.lightCoords, -1);
		aPoseStack.popPose();
	}

	/** Chest model geometry is 1:1 with the 1.7.10 ModelBase version; ModelPart is neo's carrier for the old ModelRenderer. */
	public static class MultiTileEntityModelChest {
		private final net.minecraft.client.model.geom.ModelPart mRoot, mLid, mKnob;

		public MultiTileEntityModelChest() {
			net.minecraft.client.model.geom.builders.MeshDefinition tMesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
			net.minecraft.client.model.geom.builders.PartDefinition tRoot = tMesh.getRoot();
			tRoot.addOrReplaceChild("lid",    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox( 0, -5, -14, 14,  5, 14), net.minecraft.client.model.geom.PartPose.offset(1, 7, 15));
			tRoot.addOrReplaceChild("knob",   net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox(-1, -2, -15,  2,  4,  1), net.minecraft.client.model.geom.PartPose.offset(8, 7, 15));
			tRoot.addOrReplaceChild("bottom", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 19).addBox( 0,  0,   0, 14, 10, 14), net.minecraft.client.model.geom.PartPose.offset(1, 6, 1));
			mRoot = net.minecraft.client.model.geom.builders.LayerDefinition.create(tMesh, 64, 64).bakeRoot();
			mLid  = mRoot.getChild("lid");
			mKnob = mRoot.getChild("knob");
		}

		public void submit(SubmitNodeCollector aNodes, PoseStack aPoseStack, Identifier aTexture, float aLidAngle, int aLight, int aColor) {
			aNodes.submitCustomGeometry(aPoseStack, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(aTexture), (tPose, tBuffer) -> {
				// The lid angle is set inside the deferred draw lambda, since the model instance is shared by every chest;
				// setting it earlier would let the last chest processed overwrite every other chest's angle for that frame.
				mKnob.xRot = mLid.xRot = aLidAngle;
				PoseStack tStack = new PoseStack();
				tStack.mulPose(tPose.pose());
				mRoot.render(tStack, tBuffer, aLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, aColor);
			});
		}
	}
}
