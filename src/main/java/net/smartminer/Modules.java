package net.smartminer;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;

    private BlockPos targetBlock = null;
    private int breakCooldown = 0;
    private int fishTimer = 0;
    private int attackCooldown = 0;

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (breakCooldown > 0) breakCooldown--;
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;

        if (client.world.getBlockState(player.getBlockPos().down()).getBlock() == Blocks.LAVA) {
            client.options.jumpKey.setPressed(true);
        } else {
            client.options.jumpKey.setPressed(false);
        }

        if (autoMine) doAutoMine(client, player);
        if (autoFish) doAutoFish(client, player);
        if (killAura) doKillAura(client, player);
    }

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir()) {
            targetBlock = findNearestOre(player);
        }

        if (targetBlock != null) {
            double dist = player.getPos().distanceTo(Vec3d.ofCenter(targetBlock));
            if (dist > 3.5) {
                lookAt(player, targetBlock);
                client.options.forwardKey.setPressed(true);
                if (player.horizontalCollision) client.options.jumpKey.setPressed(true);
            } else {
                client.options.forwardKey.setPressed(false);
                lookAt(player, targetBlock);
                if (breakCooldown <= 0) {
                    client.interactionManager.updateBlockBreakingProgress(targetBlock, Direction.UP);
                    player.swingHand(Hand.MAIN_HAND);
                    breakCooldown = 2; 
                }
            }
        } else {
            client.options.forwardKey.setPressed(true);
        }
    }

    private void doAutoFish(MinecraftClient client, ClientPlayerEntity player) {
        FishingBobberEntity hook = null;
        for (net.minecraft.entity.Entity e : client.world.getEntities()) {
            if (e instanceof FishingBobberEntity && e.getOwner() == player) {
                hook = (FishingBobberEntity) e;
                break;
            }
        }

        if (hook == null && fishTimer <= 0) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
            fishTimer = 20; 
        } else if (hook != null) {
            if (hook.getVelocity().y < -0.08 || hook.isSubmerged()) {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                fishTimer = 20;
            }
        }
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (attackCooldown > 0) return;
        for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
            if (entity instanceof MobEntity && entity.isAlive() && player.distanceTo(entity) < 3.5) {
                lookAtEntity(player, entity);
                client.interactionManager.attackEntity(player, entity);
                player.swingHand(Hand.MAIN_HAND);
                attackCooldown = 12; 
                break;
            }
        }
    }

    private BlockPos findNearestOre(ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 15.0;
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    var block = player.getWorld().getBlockState(pos).getBlock();
                    if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
                        double d = pPos.getSquaredDistance(pos);
                        if (d < bestDist) { bestDist = d; nearest = pos; }
                    }
                }
            }
        }
        return nearest;
    }

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x; double dy = target.y - eye.y; double dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float dYaw = Math.floorMod((int) (targetYaw - player.getYaw() + 180), 360) - 180;
        float dPitch = targetPitch - player.getPitch();
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 20.0f));
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 20.0f));
    }

    private void lookAtEntity(ClientPlayerEntity player, net.minecraft.entity.Entity entity) {
        Vec3d target = entity.getPos();
        double dx = target.x - player.getX(); double dy = target.y + entity.getHeight() / 2 - player.getEyeY(); double dz = target.z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        player.setYaw(targetYaw + (float)(Math.random()*2-1)); 
        player.setPitch(targetPitch + (float)(Math.random()*2-1));
    }
}
