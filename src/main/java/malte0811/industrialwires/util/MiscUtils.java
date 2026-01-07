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

package malte0811.industrialwires.util;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.common.util.chickenbones.Matrix4;
import com.google.common.collect.ImmutableSet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.BiPredicate;

public final class MiscUtils {
	private MiscUtils() {
	}

	public static List<BlockPos> discoverLocal(BlockPos here, BiPredicate<BlockPos, Integer> isValid) {
		if (!isValid.test(here, 0)) {
			return new ArrayList<>();
		}
		List<BlockPos> ret = new ArrayList<>();
		Queue<BlockPos> open = new ArrayDeque<>();
		open.add(here);
		while (!open.isEmpty()) {
			BlockPos curr = open.poll();
			assert curr!=null;
			ret.add(curr);
			for (EnumFacing f : EnumFacing.VALUES) {
				BlockPos next = curr.offset(f);
				if (!open.contains(next) && !ret.contains(next) && isValid.test(next, ret.size())) {
					open.offer(next);
				}
			}
		}
		return ret;
	}

	public static BlockPos offset(BlockPos p, EnumFacing f, boolean mirror, Vec3i relative) {
		return offset(p, f, mirror, relative.getX(), relative.getZ(), relative.getY());
	}
	/**
	 * @param mirror inverts right
	 */
	public static BlockPos offset(BlockPos p, EnumFacing f, boolean mirror, int right, int forward, int up) {
		if (mirror) {
			right *= -1;
		}
		return p.offset(f, forward).offset(f.rotateY(), right).add(0, up, 0);
	}

	public static Vec3d offset(Vec3d p, EnumFacing f, boolean mirror, Vec3d relative) {
		return offset(p, f, mirror, relative.x, relative.z, relative.y);
	}

	public static Vec3d offset(Vec3d p, EnumFacing f, boolean mirror, double right, double forward, double up) {
		if (mirror) {
			right *= -1;
		}
		return offset(offset(p, f, forward), f.rotateY(), right).add(0, up, 0);
	}

	public static Vec3d offset(Vec3d in, EnumFacing f, double amount) {
		if (amount==0) {
			return in;
		}
		return in.add(f.getXOffset() * amount, f.getYOffset() * amount, f.getZOffset() * amount);
	}

	@Nonnull
	public static AxisAlignedBB apply(@Nonnull Matrix4 mat, @Nonnull AxisAlignedBB in) {
		Vec3d min = new Vec3d(in.minX, in.minY, in.minZ);
		Vec3d max = new Vec3d(in.maxX, in.maxY, in.maxZ);
		min = mat.apply(min);
		max = mat.apply(max);
		return new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z);
	}

	public static Set<ImmersiveNetHandler.Connection> genConnBlockstate(Set<ImmersiveNetHandler.Connection> conns, World world) {
		if (conns == null)
			return ImmutableSet.of();
		Set<ImmersiveNetHandler.Connection> ret = new HashSet<ImmersiveNetHandler.Connection>() {
			@Override
			public boolean equals(Object o) {
				if (o == this)
					return true;
				if (!(o instanceof HashSet))
					return false;
				HashSet<ImmersiveNetHandler.Connection> other = (HashSet<ImmersiveNetHandler.Connection>) o;
				if (other.size() != this.size())
					return false;
				for (ImmersiveNetHandler.Connection c : this)
					if (!other.contains(c))
						return false;
				return true;
			}
		};
		for (ImmersiveNetHandler.Connection c : conns) {
			IImmersiveConnectable end = ApiUtils.toIIC(c.end, world, false);
			if (end == null)
				continue;
			// generate subvertices
			c.getSubVertices(world);
			ret.add(c);
		}

		return ret;
	}

	@SideOnly(Side.CLIENT)
	public static Vec2f rotate90(Vec2f in) {
		//Yes, when rotating by 90 degrees, x becomes y!
		//noinspection SuspiciousNameCombination
		return new Vec2f(-in.y, in.x);
	}

	@SideOnly(Side.CLIENT)
	public static Vec2f subtract(Vec2f a, Vec2f b) {
		return new Vec2f(a.x-b.x, a.y-b.y);
	}

	@SideOnly(Side.CLIENT)
	public static Vec2f add(Vec2f a, Vec2f b) {
		return new Vec2f(a.x+b.x, a.y+b.y);
	}

	@SideOnly(Side.CLIENT)
	public static Vec2f scale(Vec2f a, float f) {
		return new Vec2f(a.x*f, a.y*f);
	}

	@SideOnly(Side.CLIENT)
	public static Vector3f withNewY(Vec2f in, float y) {
		return new Vector3f(in.x, y, in.y);
	}

	public static <T extends TileEntity> T getLoadedTE(World w, BlockPos pos, Class<T> clazz) {
		if (w.isBlockLoaded(pos)) {
			TileEntity te = w.getTileEntity(pos);
			if (te!=null && clazz.isAssignableFrom(te.getClass())) {
				//noinspection unchecked
				return (T) te;
			}
		}
		return null;
	}
}