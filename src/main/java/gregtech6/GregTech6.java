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

package gregtech6;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import gregapi.network.NetworkHandler;
import org.slf4j.Logger;

/** Kept only because the declared modId ("gregtech6") needs a live entry point and SanityTest references MODID.
 *  All Item/Block/Fluid registration now goes through the centers GT_API and FluidGT; nothing registers here. */
// Temporary @Mod carrier for modId gregtech6, removed once GT6_Main becomes the real @Mod(GT).
// neoforge.mods.toml needs a live entrypoint already at build time, before content registration moves there.
@Mod(GregTech6.MODID)
public class GregTech6 {
    public static final String MODID = "gregtech6";

    public static final Logger LOGGER = LogUtils.getLogger();

    public GregTech6(IEventBus modEventBus) {
        // Network payload registration isn't content registration, so it's left as-is here.
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);
        LOGGER.info("[GregTech6] entrypoint loaded — content registration centralised in GT_API (F12) / FluidGT (F5)");
    }
}
