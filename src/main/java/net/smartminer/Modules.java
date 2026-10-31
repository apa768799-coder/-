package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.Comparator;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;
    public boolean fullBright = false;
    public boolean antiAfk = false;

    public int targetOreIndex = 0;
    public String[] oreNames = {"Алмаз", "Железо", "Золото", "Уголь", "Изумруд"};

    public BlockPos targetBlock = null;
    private int breakCooldown = 0;
    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int stuckTicks = 0;
    private int afkTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;

    // Для умного обхода
    private int detourTicks = 0;
    private boolean detourLeft = false;

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (breakCooldown > 0) breakCooldown--;
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (detourTicks > 0) detourTicks--;

        if (fullBright) client.options.getGamma().setValue(10.0);
        else client.options.getGamma().setValue(1.0);

        if (player.isOnGround() && player.input.movementForward > 0) player.setSprinting(true);

        if (antiAfk) {
            afkTicks++;
            if (afkTicks > 100) {
                player.setYaw(player.getYaw() + (float)(Math.random() * 4 - 2));
                afkTicks = 0;
            }
        }

        if (player.getHungerManager().getFoodLevel() < 15 && !player.getMainHandStack().isEmpty() && player.getMainHandStack().contains(DataComponentTypes.FOOD)) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.swingHand(Hand.MAIN_HAND);
        }

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
        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        // Если мы в процессе обхода препятствия
        if (detourTicks > 0) {
            player.setYaw(player.getYaw() + (detourLeft ? -15 : 15));
            client.options.forwardKey.setPressed(true);
            client.options.jumpKey.setPressed(true);
            return;
        }

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir()) {
            targetBlock = findNearestOre(client, player);
        }

        if (targetBlock != null) {
            Vec3d eyePos = player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);

            // Проверка видимости руды
            BlockHitResult hit = client.world.raycast(new RaycastContext(eyePos, targetCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            BlockPos hitBlock = hit.getBlockPos();

            // Защита от копания под собой
            if (hitBlock.equals(player.getBlockPos()) || hitBlock.equals(player.getBlockPos().down()) || hitBlock.equals(player.getBlockPos().up())) {
                lookAt(player, targetBlock); // Просто смотрим на руду, чтобы не копать под собой
                return;
            }

            // Если перед нами стена, а руда за ней - копаем стену
            if (!hitBlock.equals(targetBlock) && !client.world.getBlockState(hitBlock).isAir()) {
                double distToWall = player.getPos().distanceTo(Vec3d.ofCenter(hitBlock));
                if (distToWall <= 4.5) {
                    client.options.forwardKey.setPressed(false);
                    lookAt(player, hitBlock);
                    if (breakCooldown <= 0) {
                        client.interactionManager.updateBlockBreakingProgress(hitBlock, Direction.UP);
                        player.swingHand(Hand.MAIN_HAND);
                        breakCooldown = 2;
                    }
                } else {
                    // Умный шаг (не идем в пропасть)
                    if (canWalkForward(client, player)) {
                        lookAt(player, hitBlock);
                        client.options.forwardKey.setPressed(true);
                    } else {
                        // Впереди обрыв, пытаемся обойти
                        startDetour(); 
                    }
                }
                return; 
            }

            // Если руда видна
            double distToOre = player.getPos().distanceTo(targetCenter);
            if (distToOre > 3.5) {
                if (canWalkForward(client, player)) {
                    lookAt(player, targetBlock);
                    client.options.forwardKey.setPressed(true);
                    
                    double moved = player.getPos().squaredDistanceTo(lastPos);
                    if (moved < 0.001) {
                        stuckTicks++;
                        if (stuckTicks > 10) { startDetour(); stuckTicks = 0; }
                    } else stuckTicks = 0;
                    lastPos = player.getPos();
                } else {
                    startDetour();
                }
            } else {
                // Руда близко, копаем
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                lookAt(player, targetBlock);
                if (breakCooldown <= 0) {
                    client.interactionManager.updateBlockBreakingProgress(targetBlock, Direction.UP);
                    player.swingHand(Hand.MAIN_HAND);
                    breakCooldown = 2; 
                }
            }
        } else {
            // Если руды нет, просто идем вперед (с проверкой на ямы)
            if (canWalkForward(client, player)) {
                client.options.forwardKey.setPressed(true);
            } else {
                startDetour();
            }
        }
    }

    // Проверка, есть ли под ногами впереди блок (защита от падения)
    private boolean canWalkForward(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
        BlockPos frontDown = front.down();
        BlockPos frontDown2 = frontDown.down();
        // Если под блоком впереди - воздух, и мы не в воде/на лестнице
        return !client.world.getBlockState(frontDown).isAir() || !client.world.getBlockState(frontDown2).isAir();
    }

    private void startDetour() {
        detourTicks = 20; // 1 секунда обхода
        detourLeft = Math.random() > 0.5;
        client.options.jumpKey.setPressed(true);
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 30.0; 

        Block t1 = switch (targetOreIndex) {
            case 0 -> Blocks.DIAMOND_ORE; case 1 -> Blocks.IRON_ORE; case 2 -> Blocks.GOLD_ORE; case 3 -> Blocks.COAL_ORE; default -> Blocks.EMERALD_ORE;
        };
        Block t2 = switch (targetOreIndex) {
            case 0 -> Blocks.DEEPSLATE_DIAMOND_ORE; case 1 -> Blocks.DEEPSLATE_IRON_ORE; case 2 -> Blocks.DEEPSLATE_GOLD_ORE; case 3 -> Blocks.DEEPSLATE_COAL_ORE; default -> Blocks.DEEPSLATE_EMERALD_ORE;
        };

        for (int x = -15; x <= 15; x++) {
            for (int y = -15; y <= 15; y++) {
                for (int z = -15; z <= 15; z++) {
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

    public void renderESP(WorldRenderContext context) {
        if (targetBlock == null) return;
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d camPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Box box = new Box(targetBlock).expand(0.002);
        VertexConsumerProvider consumers = context.consumers();
        if (consumers != null) {
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            VertexRendering.drawBox(matrices, lines, box, 0.0f, 1.0f, 1.0f, 1.0f); 
            if (MinecraftClient.getInstance().player != null) {
                Vec3d eye = MinecraftClient.getInstance().player.getEyePos();
                Vec3d targetVec = Vec3d.ofCenter(targetBlock);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                lines.vertex(matrix, (float)eye.x, (float)eye.y, (float)eye.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0f, 1f, 0f);
                lines.vertex(matrix, (float)targetVec.x, (float)targetVec.y, (float)targetVec.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0f, 1f, 0f);
            }
        }
        matrices.pop();
    }

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

    // Безопасная KillAura (не бьет сквозь стены, ждет кулдаун)
    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        // Ждем полной готовности удара
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (attackCooldown > 0) return;

        // Выбираем ближайшую цель
        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(3.8), 
            e -> e != player && e.isAlive())
            .stream()
            .min(Comparator.comparingDouble(e -> player.distanceTo(e)))
            .orElse(null);

        if (target != null) {
            // Проверка видимости (чтобы не бить сквозь стены)
            Vec3d eye = player.getEyePos();
            Vec3d targetPos = target.getEyePos();
            BlockHitResult hit = client.world.raycast(new RaycastContext(eye, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            
            // Если луч не встретил блоков на пути - цель видна
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target.getBlockPos())) {
                if (!(player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem)) {
                    for (int i = 0; i < 9; i++) {
                        if (player.getInventory().getStack(i).getItem() instanceof SwordItem) {
                            player.getInventory().selectedSlot = i;
                            break;
                        }
                    }
                }
                
                lookAtEntity(player, target);
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);
                attackCooldown = 12; // Задержка
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
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 15.0f)); // Плавнее
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 15.0f));
    }

    private void lookAtEntity(ClientPlayerEntity player, net.minecraft.entity.Entity entity) {
        Vec3d target = entity.getPos();
        double dx = target.x - player.getX(); 
        double dy = target.y + entity.getHeight() / 2 - player.getEyeY(); 
        double dz = target.z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float dPitch = targetPitch - player.getPitch();
        
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 20.0f)); 
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 20.0f));
    }
}
