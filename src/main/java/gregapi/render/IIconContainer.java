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

package gregapi.render;

import net.neoforged.api.distmarker.Dist;
import net.minecraft.resources.Identifier;

/** @author Gregorius Techneticies
 *  1.7.10's IIcon/IIconRegister atlas-stitching API is gone entirely; the texture holder is now a plain Identifier,
 *  resolved into a TextureAtlasSprite by the centralized GT6QuadBuilder.resolveSprite. */
public interface IIconContainer {
	/** @return the texture reference for this render pass.
	 *  Was {@code IIcon getIcon(int)} in 1.7.10; {@code IIcon} no longer exists. */
	public Identifier getIcon(int aRenderPass);

	/**
	 * @return if this Render Pass uses Color Modulation.
	 */
	public boolean isUsingColorModulation(int aRenderPass);

	/**
	 * @return the Color Modulation of the Icon.
	 */
	public short[] getIconColor(int aRenderPass);

	/**
	 * @return the Amount of Render Passes for this Icon.
	 */
	public int getIconPasses();

	/**
	 * @return the Default Texture File for this Icon.
	 */
	public Identifier getTextureFile();

	/** Registers the icon of this IconContainer; was registerIcons(IIconRegister) in 1.7.10.
	 *  The parameter is now a neutral holder until bound to the real atlas; implementations should no-op server-side. */
	public void registerIcons(Object aIconRegister);
}
