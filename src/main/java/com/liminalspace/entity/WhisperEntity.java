package com.liminalspace.entity;

import com.liminalspace.LiminalSpaceMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class WhisperEntity extends PathAwareEntity {
    private static final int QUESTION_RANGE = 6 * 6;
    private static final int ATTACK_RANGE = 12 * 12;
    private UUID targetPlayer;
    private boolean hostile;
    private int soundTimer;
    private int attackTimer;

    public WhisperEntity(EntityType<? extends WhisperEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.setPersistent();
        this.experiencePoints = 0;
    }

    @Override
    protected void initGoals() {
        // Deliberately empty: it should remain in the house until the player answers.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) return;

        if (!(this.getWorld() instanceof ServerWorld world)) return;

        soundTimer++;
        if (soundTimer >= 70 + this.random.nextInt(90)) {
            soundTimer = 0;
            if (this.random.nextBoolean()) {
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_AMBIENT,
                        SoundCategory.HOSTILE, 0.35f, 0.45f + this.random.nextFloat() * 0.25f);
            } else {
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_SILVERFISH_AMBIENT,
                        SoundCategory.HOSTILE, 0.22f, 0.45f + this.random.nextFloat() * 0.35f);
            }
        }

        if (!hostile && targetPlayer == null) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.squaredDistanceTo(this) <= QUESTION_RANGE &&
                        LiminalSpaceMod.isInLiminal(player) &&
                        LiminalSpaceMod.canAskWhisper(player)) {
                    targetPlayer = player.getUuid();
                    LiminalSpaceMod.markWhisperPending(player);
                    player.sendMessage(Text.literal("§8[Algo detrás de ti] §7...¿estás solo?"), false);
                    world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_AMBIENT,
                            SoundCategory.HOSTILE, 0.28f, 0.38f);
                    break;
                }
            }
        }

        if (hostile && targetPlayer != null) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(targetPlayer);
            if (player == null || !player.isAlive() || !LiminalSpaceMod.isInLiminal(player)) {
                targetPlayer = null;
                return;
            }

            Vec3d delta = player.getPos().subtract(this.getPos());
            double distance = delta.lengthSquared();
            if (distance > 4.0 && distance < ATTACK_RANGE) {
                Vec3d velocity = delta.normalize().multiply(0.075);
                this.setVelocity(velocity.x, velocity.y * 0.35, velocity.z);
                this.velocityDirty = true;
                this.setYaw(this.getYaw() + 5.5f);
            } else if (distance <= 4.0) {
                this.setVelocity(Vec3d.ZERO);
                attackTimer++;
                if (attackTimer >= 20) {
                    attackTimer = 0;
                    player.damage(world.getDamageSources().mobAttack(this), 8.0f);
                    world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
                            SoundCategory.HOSTILE, 0.75f, 0.55f);
                }
            }
        }
    }

    public boolean isHostile() {
        return hostile;
    }

    public void answer(ServerPlayerEntity player, boolean alone) {
        if (hostile || !player.getUuid().equals(targetPlayer)) return;

        if (!alone) {
            player.sendMessage(Text.literal("§7La entidad inclina la cabeza. §8" +
                    ""), false);
            LiminalSpaceMod.returnPlayerToNormalWorld(player);
            this.discard();
            return;
        }

        hostile = true;
        targetPlayer = player.getUuid();
        attackTimer = 0;
        player.sendMessage(Text.literal("§4...entonces no hay nadie que te encuentre."), false);
        player.sendMessage(Text.literal("§8La mandíbula se abre demasiado..."), false);
        ServerWorld world = (ServerWorld) this.getWorld();
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.HOSTILE, 1.0f, 0.65f);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer == null && !hostile) {
            targetPlayer = serverPlayer.getUuid();
            LiminalSpaceMod.markWhisperPending(serverPlayer);
            serverPlayer.sendMessage(Text.literal("§8[La cosa] §7¿Estás solo?"), false);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }
}
