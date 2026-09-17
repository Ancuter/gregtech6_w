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

package gregtech.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;


import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import gregapi.data.CS;
import gregapi.data.CS.ToolsGT;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.util.WD;

/** Registers GT6's own ToolHandlers with Jade, since Jade's own vanilla-only handlers can't express tools
 *  like the wrench; source of truth is getHarvestTool and the ToolsGT registry, and this loads only when Jade is present. */
@WailaPlugin
public class Compat_Jade implements IWailaPlugin {
	/** GT6 tool types that actually mine blocks, measured against the registry; the three vanilla ones are left to Jade,
	 *  which already shows them. */
	private static final String[] GT6_TOOL_TYPES = {
		  CS.TOOL_wrench      // every GT6 machine
		, CS.TOOL_crowbar     // rails/frames
		, CS.TOOL_cutter      // wires
		, CS.TOOL_scoop       // hives
		, CS.TOOL_shears      // leaves/wool-like
		, CS.TOOL_sword       // cobwebs and other sword-cuttable things
		, CS.TOOL_saw         // sawable things
		, CS.TOOL_knife       // knife-trimmable things
		, CS.TOOL_hoe         // crop beds
		, CS.TOOL_screwdriver
		, CS.TOOL_hammer
		, CS.TOOL_softhammer
		, CS.TOOL_file
		, CS.TOOL_drill
		, CS.TOOL_chisel
		, CS.TOOL_plunger
	};

	/** Vanilla tool types GT6 must register its own handler for too: Jade judges pickaxe/axe/shovel by vanilla tags, but
	 *  GT6's 0..15 tier scale deliberately omits mineable/* tags above tier 3, leaving Jade's own tooltip section blank. */
	private static final String[] VANILLA_TOOL_TYPES = {CS.TOOL_pickaxe, CS.TOOL_axe, CS.TOOL_shovel};

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		// GT6 tool types have no vanilla handler at all to compete with, since neither the tag nor the item exists.
		for (String tToolType : GT6_TOOL_TYPES   ) HarvestToolProvider.registerHandler(() -> new GT6ToolHandler(tToolType, null));
		// Vanilla types are backed up only where Jade's own handler stayed silent, or the icon would double up.
		for (String tToolType : VANILLA_TOOL_TYPES) HarvestToolProvider.registerHandler(() -> new GT6ToolHandler(tToolType, snownee.jade.api.JadeIds.JADE(tToolType)));
		// The 'required level' and 'in hand' lines that follow don't exist in Jade for any mod, vanilla or otherwise.
		aRegistration.registerBlockComponent(GT6HarvestLevelProvider.INSTANCE, Block.class);
		aRegistration.registerFluidStorageClient(GT6FluidContainerProvider.INSTANCE);
	}

	@Override
	public void register(snownee.jade.api.IWailaCommonRegistration aRegistration) {
		// Showcase-only provider (BUG-088 precedent): small GT6 containers (cups, glasses) interact through
		// taps by design and expose no Fluid.BLOCK capability, so Jade's built-in bak component is silent on
		// them. This feeds the SAME tank the item tooltip prints (mTank) into the standard fluid bar —
		// mechanics untouched. Barrels/canisters are a sibling branch already covered by the capability.
		aRegistration.registerFluidStorage(GT6FluidContainerProvider.INSTANCE, gregapi.tileentity.tank.TileEntityBase08FluidContainer.class);
	}

	public enum GT6FluidContainerProvider implements snownee.jade.api.view.IServerExtensionProvider<snownee.jade.api.view.FluidView.Data>, snownee.jade.api.view.IClientExtensionProvider<snownee.jade.api.view.FluidView.Data, snownee.jade.api.view.FluidView> {
		INSTANCE;

		@Override public Identifier getUid() {return Identifier.fromNamespaceAndPath("gregapi", "fluid_container");}

		@Override
		public java.util.List<snownee.jade.api.view.ViewGroup<snownee.jade.api.view.FluidView.Data>> getGroups(snownee.jade.api.Accessor<?> aAccessor) {
			if (aAccessor instanceof snownee.jade.api.BlockAccessor tBlockAccessor
					&& tBlockAccessor.getBlockEntity() instanceof gregapi.tileentity.tank.TileEntityBase08FluidContainer tContainer)
				return snownee.jade.util.JadeForgeUtils.fromFluidHandler(tContainer.mTank.asResourceHandler());
			return null;
		}

		@Override
		public java.util.List<snownee.jade.api.view.ClientViewGroup<snownee.jade.api.view.FluidView>> getClientGroups(snownee.jade.api.Accessor<?> aAccessor, java.util.List<snownee.jade.api.view.ViewGroup<snownee.jade.api.view.FluidView.Data>> aGroups) {
			return snownee.jade.api.view.ClientViewGroup.map(aGroups, snownee.jade.api.view.FluidView::readDefault, null);
		}
	}

	/** One GT6 tool type; whether it fits a block is decided by the block's own getHarvestTool method. */
	private static class GT6ToolHandler implements ToolHandler {
		private final String mToolType;
		private final Identifier mUID;
		private final Identifier mVanillaUID; // non-null only for vanilla types, whose handler this really is at Jade's own registry
		private List<ItemStack> mTools; // lazy: the tool registry fills in after the plugin loads

		GT6ToolHandler(String aToolType, Identifier aVanillaUID) {
			mToolType = aToolType;
			mVanillaUID = aVanillaUID;
			mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "tool/" + aToolType);
		}

		@Override
		public ItemStack test(BlockState aState, Level aWorld, BlockPos aPos) {
			// Asks each tool whether it mines the block, not the block's single declared type: GT6 tools decide via
			// their own isMinableBlock (a wrench legally mines pistons too), so the old comparison lied about which tools work.
			if (!anyToolOfTypeMines(mToolType, aState, aWorld, aPos)) return ItemStack.EMPTY;
			// On a non-GT6 block, only step in if Jade itself showed nothing at all; adding icons to a display Jade already drew
			// would invade a foreign scale, caught by testing against Grass Block.
			if (!(aState.getBlock() instanceof gregapi.block.IBlock) && vanillaJadeAlreadyShowed(aState, aWorld, aPos)) return ItemStack.EMPTY;
			// Asks Jade's own handler directly rather than guessing from a tag, since a second identical icon would
			// be a duplicate; a tag-based guess copied Jade's own instant-break skip and produced the same blank tooltip by accident.
			if (mVanillaUID != null) {
				ToolHandler tVanilla = HarvestToolProvider.TOOL_HANDLERS.get(mVanillaUID);
				if (tVanilla != null && !tVanilla.test(aState, aWorld, aPos).isEmpty()) return ItemStack.EMPTY;
			}
			List<ItemStack> tTools = getTools();
			return tTools.isEmpty() ? ItemStack.EMPTY : tTools.getFirst();
		}

		@Override
		public List<ItemStack> getTools() {
			if (mTools == null || mTools.isEmpty()) {
				List<ItemStack> tList = new ArrayList<>();
				try {
					for (gregapi.code.ItemStackContainer tContainer : ToolsGT.list(mToolType)) {
						ItemStack tStack = tContainer.toStack();
						if (tStack != null && !tStack.isEmpty()) tList.add(tStack);
					}
				} catch (Throwable e) {/* registry not filled yet, try again next time */}
				mTools = tList;
			}
			return mTools;
		}

		@Override
		public Identifier getUid() {return mUID;}
	}

	/** Did Jade draw this block's showcase on its own? Only its own handlers are asked, since iterating all of them would
	 *  recurse into us. */
	private static boolean vanillaJadeAlreadyShowed(BlockState aState, Level aWorld, BlockPos aPos) {
		for (java.util.Map.Entry<Identifier, ToolHandler> tE : HarvestToolProvider.TOOL_HANDLERS.entrySet()) {
			if (tE.getKey().getNamespace().equals(MD.GT.mID)) continue; // ours, skip it
			try {if (!tE.getValue().test(aState, aWorld, aPos).isEmpty()) return true;} catch (Throwable e) {/* a foreign handler's failure isn't our problem */}
		}
		return false;
	}

	/** Does the block take at least one tool of this type, asked via the tools' own isMinableBlock, the same method GT6 uses
	 *  in-game; the source is the ToolsGT registry, nothing hand-listed. */
	private static boolean anyToolOfTypeMines(String aToolType, BlockState aState, Level aWorld, BlockPos aPos) {
		try {
			int tMeta = gregapi.util.WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
			for (gregapi.code.ItemStackContainer tContainer : ToolsGT.list(aToolType)) {
				ItemStack tStack = tContainer.toStack();
				if (tStack == null || tStack.isEmpty()) continue;
				// Asks the tool itself through its own method, rather than reaching into its IToolStats from outside.
				if (tStack.getItem() instanceof gregapi.item.multiitem.MultiItemTool tTool
				 && tTool.isMinableBlock(tStack, aState.getBlock(), (byte)tMeta)) return true;
			}
		} catch (Throwable e) {/* registry not filled yet, or no world */}
		return false;
	}

	/** The one line Jade has for no mod at all: required tool tier on GT6's 0..15 scale, printed as a word
	 *  through the same central LH table item tooltips use; omits the tool name and match icon, since Jade shows those already. */
	public enum GT6HarvestLevelProvider implements IBlockComponentProvider {
		INSTANCE;

		private final Identifier mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "harvest_level");

		// Jade's config screen requires a translation for every plugin option and crashes if one is missing; both the option key
		// and the section-header key are supplied through the mod's own central localization, like every other GT6 name.
		static {
			LH.add("config.jade.plugin_" + MD.GT.mID, "GregTech");
			LH.add("config.jade.plugin_" + MD.GT.mID + ".harvest_level", "Harvest Level");
		}

		@Override
		public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
			Block tBlock = aAccessor.getBlockState().getBlock();
			Level tWorld = aAccessor.getLevel();
			BlockPos tPos = aAccessor.getPosition();
			// Jade exits before drawing when break speed is zero, which a wrong GT6 tool genuinely gives (confirmed
			// by a paired measurement against 1.7.10); draws the same tool list Jade already knows, only where its own display can't.
			try {
				if (aAccessor.getBlockState().getDestroyProgress(aAccessor.getPlayer(), tWorld, tPos) <= 0) {
					java.util.List<snownee.jade.api.ui.Element> tTools = HarvestToolProvider.INSTANCE.getText(aAccessor, aConfig);
					if (!tTools.isEmpty()) aTooltip.add(tTools);
				}
			} catch (Throwable e) {/* Jade's own showcase is unavailable; the tier line below still appears */}
			try {
				int tNeeded = WD.harvestLevel(tWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				// Show condition is whether the tool is known, not whether it's required, since materials like sand or dirt need no tool
				// yet still have one assigned at tier 0; silence is reserved for blocks with neither a known tool nor a requirement.
				String tTool = WD.harvestTool(tBlock, WD.meta(tWorld, tPos.getX(), tPos.getY(), tPos.getZ()));
				if ((tTool == null || tTool.isEmpty()) && WD.getMaterial(tBlock).isToolNotRequired()) return;
				// No more filtering by block origin: the old exit for non-GT6 blocks left vanilla ones blank, a half
				// answer; WD now knows every block's tier from the 1.7.10 passport, so all blocks are asked identically now.
				if (tNeeded < 0) return;
				String tName = LH.getHarvestLevelMaterial(tNeeded);
				aTooltip.add(Component.literal(LH.Chat.DGRAY + LH.get(LH.TOOL_HARVEST_TIER, "Requires Tier") + ": " + LH.Chat.WHITE
					+ tNeeded + (tName.isEmpty() ? "" : " (" + tName + ")")));
			} catch (Throwable e) {/* no world or data yet; the line simply won't appear */}
		}

		@Override
		public Identifier getUid() {return mUID;}
	}
}
