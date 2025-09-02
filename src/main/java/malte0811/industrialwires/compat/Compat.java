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

package malte0811.industrialwires.compat;

import com.google.common.collect.ImmutableMap;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class Compat {
	public static IBlockAction<EnumFacing, byte[]> getBundledRS = (w, p, f) -> new byte[16];
	public static IBlockAction<Void, Void> updateBundledRS = (w, p, f) -> null;
	public static boolean enableOtherRS = false;
	private static Map<String, Class<? extends CompatModule>> modules = ImmutableMap.of(ProjectRedAPI.modIDCore, CompatProjectRed.class);
	private static Method preInit;
	private static Method init;

	static {
		try {
			preInit = CompatModule.class.getMethod("preInit");
			init = CompatModule.class.getMethod("init");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public static void preInit() {
		for (Map.Entry<String, Class<? extends CompatModule>> e:modules.entrySet()) {
			if (Loader.isModLoaded(e.getKey())) {
				try {
					preInit.invoke(e.getValue().newInstance());
				} catch (IllegalAccessException | InvocationTargetException | InstantiationException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public static void init() {
		for (Map.Entry<String, Class<? extends CompatModule>> e:modules.entrySet()) {
			if (Loader.isModLoaded(e.getKey())) {
				try {
					init.invoke(e.getValue().newInstance());
				} catch (IllegalAccessException | InvocationTargetException | InstantiationException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	static abstract class CompatModule {
		public void preInit() {
		}

		public void init() {
		}
	}

	public static class CompatProjectRed extends CompatModule {

		@Override
		public void preInit() {
			enableOtherRS = true;
		}

		@Override
		public void init() {
			super.init();
			IBlockAction<EnumFacing, byte[]> oldGet = getBundledRS;
			getBundledRS = (w, p, f) -> {
				byte[] oldIn = oldGet.run(w, p, f);
				byte[] prIn = ProjectRedAPI.transmissionAPI.getBundledInput(w, p, f);
				if (prIn!=null) {
					for (int i = 0; i < 16; i++) {
						oldIn[i] = (byte) Math.ceil((prIn[i] & 255) / 17.0);
					}
				}
				return oldIn;
			};
			IBlockAction<Void, Void> oldUpdate = updateBundledRS;
			updateBundledRS = (w, p, f)-> {
				oldUpdate.run(w, p, f);
				w.notifyNeighborsOfStateChange(p, w.getBlockState(p).getBlock(), true);
				return null;
			};
		}
	}
}