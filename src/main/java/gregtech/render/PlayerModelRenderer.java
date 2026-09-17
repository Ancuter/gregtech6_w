/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregtech.render;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Collection;

import static gregapi.data.CS.RES_PATH_MODEL;

/** The old immediate-mode player renderer is gone in 26.1.2; this class now holds only cape-selection logic.
 *  The cape is drawn by swapping AvatarRenderState.skin via the engine's own CapeLayer, hooked to RenderPlayerEvent.Pre. */
public class PlayerModelRenderer {
	// neo's Identifier.assertValidPath forbids uppercase paths, so cape texture filenames are lowercased.
	// Files renamed to match; selection order and logic are unchanged.
	private final Identifier[] mResources = new Identifier[] {Identifier.parse(RES_PATH_MODEL + "braintech.png"), Identifier.parse(RES_PATH_MODEL + "silver.png"), Identifier.parse(RES_PATH_MODEL + "mrbrain.png"), Identifier.parse(RES_PATH_MODEL + "dev.png"), Identifier.parse(RES_PATH_MODEL + "gold.png"), Identifier.parse(RES_PATH_MODEL + "crazy.png"), Identifier.parse(RES_PATH_MODEL + "sus.png")};
	private final Collection<String> mSupporterListSilver, mSupporterListGold;

	public PlayerModelRenderer(Collection<String> aSupporterListSilver, Collection<String> aSupporterListGold) {
		mSupporterListSilver = aSupporterListSilver;
		mSupporterListGold   = aSupporterListGold;
	}

	private Identifier getResource(String aPlayer) {
		aPlayer = aPlayer.toLowerCase();
		// I sure as fuck won't make a Microsoft Account!
		if (aPlayer.startsWith("gregori"))            return mResources[6];
		// GT6 Team
		if (aPlayer.equalsIgnoreCase("GregoriusT"))   return mResources[6];
		if (aPlayer.equalsIgnoreCase("OvermindDL1"))  return mResources[3];
		// GT6U Team
		if (aPlayer.equalsIgnoreCase("jihuayu123"))   return mResources[3];
		if (aPlayer.equalsIgnoreCase("Yuesha_Kev14")) return mResources[3];
		if (aPlayer.equalsIgnoreCase("Evanvenir"))    return mResources[3];
		// This "special" Cape is totally just to mess with her. XD
		if (aPlayer.equalsIgnoreCase("CrazyJ1984"))   return mResources[5];
		// People who helped back in ancient GT Versions.
		if (aPlayer.equalsIgnoreCase("Mr_Brain"))     return mResources[2];
		if (aPlayer.equalsIgnoreCase("Friedi4321"))   return mResources[0];
		// Supporter Lists
		if (mSupporterListGold  .contains(aPlayer))   return mResources[4];
		if (mSupporterListSilver.contains(aPlayer))   return mResources[1];
		return null;
	}

	/** GT6's cape layers onto the engine via data, not code: only the texture choice is ours.
	 *  Player identity comes from the entity id in the render state, since the state carries no nickname. */
	public void receiveRenderSpecialsEvent(RenderPlayerEvent.Pre<?> aEvent) {
		try {
			net.minecraft.client.renderer.entity.state.AvatarRenderState tState = aEvent.getRenderState();
			if (tState.skin == null || tState.skin.cape() != null) return;
			net.minecraft.client.multiplayer.ClientLevel tLevel = net.minecraft.client.Minecraft.getInstance().level;
			if (tLevel == null) return;
			if (!(tLevel.getEntity(tState.id) instanceof net.minecraft.world.entity.player.Player tPlayer)) return;
			// Name comes from getScoreboardName(): for a Player entity, that's the profile name.
			Identifier tCape = getResource(tPlayer.getScoreboardName());
			if (tCape == null) tCape = getResource(tPlayer.getUUID().toString());
			if (tCape == null) return;
			// ResourceTexture(id, texturePath) is the 2-arg constructor with a direct path (no auto "textures/…png");
			// mResources already carry the full path, e.g. gregtech:textures/model/<name>.png.
			tState.skin = net.minecraft.world.entity.player.PlayerSkin.insecure(
				tState.skin.body(), new net.minecraft.core.ClientAsset.ResourceTexture(tCape, tCape), tState.skin.elytra(), tState.skin.model());
		} catch (Throwable e) {e.printStackTrace(gregapi.data.CS.ERR);}
	}
}
