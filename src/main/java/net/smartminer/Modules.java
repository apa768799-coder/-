package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;

    // 0 - Алмаз, 1 - Железо, 2 - Золото, 3 - Уголь, 4 - Изумруд
    public int targetOreIndex = 0;
    public String[] oreNames = {"Алмаз", "Железо", "Золото", "Уголь", "Изумруд"};

    public BlockPos targetBlock = null;
    private int breakCooldown = 0;
    private int fishTimer = 0;
    private int attackCooldown = 0;
    
    // Переменные для Анти-Застревания
    private int stuckTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;
    private boolean jumping = false;

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (breakCooldown > 0) breakCooldown--;
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;

        // Умная еда
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

        // ================= ЛОГИКА АНТИ-ЗАСТРЕВАНИЯ =================
        double moved = player.getPos().squaredDistanceTo(lastPos);
        if (moved < 0.001) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            jumping = false;
            client.options.jumpKey.setPressed(false);
        }
        lastPos = player.getPos();

        if (stuckTicks > 10 && targetBlock != null) {
            // Если застряли - начинаем прыгать и копать впереди себя
            if (!jumping) {
                jumping = true;
                client.options.jumpKey.setPressed(true);
            }
            
            // Раз в полсекунды дергаемся в сторону (стрейф)
            if (stuckTicks % 10 == 0) {
                client.options.leftKey.setPressed(!client.options.leftKey.isPressed());
                client.options.rightKey.setPressed(!client.options.rightKey.isPressed());
            }

            // И ломаем блок прямо перед носом, чтобы прокопать путь
            BlockPos frontBlock = player.getBlockPos().offset(player.getHorizontalFacing());
            if (!client.world.getBlockState(frontBlock).isAir() && breakCooldown <= 0) {
                client.interactionManager.updateBlockBreakingProgress(frontBlock, Direction.UP);
                player.swingHand(Hand.MAIN_HAND);
                breakCooldown = 2;
            }
            return; // Пока выбираемся, не ищем новую цель
        }
        // ============================================================

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
            // Ищем руду (если нет - просто идем)
            client.options.forwardKey.setPressed(true);
        }
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 25.0; // Радиус 25 блоков (видит далеко)

        Block t1 = switch (targetOreIndex) {
            case 0 -> Blocks.DIAMOND_ORE; case 1 -> Blocks.IRON_ORE; case 2 -> Blocks.GOLD_ORE; case 3 -> Blocks.COAL_ORE; default -> Blocks.EMERALD_ORE;
        };
        Block t2 = switch (targetOreIndex) {
            case 0 -> Blocks.DEEPSLATE_DIAMOND_ORE; case 1 -> Blocks.DEEPSLATE_IRON_ORE; case 2 -> Blocks.DEEPSLATE_GOLD_ORE; case 3 -> Blocks.DEEPSLATE_COAL_ORE; default -> Blocks.DEEPSLATE_EMERALD_ORE;
        };

        for (int x = -10; x <= 10; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    Block b = client.world.getBlockState(pos).getBlock();
                    if (b == t1 || b == t2) {
                        double d = pPos.getSquaredDistance(pos);
                        if (d < bestDist) { bestDist = d; nearest = pos; }
                    }
                }
            }
        }
        return nearest;
    }

    // ================== ВИЗУАЛЫ (ESP) ==================
    public void renderESP(WorldRenderContext context) {
        if (targetBlock == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d camPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Box box = new Box(targetBlock).expand(0.002);
        VertexConsumerProvider.Immediate consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        
        // Рисуем красную рамку вокруг блока
        WorldRenderer.drawBox(matrices, lines, box, 1.0f, 0.0f, 0.0f, 1.0f);
        
        consumers.draw();
        matrices.pop();
    }

    // ================== РЫБАЛКА И КОМБАТ ==================
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

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x; double dy = target.y - eye.y; double dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
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
