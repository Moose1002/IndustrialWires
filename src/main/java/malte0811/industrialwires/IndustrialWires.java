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
package malte0811.industrialwires;

 import blusunrize.immersiveengineering.ImmersiveEngineering;
 import malte0811.industrialwires.blocks.BlockIWBase;
 import malte0811.industrialwires.blocks.TEDataFixer;
 import malte0811.industrialwires.blocks.controlpanel.*;
 import malte0811.industrialwires.compat.Compat;
 import malte0811.industrialwires.controlpanel.PanelComponent;
 import malte0811.industrialwires.controlpanel.PanelUtils;
 import malte0811.industrialwires.crafting.Recipes;
 import malte0811.industrialwires.items.ItemKey;
 import malte0811.industrialwires.items.ItemPanelComponent;
 import malte0811.industrialwires.network.MessageGUIInteract;
 import malte0811.industrialwires.network.MessageItemSync;
 import malte0811.industrialwires.network.MessagePanelInteract;
 import malte0811.industrialwires.network.MessageTileSyncIW;
 import net.minecraft.block.Block;
 import net.minecraft.creativetab.CreativeTabs;
 import net.minecraft.item.Item;
 import net.minecraft.item.ItemStack;
 import net.minecraft.item.crafting.IRecipe;
 import net.minecraft.util.ResourceLocation;
 import net.minecraft.util.datafix.FixTypes;
 import net.minecraftforge.common.util.ModFixs;
 import net.minecraftforge.event.RegistryEvent;
 import net.minecraftforge.fml.common.FMLCommonHandler;
 import net.minecraftforge.fml.common.Loader;
 import net.minecraftforge.fml.common.Mod;
 import net.minecraftforge.fml.common.Mod.EventHandler;
 import net.minecraftforge.fml.common.SidedProxy;
 import net.minecraftforge.fml.common.event.FMLInitializationEvent;
 import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
 import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
 import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
 import net.minecraftforge.fml.common.network.NetworkRegistry;
 import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
 import net.minecraftforge.fml.common.registry.GameRegistry;
 import net.minecraftforge.fml.relauncher.Side;
 import org.apache.logging.log4j.Logger;

 import java.util.ArrayList;
 import java.util.List;


 @Mod(modid = IndustrialWires.MODID, version = IndustrialWires.VERSION, dependencies = "required-after:immersiveengineering@[0.12-86,);after:ic2;required-after:forge@[14.23.3.2694,)")
@Mod.EventBusSubscriber
public class IndustrialWires {
	public static final String MODID = "industrialwires";
	public static final String VERSION = "${version}";
	public static final String MODNAME = "Immersive Panels";
	public static final int DATAFIXER_VER = 1;

	public static final List<BlockIWBase> blocks = new ArrayList<>();
	public static final List<Item> items = new ArrayList<>();

	@GameRegistry.ObjectHolder(MODID+":"+BlockPanel.NAME)
	public static BlockPanel panel = null;

	@GameRegistry.ObjectHolder(MODID+":"+ItemPanelComponent.NAME)
	public static ItemPanelComponent panelComponent = null;
	@GameRegistry.ObjectHolder(MODID+":"+ItemKey.ITEM_NAME)
	public static ItemKey key = null;

	public static final SimpleNetworkWrapper packetHandler = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

	public static Logger logger;
	@Mod.Instance(MODID)
	public static IndustrialWires instance = new IndustrialWires();
	public static CreativeTabs creativeTab = new CreativeTabs(MODID) {


		@Override
		public ItemStack createIcon() {
			return new ItemStack(panel, 1, 3);
		}
	};
	@SidedProxy(clientSide = "malte0811.industrialwires.client.ClientProxy", serverSide = "malte0811.industrialwires.CommonProxy")
	public static CommonProxy proxy;
	public static boolean isOldIE;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		{
			double ieThreshold = 12.74275;
			String ieVer = Loader.instance().getIndexedModList().get(ImmersiveEngineering.MODID).getDisplayVersion();
			int firstDash = ieVer.indexOf('-');
			String end = ieVer.substring(firstDash + 1);
			String start = ieVer.substring(0, firstDash);
			end = end.replaceAll("[^0-9]", "");
			start = start.replaceAll("[^0-9]", "");
			double ieVerDouble = Double.parseDouble(start + "." + end);
			isOldIE = ieVerDouble < ieThreshold;
		}
		logger = e.getModLog();
		new IWConfig();

		GameRegistry.registerTileEntity(TileEntityPanel.class, new ResourceLocation(MODID, "control_panel"));
		GameRegistry.registerTileEntity(TileEntityGeneralCP.class, new ResourceLocation(MODID, "gcp"));
		GameRegistry.registerTileEntity(TileEntityRSPanelIE.class, new ResourceLocation(MODID, "control_panel_rs"));
		GameRegistry.registerTileEntity(TileEntityRSPanelOthers.class, new ResourceLocation(MODID, "control_panel_rs_compat"));
		GameRegistry.registerTileEntity(TileEntityPanelCreator.class, new ResourceLocation(MODID, "panel_creator"));
		GameRegistry.registerTileEntity(TileEntityUnfinishedPanel.class, new ResourceLocation(MODID, "unfinished_panel"));
		GameRegistry.registerTileEntity(TileEntityComponentPanel.class, new ResourceLocation(MODID, "single_component_panel"));


		proxy.preInit();
		Compat.preInit();
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().register(new BlockPanel());
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		for (BlockIWBase b:blocks) {
			event.getRegistry().register(b.createItemBlock());
		}

		event.getRegistry().register(new ItemPanelComponent());
		event.getRegistry().register(new ItemKey());
	}

	@SubscribeEvent
	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
		Recipes.addRecipes(event.getRegistry());
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {

		packetHandler.registerMessage(MessageTileSyncIW.HandlerClient.class, MessageTileSyncIW.class, 0, Side.CLIENT);
		packetHandler.registerMessage(MessagePanelInteract.HandlerServer.class, MessagePanelInteract.class, 1, Side.SERVER);
		packetHandler.registerMessage(MessageGUIInteract.HandlerServer.class, MessageGUIInteract.class, 2, Side.SERVER);
		packetHandler.registerMessage(MessageItemSync.HandlerServer.class, MessageItemSync.class, 3, Side.SERVER);

		NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
		Compat.init();
		PanelComponent.init();
		ModFixs fixer = FMLCommonHandler.instance().getDataFixer().init(MODID, DATAFIXER_VER);
		fixer.registerFix(FixTypes.BLOCK_ENTITY, new TEDataFixer());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {
        PanelUtils.PANEL_ITEM = Item.getItemFromBlock(panel);
        proxy.postInit();
	}
}
