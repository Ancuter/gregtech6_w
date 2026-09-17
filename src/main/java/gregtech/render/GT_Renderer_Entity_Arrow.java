/**
 * Copyright (c) 2019 Gregorius Techneticies
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

import static gregapi.data.CS.*;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.resources.Identifier;

/** 1.7.10's RenderArrow.getEntityTexture(Arrow) took the entity instance; neo's getTextureLocation takes render state.
 *  Both return the same mTexture holder unconditionally. */
public class GT_Renderer_Entity_Arrow extends ArrowRenderer<Arrow, ArrowRenderState> {
	private final Identifier mTexture;

	// Real EntityRendererProvider.Context: the renderer builds in EntityRenderersEvent.RegisterRenderers.
	// 1.7.10's RenderingRegistry handler is gone; registration is now keyed by EntityType.
	public GT_Renderer_Entity_Arrow(EntityRendererProvider.Context aContext, String aTextureName) {
		super(aContext);
		mTexture = Identifier.parse(RES_PATH_ENTITY+aTextureName+".png");
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState aState) {
		return mTexture;
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}
}
