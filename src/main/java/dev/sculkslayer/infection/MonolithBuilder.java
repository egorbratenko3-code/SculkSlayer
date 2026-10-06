package dev.sculkslayer.infection;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.sculkslayer.registry.ModBlocks;

/** Builds the command-block monolith, its temple and the Hallow Altar, and records the protected region. */
public final class MonolithBuilder {
	private static final int R = 12;
	private static final int FLAGS = Block.UPDATE_CLIENTS;

	private static void set(ServerLevel level, BlockPos p, BlockState s) {
		level.setBlock(p, s, FLAGS);
	}

	/** One square plinth layer: border ring, filled interior, distinct corners. */
	private static void tier(ServerLevel level, int cx, int cz, int y, int r, BlockState fill, BlockState border, BlockState corner) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				boolean edgeX = Math.abs(dx) == r;
				boolean edgeZ = Math.abs(dz) == r;
				set(level, new BlockPos(cx + dx, y, cz + dz), edgeX && edgeZ ? corner : (edgeX || edgeZ) ? border : fill);
			}
		}
	}

	public static void build(ServerLevel level, BlockPos origin, SculkState st) {
		int cx = origin.getX();
		int cz = origin.getZ();
		int floorY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) + 2;

		BlockState air = Blocks.AIR.defaultBlockState();
		BlockState bricks = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		BlockState deepslate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		BlockState hallow = ModBlocks.HALLOW_BLOCK.defaultBlockState();
		// Raise the sanctuary on a compact, shallow hill so the monolith reads clearly above the landscape.
		for (int ring = 0; ring < 4; ring++) {
			int radius = R + 3 - ring;
			int y = floorY - 4 + ring;
			for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
				if (Math.abs(dx) == radius || Math.abs(dz) == radius || ring == 0)
					set(level, new BlockPos(cx + dx, y, cz + dz), ring == 0 ? Blocks.DIRT.defaultBlockState() : hallow);
			}
		}

		// platform, foundation and cleared airspace
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				int x = cx + dx;
				int z = cz + dz;
				for (int y = floorY - 1; y >= floorY - 14; y--) {
					BlockPos fp = new BlockPos(x, y, z);
					BlockState fs = level.getBlockState(fp);
					if (!fs.isAir() && fs.getFluidState().isEmpty() && !fs.canBeReplaced()) break;
					set(level, fp, deepslate);
				}
				boolean edge = Math.abs(dx) == R || Math.abs(dz) == R;
				set(level, new BlockPos(x, floorY, z), edge ? hallow : ((dx + dz) % 2 == 0 ? bricks : deepslate));
				for (int y = floorY + 1; y <= floorY + 26; y++) {
					BlockPos up = new BlockPos(x, y, z);
					if (!level.getBlockState(up).isAir()) set(level, up, air);
				}
			}
		}

		// corner beacons of hallow blocks
		for (int sx : new int[] {-R, R}) {
			for (int sz : new int[] {-R, R}) {
				for (int h = 1; h <= 5; h++) set(level, new BlockPos(cx + sx, floorY + h, cz + sz), hallow);
				set(level, new BlockPos(cx + sx, floorY + 6, cz + sz), Blocks.SEA_LANTERN.defaultBlockState());
			}
		}

		// ---- the monolith: three plinth tiers, an obsidian shaft banded with command blocks,
		// ---- gilded ribs, amethyst crowns and a spire that ends in a lightning rod
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
		BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
		tier(level, cx, cz, floorY + 1, 4, Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
				Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState());
		tier(level, cx, cz, floorY + 2, 3, deepslate, Blocks.DEEPSLATE_TILES.defaultBlockState(), crying);
		tier(level, cx, cz, floorY + 3, 2, bricks, hallow, hallow);
		for (int sx : new int[] {-2, 2}) {
			for (int sz : new int[] {-2, 2}) {
				set(level, new BlockPos(cx + sx, floorY + 4, cz + sz), Blocks.AMETHYST_CLUSTER.defaultBlockState());
			}
		}

		// shaft: obsidian / crying obsidian corners, command-block bands whose arrow faces point outward
		for (int h = 4; h <= 15; h++) {
			Block band = switch ((h - 4) % 3) {
				case 0 -> Blocks.COMMAND_BLOCK;
				case 1 -> Blocks.REPEATING_COMMAND_BLOCK;
				default -> Blocks.CHAIN_COMMAND_BLOCK;
			};
			BlockState corner = h % 2 == 0 ? obsidian : crying;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					BlockState s;
					if (dx == 0 && dz == 0) {
						s = hallow;
					} else if (dx != 0 && dz != 0) {
						s = corner;
					} else {
						Direction out = dz < 0 ? Direction.NORTH : dz > 0 ? Direction.SOUTH : dx < 0 ? Direction.WEST : Direction.EAST;
						s = band.defaultBlockState().setValue(BlockStateProperties.FACING, out);
					}
					set(level, new BlockPos(cx + dx, floorY + h, cz + dz), s);
				}
			}
		}

		// two gilded ribs (5x5 rings) with glowing corners and end rods
		for (int h : new int[] {7, 11}) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
					boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
					set(level, new BlockPos(cx + dx, floorY + h, cz + dz), corner ? lantern : hallow);
					if (corner) set(level, new BlockPos(cx + dx, floorY + h + 1, cz + dz), Blocks.END_ROD.defaultBlockState());
				}
			}
		}

		// crown and spire
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) set(level, new BlockPos(cx + dx, floorY + 16, cz + dz), hallow);
		}
		set(level, new BlockPos(cx, floorY + 17, cz), crying);
		for (Direction d : Direction.Plane.HORIZONTAL) set(level, new BlockPos(cx, floorY + 17, cz).relative(d), crying);
		set(level, new BlockPos(cx, floorY + 18, cz), hallow);
		set(level, new BlockPos(cx, floorY + 19, cz), lantern);
		for (int h = 20; h <= 22; h++) set(level, new BlockPos(cx, floorY + h, cz), Blocks.END_ROD.defaultBlockState());
		set(level, new BlockPos(cx, floorY + 23, cz), Blocks.LIGHTNING_ROD.defaultBlockState());

		// clickable signs on the front (south) face
		BlockPos swordSign = new BlockPos(cx - 1, floorY + 5, cz + 2);
		BlockPos pickSign = new BlockPos(cx + 1, floorY + 5, cz + 2);
		BlockPos axeSign = new BlockPos(cx, floorY + 5, cz + 3);
		placeSign(level, swordSign, "get hallow", "sword", "[click]");
		placeSign(level, pickSign, "get hallow", "pickaxe", "[click]");
		placeSign(level, axeSign, "get Hallow", "axe", "[Click]");

		// the temple: altar, dais, four pillars and a roof
		int az = cz + 8;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) set(level, new BlockPos(cx + dx, floorY, az + dz), hallow);
		}
		BlockPos altar = new BlockPos(cx, floorY + 1, az);
		set(level, altar, ModBlocks.HALLOW_ALTAR.defaultBlockState());
		BlockPos recipeLectern = new BlockPos(cx + 2, floorY + 1, az + 2);
		placeRecipeBook(level, recipeLectern);
		for (int sx : new int[] {-3, 3}) {
			for (int sz : new int[] {-3, 3}) {
				for (int h = 1; h <= 5; h++) set(level, new BlockPos(cx + sx, floorY + h, az + sz), Blocks.QUARTZ_PILLAR.defaultBlockState());
				set(level, new BlockPos(cx + sx, floorY + 6, az + sz), hallow);
			}
		}
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) set(level, new BlockPos(cx + dx, floorY + 7, az + dz), bricks);
		}
		set(level, new BlockPos(cx, floorY + 6, az), Blocks.SEA_LANTERN.defaultBlockState());

		// record everything
		st.active = true;
		st.cleansed = false;
		st.dim = SculkManager.dimKey(level);
		st.monolith = new BlockPos(cx, floorY + 1, cz);
		st.altar = altar;
		st.signSword = swordSign;
		st.signPick = pickSign;
		st.signAxe = axeSign;
		st.sanctuaryPatched = true;
		st.boxMin = new BlockPos(cx - R - 1, floorY - 15, cz - R - 1);
		st.boxMax = new BlockPos(cx + R + 1, floorY + 30, cz + R + 1);
	}

	/** Applies the sanctuary fixes once to monoliths already saved by earlier 1.2 builds. */
	public static void patchExisting(ServerLevel level, SculkState st) {
		if (st.sanctuaryPatched || st.monolith == null || !st.dim.equals(SculkManager.dimKey(level))) return;
		int cx = st.monolith.getX();
		int cz = st.monolith.getZ();
		int floorY = st.monolith.getY() - 1;
		int az = cz + 8;
		if (!level.hasChunkAt(new BlockPos(cx - 6, floorY, az - 6))
				|| !level.hasChunkAt(new BlockPos(cx + 6, floorY + 8, az + 6))) return;

		// Remove only the old Hallow-wood enclosure blocks at the known temple perimeter.
		for (int h = 1; h <= 3; h++) for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
			if (Math.abs(dx) != 5 && Math.abs(dz) != 5) continue;
			BlockPos wall = new BlockPos(cx + dx, floorY + h, az + dz);
			if (level.getBlockState(wall).is(ModBlocks.HALLOW_WOOD)) set(level, wall, Blocks.AIR.defaultBlockState());
		}
		BlockPos oldLectern = new BlockPos(cx + 4, floorY + 1, az);
		if (level.getBlockState(oldLectern).is(Blocks.LECTERN)) set(level, oldLectern, Blocks.AIR.defaultBlockState());
		placeRecipeBook(level, new BlockPos(cx + 2, floorY + 1, az + 2));
		BlockPos axeSign = new BlockPos(cx, floorY + 5, cz + 3);
		placeSign(level, axeSign, "get Hallow", "axe", "[Click]");
		st.signAxe = axeSign;
		st.sanctuaryPatched = true;
		SculkManager.save();
	}

	private static void placeRecipeBook(ServerLevel level, BlockPos pos) {
		set(level, pos, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
			ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
			book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
					Filterable.passThrough("Hallow Recipes"), "Sculk Slayer", 0,
					List.of(
							Filterable.passThrough(Component.literal("Hallow Bar: 8 gold ingots + 1 diamond.\nHallow Block: 9 bars; uncraft 1 block into 9 bars.\nHallow Wood: bar + birch planks.")),
							Filterable.passThrough(Component.literal("At the Altar: Hallow armor, shield, sword, pickaxe, axe, holy water, crucifix and grenades.\nThe Hallow Axe awakens at infection stage 7.")),
							Filterable.passThrough(Component.literal("Hallow Power books I–III upgrade Hallow tools in an anvil.\nKeep the monolith lit. When the Core wakes, weaken it with a Holy Grenade and break it using the Hallow Pickaxe."))
					), true));
			lectern.setBook(book);
			lectern.setChanged();
			BlockState lecternState = level.getBlockState(pos);
			level.sendBlockUpdated(pos, lecternState, lecternState, Block.UPDATE_ALL);
		}
	}

	private static void placeSign(ServerLevel level, BlockPos pos, String l1, String l2, String l3) {
		BlockState state = Blocks.WARPED_WALL_SIGN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		set(level, pos, state);
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			SignText text = new SignText()
					.setMessage(0, Component.literal(l1))
					.setMessage(1, Component.literal(l2))
					.setMessage(2, Component.literal(l3))
					.setColor(DyeColor.CYAN)
					.setHasGlowingText(true);
			sign.setText(text, true);
			sign.setWaxed(true);
			sign.setChanged();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private MonolithBuilder() {}
}
