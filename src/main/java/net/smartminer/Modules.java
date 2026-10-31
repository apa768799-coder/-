package net.smartminer;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;

    // Выбор руды (0 - Алмаз, 1 - Железо, 2 - Золото, 3 - Уголь, 4 - Изумруд)
    public int targetOreIndex = 0;
    public String[] oreNames = {"Алмаз", "Железо", "Золото", "Уголь", "Изумруд"};

    private BlockPos targetBlock = null;
    private int breakCooldown = 0;
    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int stuckTimer = 0;
    private Vec3d lastPos = Vec3d.ZERO;

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (breakCooldown > 0) breakCooldown--;
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;

        // Умная еда (если голод < 15)
        if (player.getHungerManager().getFoodLevel() < 15 && !player.getMainHandStack().isEmpty() && player.getMainHandStack().contains(DataComponentTypes.FOOD)) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
        }

        // Защита от лавы
        if (client.world.getBlockState(player.getBlockPos().down()).getBlock() == Blocks.LAVA) {
            client.options.jumpKey.setPressed(true);
            client.options.forwardKey.setPressed(true);
            return;
        }

        if (autoMine) doAutoMine(client, player);
        if (autoFish) doAutoFish(client, player);
        if (killAura) doKillAura(client, player);
    }

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        // Авто-выбор кирки
        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        // Анти-застревание (если не двигались 20 тиков)
        if (player.getPos().squaredDistanceTo(lastPos) < 0.1 && targetBlock != null) {
            stuckTimer++;
            if (stuckTimer > 20) {
                client.options.jumpKey.setPressed(true);
                client.options.sneakKey.setPressed(true);
                if (stuckTimer > 40) { stuckTimer = 0; targetBlock = null; } // Сброс цели
            }
        } else {
            stuckTimer = 0;
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
        }
        lastPos = player.getPos();

        // Поиск цели
        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir()) {
            targetBlock = findNearestOre(client, player);
        }

        if (targetBlock != null) {
            double dist = player.getPos().distanceTo(Vec3d.ofCenter(targetBlock));
            if (dist > 3.5) {
                lookAt(player, targetBlock);
                client.options.forwardKey.setPressed(true);
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
            // Если руды нет, просто идем вперед
            client.options.forwardKey.setPressed(true);
        }
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 20.0; // Радиус поиска (20 блоков)

        // Выбираем блоки в зависимости от индекса
        Block targetBlockType = switch (targetOreIndex) {
            case 0 -> Blocks.DIAMOND_ORE;
            case 1 -> Blocks.IRON_ORE;
            case 2 -> Blocks.GOLD_ORE;
            case 3 -> Blocks.COAL_ORE;
            default -> Blocks.EMERALD_ORE;
        };
        Block targetDeepslateType = switch (targetOreIndex) {
            case 0 -> Blocks.DEEPSLATE_DIAMOND_ORE;
            case 1 -> Blocks.DEEPSLATE_IRON_ORE;
            case 2 -> Blocks.DEEPSLATE_GOLD_ORE;
            case 3 -> Blocks.DEEPSLATE_COAL_ORE;
            default -> Blocks.DEEPSLATE_EMERALD_ORE;
        };

        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    Block b = client.world.getBlockState(pos).getBlock();
                    if (b == targetBlockType || b == targetDeepslateType) {
                        double d = pPos.getSquaredDistance(pos);
                        if (d < bestDist) { bestDist = d; nearest = pos; }
                    }
                }
            }
        }
        return nearest;
    }

    // ================== ВИЗУАЛЫ (ESP) ==================
    public void renderESP(net.minecraft.client.render.WorldRenderContext context) {
        if (targetBlock == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Vec3d camPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        // Рисуем рамку вокруг блока
        Box box = new Box(targetBlock).expand(0.002);
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        
        // Рамка (красный цвет)
        net.minecraft.client.render.WorldRenderer.drawBox(matrices, lines, box, 1.0f, 0.0f, 0.0f, 1.0f);
        
        matrices.pop();
    }

    // ================== ОСТАЛЬНЫЕ МОДУЛИ ==================
    private void doAutoFish(MinecraftClient client, ClientPlayerEntity player) {
        FishingBobberEntity hook = null;
        for (net.minecraft.entity.Entity e : client.world.getEntities()) {
            if (e instanceof FishingBobberEntity && ((FishingBobberEntity) e).getPlayerOwner() == player) {
                hook = (FishingBobberEntity) e; break;
            }
        }
        if (hook == null && fishTimer <= 0) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
            fishTimer = 20; 
        } else if (hook != null && hook.getVelocity().y < -0.08) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
            fishTimer = 20;
        }
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (attackCooldown > 0) return;
        for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
            if (entity instanceof MobEntity && entity.isAlive() && player.distanceTo(entity) < 3.5) {
                lookAtEntity(player, entity);
                client.interactionManager.attackEntity(player, entity);
                player.swingHand(Hand.MAIN_HAND);
                attackCooldown = 12; break;
            }
        }
    }

    // Плавный поворот камеры
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
