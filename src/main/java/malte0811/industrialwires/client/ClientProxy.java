/*
 * This file is part of Industrial Wires.
 * Copyright (C) 2016-2018 malte0811
 * Industrial Wires is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Industrial Wires is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Industrial Wires.  If not, see <http://www.gnu.org/licenses/>.
 */
package malte0811.industrialwires.client;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.Config;
import blusunrize.lib.manual.IManualPage;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.ManualPages;
import blusunrize.lib.manual.ManualPages.PositionedItemStack;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import malte0811.industrialwires.*;
import malte0811.industrialwires.blocks.controlpanel.BlockTypes_Panel;
import malte0811.industrialwires.blocks.controlpanel.TileEntityPanelCreator;
import malte0811.industrialwires.blocks.controlpanel.TileEntityRSPanel;
import malte0811.industrialwires.client.gui.GuiPanelComponent;
import malte0811.industrialwires.client.gui.GuiPanelCreator;
import malte0811.industrialwires.client.gui.GuiRSPanelConn;
import malte0811.industrialwires.client.gui.GuiRenameKey;
import malte0811.industrialwires.client.manual.TextSplitter;
import malte0811.industrialwires.client.multiblock_io_model.MBIOModelLoader;
import malte0811.industrialwires.client.panelmodel.PanelModelLoader;
import malte0811.industrialwires.compat.Compat;
import malte0811.industrialwires.controlpanel.PanelComponent;
import malte0811.industrialwires.items.ItemPanelComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	@Override
	public void preInit() {
		super.preInit();
		OBJLoader.INSTANCE.addDomain(IndustrialWires.MODID);
		ModelLoaderRegistry.registerLoader(new PanelModelLoader());
		ModelLoaderRegistry.registerLoader(new MBIOModelLoader());
	}

	@Override
	public void postInit() {
		super.postInit();
		ManualInstance m = ManualHelper.getManual();
		boolean uni = m.fontRenderer.getUnicodeFlag();
		m.fontRenderer.setUnicodeFlag(true);
		m.entryRenderPre();
		TextSplitter splitter;
		{
			PositionedItemStack[][] wireRecipes = new PositionedItemStack[3][10];
			int xBase = 15;
			Object2IntMap<ItemStack> copperCables = new Object2IntLinkedOpenHashMap<>();
			copperCables.put(new ItemStack(IEObjects.itemWireCoil, 1, 0), 8);
			List<ItemStack> copperCableList = new ArrayList<>(copperCables.keySet());
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					wireRecipes[0][3 * i + j] = new PositionedItemStack(copperCableList, 18 * i + xBase, 18 * j);
				}
			}

			splitter = new TextSplitter(m);
			splitter.addSpecialPage(0, 0, 10,
					s->new ManualPages.CraftingMulti(m, s, (Object[]) wireRecipes));
			String text = I18n.format("ie.manual.entry.industrialwires.wires");
			splitter.split(text);
			List<IManualPage> entry = splitter.toManualEntry();
			m.addEntry("industrialwires.wires", IndustrialWires.MODID, entry.toArray(new IManualPage[0]));
		}

		ClientUtils.mc().getItemColors().registerItemColorHandler((stack, pass) -> {
			if (pass == 1) {
				PanelComponent pc = ItemPanelComponent.componentFromStack(stack);
				if (pc != null) {
					return 0xff000000 | pc.getColor();
				}
			}
			return ~0;
		}, IndustrialWires.panelComponent);

		Config.manual_int.put("iwKeysOnRing", IWConfig.maxKeysOnRing);

		String text = I18n.format("ie.manual.entry.industrialwires.intro");
		splitter = new TextSplitter(m);
		splitter.addSpecialPage(0, 0, 9, s -> new ManualPages.Crafting(m, s,
				new ItemStack(IndustrialWires.panel, 1, BlockTypes_Panel.DUMMY.ordinal())));
		splitter.addSpecialPage(1, 0, 9, s -> new ManualPages.Crafting(m, s,
				new ItemStack(IndustrialWires.panel, 1, BlockTypes_Panel.UNFINISHED.ordinal())));
		splitter.split(text);
		m.addEntry("industrialwires.intro", "control_panels",
				splitter.toManualEntry().toArray(new IManualPage[0])
		);
		m.addEntry("industrialwires.panel_creator", "control_panels",
				new ManualPages.Crafting(m, "industrialwires.panel_creator0", new ItemStack(IndustrialWires.panel, 1, BlockTypes_Panel.CREATOR.ordinal())),
				new ManualPages.Text(m, "industrialwires.panel_creator1"),
				new ManualPages.Text(m, "industrialwires.panel_creator2")
		);
		text = I18n.format("ie.manual.entry.industrialwires.redstone");
		splitter = new TextSplitter(m);
		splitter.addSpecialPage(-1, 0, Compat.enableOtherRS ? 9 : 12, s -> new ManualPages.CraftingMulti(m, s,
				new ResourceLocation(IndustrialWires.MODID, "control_panel_rs_other"),
				new ResourceLocation(IndustrialWires.MODID, "control_panel_rs_wire")));
		splitter.split(text);
		m.addEntry("industrialwires.redstone", "control_panels",
				splitter.toManualEntry().toArray(new IManualPage[0])
		);
		m.addEntry("industrialwires.components", "control_panels",
				new ManualPages.Text(m, "industrialwires.components.general"),
				new ManualPages.Crafting(m, "industrialwires.button", new ItemStack(IndustrialWires.panelComponent, 1, 0)),
				new ManualPages.Crafting(m, "industrialwires.label", new ItemStack(IndustrialWires.panelComponent, 1, 1)),
				new ManualPages.Crafting(m, "industrialwires.indicator_light", new ItemStack(IndustrialWires.panelComponent, 1, 2)),
				new ManualPages.Crafting(m, "industrialwires.slider", new ItemStack(IndustrialWires.panelComponent, 1, 3)),
				new ManualPages.CraftingMulti(m, "industrialwires.toggle_switch", new ItemStack(IndustrialWires.panelComponent, 1, 5), new ItemStack(IndustrialWires.panelComponent, 1, 6)),
				new ManualPages.Text(m, "industrialwires.toggle_switch1"),
				new ManualPages.Crafting(m, "industrialwires.variac", new ItemStack(IndustrialWires.panelComponent, 1, 4)),
				new ManualPages.CraftingMulti(m, "industrialwires.lock", new ItemStack(IndustrialWires.panelComponent, 1, 7), new ItemStack(IndustrialWires.key)),
				new ManualPages.Crafting(m, "industrialwires.lock1", new ItemStack(IndustrialWires.key, 1, 2)),
				new ManualPages.Crafting(m, "industrialwires.panel_meter", new ItemStack(IndustrialWires.panelComponent, 1, 8)),
				new ManualPages.Crafting(m, "industrialwires.7seg", new ItemStack(IndustrialWires.panelComponent, 1, 9)),
				new ManualPages.Crafting(m, "industrialwires.rgb_led", new ItemStack(IndustrialWires.panelComponent, 1, 10))
		);

 		m.entryRenderPost();
		m.fontRenderer.setUnicodeFlag(uni);
	}


	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}

	@Override
	public boolean isSingleplayer() {
		return Minecraft.getMinecraft().isSingleplayer();
	}

	@Override
	public boolean isValidTextureSource(ItemStack stack) {
		if (!super.isValidTextureSource(stack)) {
			return false;
		}
		IBakedModel texModel = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack,
				null, null);
		TextureAtlasSprite sprite = texModel.getParticleTexture();
		//noinspection ConstantConditions
		if (sprite == null || sprite.hasAnimationMetadata()) {
			return false;
		}
		int[][] data = sprite.getFrameTextureData(0);
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				if ((data[x][y] >>> 24) != 255) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Gui getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID == 0) {
			TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
			if (te instanceof TileEntityRSPanel) {
				return new GuiRSPanelConn((TileEntityRSPanel) te);
			}
			if (te instanceof TileEntityPanelCreator) {
				return new GuiPanelCreator(player.inventory, (TileEntityPanelCreator) te);
			}
		} else if (ID == 1) {
			EnumHand h = z == 1 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
			ItemStack held = player.getHeldItem(h);
			if (!held.isEmpty()) {
				if (held.getItem() == IndustrialWires.panelComponent) {
					return new GuiPanelComponent(h, ItemPanelComponent.componentFromStack(held));
				} else if (held.getItem() == IndustrialWires.key) {
					return new GuiRenameKey(h);
				}
			}
		}
		return null;
	}
}
