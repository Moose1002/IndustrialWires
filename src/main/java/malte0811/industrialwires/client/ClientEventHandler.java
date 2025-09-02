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

import malte0811.industrialwires.IndustrialWires;
import malte0811.industrialwires.blocks.BlockIWBase;
import malte0811.industrialwires.blocks.IMetaEnum;
import malte0811.industrialwires.blocks.controlpanel.TileEntityPanel;
import malte0811.industrialwires.client.panelmodel.PanelModel;
import malte0811.industrialwires.controlpanel.PanelComponent;
import malte0811.industrialwires.items.ItemKey;
import malte0811.industrialwires.items.ItemPanelComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = IndustrialWires.MODID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class ClientEventHandler {
	public static boolean shouldScreenshot = false;

	@SubscribeEvent
	public static void renderBoundingBoxes(DrawBlockHighlightEvent event) {
		if (!event.isCanceled() && event.getSubID() == 0 && event.getTarget().typeOfHit == RayTraceResult.Type.BLOCK) {
			TileEntity tile = event.getPlayer().world.getTileEntity(event.getTarget().getBlockPos());
			if (tile instanceof TileEntityPanel) {
				TileEntityPanel panel = (TileEntityPanel) tile;
				Pair<PanelComponent, RayTraceResult> pc = panel.getSelectedComponent(Minecraft.getMinecraft().player, event.getTarget().hitVec, true);
				if (pc != null) {
					pc.getLeft().renderBox();
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public static void bakeModel(ModelBakeEvent event) {
		event.getModelRegistry().putObject(new ModelResourceLocation(IndustrialWires.MODID + ":control_panel", "inventory,type=top"), new PanelModel());
		event.getModelRegistry().putObject(new ModelResourceLocation(IndustrialWires.MODID + ":control_panel", "inventory,type=unfinished"), new PanelModel());
	}
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent evt) {
		for (int meta = 0; meta < ItemPanelComponent.types.length; meta++) {
			ResourceLocation loc = new ResourceLocation(IndustrialWires.MODID, "panel_component/" + ItemPanelComponent.types[meta]);
			ModelBakery.registerItemVariants(IndustrialWires.panelComponent, loc);
			ModelLoader.setCustomModelResourceLocation(IndustrialWires.panelComponent, meta, new ModelResourceLocation(loc, "inventory"));
		}
		for (int meta = 0; meta < ItemKey.types.length; meta++) {
			ResourceLocation loc = new ResourceLocation(IndustrialWires.MODID, "key/" + ItemKey.types[meta]);
			ModelBakery.registerItemVariants(IndustrialWires.key, loc);
			ModelLoader.setCustomModelResourceLocation(IndustrialWires.key, meta, new ModelResourceLocation(loc, "inventory"));
		}
		for (BlockIWBase b : IndustrialWires.blocks) {
			Item blockItem = Item.getItemFromBlock(b);
			final ResourceLocation loc = b.getRegistryName();
			assert loc != null;
			ModelLoader.setCustomMeshDefinition(blockItem, stack -> new ModelResourceLocation(loc, "inventory"));
			Object[] v = ((IMetaEnum) b).getValues();
			for (int meta = 0; meta < v.length; meta++) {
				String location = loc.toString();
				String prop = "inventory,type=" + v[meta].toString().toLowerCase(Locale.US);
				try {
					ModelLoader.setCustomModelResourceLocation(blockItem, meta, new ModelResourceLocation(location, prop));
				} catch (NullPointerException npe) {
					throw new RuntimeException(b + " lacks an item!", npe);
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void renderWorldLastLow(RenderWorldLastEvent ev) {
		if (shouldScreenshot) {
			Minecraft mc = Minecraft.getMinecraft();
			ITextComponent comp = ScreenShotHelper.saveScreenshot(mc.gameDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());//TODO
			mc.player.sendMessage(comp);
			shouldScreenshot = false;
		}
	}

}
