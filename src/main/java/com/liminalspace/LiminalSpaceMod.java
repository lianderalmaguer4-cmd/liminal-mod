package com.liminalspace;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import com.liminalspace.entity.WhisperEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LiminalSpaceMod implements ModInitializer {
    public static final String MOD_ID = "liminalspace";
    public static final RegistryKey<World> LIMINAL_WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID, "liminal"));
    public static final RegistryKey<EntityType<?>> WHISPER_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "whisper"));
    public static final EntityType<WhisperEntity> WHISPER = Registry.register(
            Registries.ENTITY_TYPE,
            WHISPER_KEY,
            EntityType.Builder.create(WhisperEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.5f, 5.8f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(WHISPER_KEY)
    );
    private static final Map<UUID, ReturnPoint> RETURN_POINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> WHISPER_PENDING = new ConcurrentHashMap<>();
    private static final AtomicBoolean BUILT = new AtomicBoolean(false);

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(LiminalSpaceMod::registerCommands);
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(LiminalSpaceMod::handleWhisperChat);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("liminal")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(context -> enterLiminal(context.getSource())));

        dispatcher.register(CommandManager.literal("liminalbuild")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> buildForCommand(context.getSource())));
    }

    private static int enterLiminal(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return 0;
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getWorld(LIMINAL_WORLD);
        if (world == null) {
            source.sendError(net.minecraft.text.Text.literal("The Liminal Space dimension could not be loaded."));
            return 0;
        }

        buildWorld(world);
        if (!isInLiminal(player)) {
            RETURN_POINTS.put(player.getUuid(), new ReturnPoint(player.getWorld().getRegistryKey(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
        }
        player.teleport(world, 0.5, 5.0, 8.5, java.util.Set.of(), 0.0f, 0.0f, true);
        source.sendFeedback(() -> net.minecraft.text.Text.literal("§7You enter the Liminal Space..."), false);
        return 1;
    }

    private static int buildForCommand(ServerCommandSource source) {
        ServerWorld world = source.getServer().getWorld(LIMINAL_WORLD);
        if (world == null) return 0;
        buildWorld(world);
        source.sendFeedback(() -> net.minecraft.text.Text.literal("§aLiminal Space generated."), true);
        return 1;
    }

    private static void buildWorld(ServerWorld world) {
        if (BUILT.get()) return;
        BUILT.set(true);

        // Large empty grassy plane. The custom flat generator already provides the floor.
        // Add sparse low hills to break the horizon without making the landscape busy.
        makeHill(world, -48, -35, 2, 9, 6);
        makeHill(world, 42, -22, 2, 7, 5);
        makeHill(world, -35, 36, 1, 8, 5);
        makeHill(world, 51, 35, 3, 10, 7);
        makeHill(world, 78, 2, 2, 7, 6);

        int baseY = 4;
        int pillarTop = 78;

        // Main giant rock pillar, irregular and intentionally unnatural.
        for (int y = baseY; y <= pillarTop; y++) {
            double t = (y - baseY) / (double) (pillarTop - baseY);
            int radius = (int) Math.max(4, 12 - t * 7);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double wobble = Math.sin(y * 0.41) * 1.7 + Math.cos(y * 0.19) * 1.3;
                    double dx = x - wobble;
                    double dz = z + Math.sin(y * 0.27) * 1.2;
                    if (dx * dx + dz * dz <= radius * radius) {
                        Block block = ((x + z + y) % 7 == 0) ? Blocks.COBBLESTONE : Blocks.STONE;
                        set(world, x, y, z, block);
                    }
                }
            }
        }

        // Dirt/grass cap on top.
        for (int x = -6; x <= 6; x++) {
            for (int z = -5; z <= 5; z++) {
                if (x * x / 36.0 + z * z / 25.0 <= 1.0) {
                    set(world, x, pillarTop + 1, z, Blocks.DIRT);
                    set(world, x, pillarTop + 2, z, Blocks.GRASS_BLOCK);
                }
            }
        }

        // Long staircase climbing around the pillar.
        buildStaircase(world, pillarTop);

        // House sits on the grassy cap.
        buildHouse(world, -4, pillarTop + 3, -3);

        // The Whisperer is waiting inside the house, slightly hunched and unnaturally still.
        WhisperEntity whisper = new WhisperEntity(WHISPER, world);
        whisper.refreshPositionAndAngles(1.5, pillarTop + 4, 1.0, 180.0f, 0.0f);
        world.spawnEntity(whisper);

        // A few unsettling details around the house.
        set(world, 7, pillarTop + 3, 3, Blocks.COBBLESTONE);
        set(world, 7, pillarTop + 4, 3, Blocks.COBBLESTONE);
        set(world, 7, pillarTop + 5, 3, Blocks.COBBLESTONE);
    }

    private static void buildStaircase(ServerWorld world, int topY) {
        // Spiral-ish staircase on the south/east side. Each segment climbs steadily.
        int y = 5;
        int x = 15;
        int z = 7;
        int step = 0;
        while (y < topY + 1) {
            for (int i = 0; i < 3 && y < topY + 1; i++) {
                set(world, x, y, z, Blocks.STONE_BRICK_STAIRS);
                set(world, x, y - 1, z, Blocks.STONE_BRICKS);
                y++;
                z--;
            }
            for (int i = 0; i < 3 && y < topY + 1; i++) {
                x--;
                set(world, x, y, z, Blocks.STONE_BRICK_STAIRS);
                set(world, x, y - 1, z, Blocks.STONE_BRICKS);
                y++;
            }
            for (int i = 0; i < 3 && y < topY + 1; i++) {
                set(world, x, y, z + 1, Blocks.STONE_BRICK_STAIRS);
                set(world, x, y - 1, z + 1, Blocks.STONE_BRICKS);
                y++;
                z++;
            }
            for (int i = 0; i < 3 && y < topY + 1; i++) {
                x++;
                set(world, x, y, z, Blocks.STONE_BRICK_STAIRS);
                set(world, x, y - 1, z, Blocks.STONE_BRICKS);
                y++;
            }
            step++;
            if (step > 30) break;
        }
    }

    private static void buildHouse(ServerWorld world, int x0, int y0, int z0) {
        int w = 11, d = 9, h = 5;

        // Floor.
        for (int x = x0; x < x0 + w; x++)
            for (int z = z0; z < z0 + d; z++)
                set(world, x, y0, z, Blocks.OAK_PLANKS);

        // Walls and ceiling.
        for (int y = y0 + 1; y <= y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                set(world, x, y, z0, Blocks.DARK_OAK_PLANKS);
                set(world, x, y, z0 + d - 1, Blocks.DARK_OAK_PLANKS);
            }
            for (int z = z0; z < z0 + d; z++) {
                set(world, x0, y, z, Blocks.DARK_OAK_PLANKS);
                set(world, x0 + w - 1, y, z, Blocks.DARK_OAK_PLANKS);
            }
        }
        for (int x = x0; x < x0 + w; x++)
            for (int z = z0; z < z0 + d; z++)
                set(world, x, y0 + h + 1, z, Blocks.DARK_OAK_PLANKS);

        // Door opening and a simple oak door.
        set(world, x0 + w / 2, y0 + 1, z0, Blocks.AIR);
        set(world, x0 + w / 2, y0 + 2, z0, Blocks.AIR);
        set(world, x0 + w / 2, y0 + 1, z0 - 1, Blocks.OAK_DOOR);
        set(world, x0 + w / 2, y0 + 2, z0 - 1, Blocks.OAK_DOOR);

        // Windows.
        for (int x : new int[]{x0 + 2, x0 + 3, x0 + 7, x0 + 8}) {
            set(world, x, y0 + 2, z0, Blocks.GLASS_PANE);
            set(world, x, y0 + 2, z0 + d - 1, Blocks.GLASS_PANE);
        }
        for (int z : new int[]{z0 + 2, z0 + 6}) {
            set(world, x0, y0 + 2, z, Blocks.GLASS_PANE);
            set(world, x0 + w - 1, y0 + 2, z, Blocks.GLASS_PANE);
        }

        // Interior divider: living room + bedroom + strange hallway.
        for (int z = z0 + 1; z < z0 + d - 1; z++) {
            if (z != z0 + 4) set(world, x0 + 6, y0 + 1, z, Blocks.OAK_PLANKS);
        }
        for (int z = z0 + 1; z < z0 + d - 1; z++) {
            if (z != z0 + 4) set(world, x0 + 6, y0 + 2, z, Blocks.OAK_PLANKS);
        }

        // Living room: table, chairs and fireplace.
        set(world, x0 + 2, y0 + 1, z0 + 3, Blocks.OAK_PLANKS);
        set(world, x0 + 3, y0 + 1, z0 + 3, Blocks.OAK_PLANKS);
        set(world, x0 + 2, y0 + 2, z0 + 3, Blocks.OAK_FENCE);
        set(world, x0 + 3, y0 + 2, z0 + 3, Blocks.OAK_FENCE);
        set(world, x0 + 1, y0 + 1, z0 + 3, Blocks.OAK_STAIRS);
        set(world, x0 + 4, y0 + 1, z0 + 3, Blocks.OAK_STAIRS);
        set(world, x0 + 5, y0 + 1, z0 + 1, Blocks.COBBLESTONE);
        set(world, x0 + 5, y0 + 2, z0 + 1, Blocks.COBBLESTONE);
        set(world, x0 + 5, y0 + 3, z0 + 1, Blocks.SOUL_LANTERN);

        // Bedroom: bed and bedside table.
        set(world, x0 + 8, y0 + 1, z0 + 2, Blocks.RED_WOOL);
        set(world, x0 + 9, y0 + 1, z0 + 2, Blocks.RED_WOOL);
        set(world, x0 + 8, y0 + 1, z0 + 4, Blocks.OAK_PLANKS);
        set(world, x0 + 9, y0 + 1, z0 + 4, Blocks.CHEST);

        // Kitchen-ish corner.
        for (int x = x0 + 7; x <= x0 + 9; x++) set(world, x, y0 + 1, z0 + 6, Blocks.SMOOTH_STONE);
        set(world, x0 + 8, y0 + 2, z0 + 6, Blocks.FURNACE);
        set(world, x0 + 9, y0 + 2, z0 + 6, Blocks.CAULDRON);

        // Lights intentionally sparse for shader-friendly shadows.
        set(world, x0 + 3, y0 + 4, z0 + 4, Blocks.LANTERN);
        set(world, x0 + 8, y0 + 4, z0 + 5, Blocks.LANTERN);

        // Attic access and small attic.
        set(world, x0 + 2, y0 + 1, z0 + 7, Blocks.LADDER);
        set(world, x0 + 2, y0 + 2, z0 + 7, Blocks.LADDER);
        set(world, x0 + 2, y0 + 3, z0 + 7, Blocks.LADDER);
        set(world, x0 + 2, y0 + 4, z0 + 7, Blocks.LADDER);
        set(world, x0 + 2, y0 + 5, z0 + 7, Blocks.LADDER);

        // Empty attic floor and a single chest: gives the house an explorable second level.
        for (int x = x0 + 1; x < x0 + w - 1; x++)
            for (int z = z0 + 1; z < z0 + d - 1; z++)
                set(world, x, y0 + h + 1, z, Blocks.DARK_OAK_PLANKS);
        set(world, x0 + 8, y0 + h + 2, z0 + 5, Blocks.CHEST);
        set(world, x0 + 5, y0 + h + 2, z0 + 2, Blocks.COBWEB);
        set(world, x0 + 5, y0 + h + 2, z0 + 6, Blocks.COBWEB);
    }

    private static void makeHill(ServerWorld world, int cx, int cz, int height, int radius, int seed) {
        for (int y = 4; y < 4 + height; y++) {
            double frac = (y - 3) / (double) height;
            int r = Math.max(1, (int) Math.ceil(radius * (1.0 - frac)));
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    double dx = x - cx;
                    double dz = z - cz;
                    if (dx * dx + dz * dz <= r * r + ((x * 31L + z * 17L + seed) & 2)) {
                        set(world, x, y, z, y == 4 + height - 1 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
                    }
                }
            }
        }
    }


    public static boolean isInLiminal(ServerPlayerEntity player) {
        return player.getServerWorld().getRegistryKey().equals(LIMINAL_WORLD);
    }

    public static boolean canAskWhisper(ServerPlayerEntity player) {
        Long time = WHISPER_PENDING.get(player.getUuid());
        return time == null || player.getServerWorld().getTime() - time > 600;
    }

    public static void markWhisperPending(ServerPlayerEntity player) {
        WHISPER_PENDING.put(player.getUuid(), player.getServerWorld().getTime());
    }

    private static boolean handleWhisperChat(SignedMessage message, ServerPlayerEntity sender, net.minecraft.network.message.MessageType.Parameters params) {
        Long pending = WHISPER_PENDING.get(sender.getUuid());
        if (pending == null || !isInLiminal(sender)) return true;

        String answer = message.getContent().getString().trim().toLowerCase(java.util.Locale.ROOT);
        boolean yes = answer.equals("si") || answer.equals("sí") || answer.equals("yes") || answer.equals("s");
        boolean no = answer.equals("no") || answer.equals("n");
        if (!yes && !no) {
            sender.sendMessage(Text.literal("§8La cosa susurra: §7¿sí... o no?"), false);
            return false;
        }

        WhisperEntity nearest = null;
        double best = 64.0;
        for (WhisperEntity e : sender.getServerWorld().getEntitiesByType(WHISPER, e -> !e.isRemoved() && e.squaredDistanceTo(sender) <= 64.0)) {
            double d = e.squaredDistanceTo(sender);
            if (d < best) { best = d; nearest = e; }
        }
        if (nearest != null) nearest.answer(sender, yes);
        WHISPER_PENDING.remove(sender.getUuid());
        return false;
    }


    public static void returnPlayerToNormalWorld(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ReturnPoint point = RETURN_POINTS.remove(player.getUuid());
        ServerWorld overworld = server.getOverworld();
        if (point != null) {
            ServerWorld destination = server.getWorld(point.worldKey());
            if (destination == null) destination = overworld;
            player.teleport(destination, point.x(), point.y(), point.z(), Set.of(), point.yaw(), point.pitch(), true);
        } else {
            player.teleport(overworld, overworld.getSpawnPoint().getX() + 0.5, overworld.getSpawnPoint().getY() + 1.0, overworld.getSpawnPoint().getZ() + 0.5, Set.of(), 0.0f, 0.0f, true);
        }
    }

    private record ReturnPoint(RegistryKey<World> worldKey, double x, double y, double z, float yaw, float pitch) {}

    private static void set(ServerWorld world, int x, int y, int z, Block block) {
        world.setBlockState(new BlockPos(x, y, z), block.getDefaultState());
    }
}
