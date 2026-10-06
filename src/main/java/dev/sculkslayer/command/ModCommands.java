package dev.sculkslayer.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.sculkslayer.infection.MonolithBuilder;
import dev.sculkslayer.infection.SculkManager;
import dev.sculkslayer.infection.SculkState;
import dev.sculkslayer.infection.Stage;

public final class ModCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("sculkslayer")
				.executes(ctx -> {
					SculkState st = SculkManager.state();
					return st != null && !st.active ? start(ctx.getSource()) : status(ctx.getSource());
				})
				.then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
				.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
				.then(Commands.literal("stop")
					.requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.executes(ctx -> stop(ctx.getSource())))
				.then(Commands.literal("setinfection")
					.requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("percent", DoubleArgumentType.doubleArg(0, 100))
						.executes(ctx -> setInfection(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "percent")))))
				.then(Commands.literal("setstage")
					.requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
					.then(Commands.argument("stage", IntegerArgumentType.integer(0, 10))
						.executes(ctx -> setStage(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stage")))))));
	}

	private static int start(CommandSourceStack src) {
		SculkState st = SculkManager.state();
		if (st == null) {
			src.sendFailure(Component.literal("The world is not ready yet, try again in a moment."));
			return 0;
		}
		if (st.active) {
			src.sendFailure(Component.literal("The plague is already active. Monolith at " + pos(st.monolith) + "."));
			return 0;
		}
		ServerPlayer player = src.getPlayer();
		if (player == null || !SculkManager.startAt(player)) {
			src.sendFailure(Component.literal("Run this as a player in the Overworld."));
			return 0;
		}
		return 1;
	}

	private static int status(CommandSourceStack src) {
		SculkState st = SculkManager.state();
		if (st == null) return 0;
		String msg = !st.active ? "Inactive. Use /sculkslayer start."
				: st.cleansed ? "The world is cleansed."
				: String.format("Infected: %.1f%%  Stage: %s  Monolith: %s%s", st.infection, Stage.label(st.stage()), pos(st.monolith),
						st.core == null ? "" : "  Core: " + pos(st.core) + (st.coreWeak ? " (weakened)" : " (dormant)"));
		src.sendSuccess(() -> Component.literal(msg), false);
		return 1;
	}

	private static int stop(CommandSourceStack src) {
		SculkState st = SculkManager.state();
		if (st == null) return 0;
		st.active = false;
		SculkManager.syncAll();
		src.sendSuccess(() -> Component.literal("Plague frozen. Use /sculkslayer start to build a new monolith and restart."), true);
		return 1;
	}

	private static int setInfection(CommandSourceStack src, double percent) {
		SculkState st = SculkManager.state();
		if (st == null) return 0;
		st.infection = percent;
		SculkManager.syncAll();
		src.sendSuccess(() -> Component.literal("Infection set to " + percent + "%."), true);
		return 1;
	}

	private static int setStage(CommandSourceStack src, int stage) {
		return setInfection(src, Stage.threshold(stage));
	}

	private static String pos(BlockPos p) {
		return p == null ? "?" : p.getX() + " " + p.getY() + " " + p.getZ();
	}

	private ModCommands() {}
}
