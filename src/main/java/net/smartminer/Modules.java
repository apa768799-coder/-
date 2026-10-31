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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

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

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (breakCooldown > 0) breakCooldown--;
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;

        // FullBright
        if (fullBright) client.options.getGamma().setValue(10.0);
        else client.options.getGamma().setValue(1.0);

        // AutoSprint
        if (player.isOnGround() && player.input.movementForward > 0) player.setSprinting(true);

        // Anti-AFK
        if (antiAfk) {
            afkTicks++;
            if (afkTicks > 100) {
                player.setYaw(player.getYaw() + (float)(Math.random() * 4 - 2));
                if (Math.random() > 0.5) client.options.jumpKey.setPressed(true);
                afkTicks = 0;
            } else client.options.jumpKey.setPressed(false);
        }

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

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir()) {
            targetBlock = findNearestOre(client, player);
        }

        if (targetBlock != null) {
            Vec3d eyePos = player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);

            // --- УМНЫЙ RAYCAST (Проверка, видно ли руду) ---
            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    eyePos, targetCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
            ));
            
            BlockPos hitBlock = hit.getBlockPos();

            // Если луч уперся НЕ в руду, значит перед нами стена/камень
            if (!hitBlock.equals(targetBlock) && !client.world.getBlockState(hitBlock).isAir()) {
                double distToWall = player.getPos().distanceTo(Vec3d.ofCenter(hitBlock));
                
                if (distToWall <= 4.5) {
                    // Стена близко - ломаем её, чтобы прокопать путь к руде
                    client.options.forwardKey.setPressed(false);
                    client.options.jumpKey.setPressed(false);
                    lookAt(player, hitBlock);
                    if (breakCooldown <= 0) {
                        client.interactionManager.updateBlockBreakingProgress(hitBlock, Direction.UP);
                        player.swingHand(Hand.MAIN_HAND);
                        breakCooldown = 2;
                    }
                } else {
                    // Стена далеко - идем к ней
                    lookAt(player, hitBlock);
                    client.options.forwardKey.setPressed(true);
                }
                return; // Выходим, так как сначала надо сломать стену
            }

            // --- ЕСЛИ ПУТЬ СВОБОДЕН ---
            double distToOre = player.getPos().distanceTo(targetCenter);
            
            if (distToOre > 3.5) {
                lookAt(player, targetBlock);
                client.options.forwardKey.setPressed(true);
                client.options.jumpKey.setPressed(false); // Не прыгаем на ровном месте

                // Анти-Застревание (если идем-идем, а не движемся)
                double moved = player.getPos().squaredDistanceTo(lastPos);
                if (moved < 0.001) {
                    stuckTicks++;
                    if (stuckTicks > 15) {
                        client.options.jumpKey.setPressed(true); // Прыгаем, чтобы перепрыгнуть
                    }
                } else stuckTicks = 0;
                lastPos = player.getPos();

            } else {
                // Руда в зоне досягаемости, путь свободен - копаем!
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
            // Если руды не видно, просто идем вперед
            client.options.jumpKey.setPressed(false);
            client.options.forwardKey.setPressed(true);
        }
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 40.0; // Увеличили радиус поиска до 40 блоков

        Block t1 = switch (targetOreIndex) {
            case 0 -> Blocks.DIAMOND_ORE; case 1 -> Blocks.IRON_ORE; case 2 -> Blocks.GOLD_ORE; case 3 -> Blocks.COAL_ORE; default -> Blocks.EMERALD_ORE;
        };
        Block t2 = switch (targetOreIndex) {
            case 0 -> Blocks.DEEPSLATE_DIAMOND_ORE; case 1 -> Blocks.DEEPSLATE_IRON_ORE; case 2 -> Blocks.DEEPSLATE_GOLD_ORE; case 3 -> Blocks.DEEPSLATE_COAL_ORE; default -> Blocks.DEEPSLATE_EMERALD_ORE;
        };

        for (int x = -20; x <= 20; x++) {
            for (int y = -20; y <= 20; y++) {
                for (int z = -20; z <= 20; z++) {
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

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (attackCooldown > 0) return;
        
        if (!(player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof SwordItem || player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
            if (entity instanceof MobEntity && entity.isAlive() && player.distanceTo(entity) < 3.8) {
                lookAtEntity(player, entity);
                if (player.isOnGround()) {
                    client.options.jumpKey.setPressed(true);
                } else {
                    client.interactionManager.attackEntity(player, entity);
                    player.swingHand(Hand.MAIN_HAND);
                    attackCooldown = 12; 
                }
                break;
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
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 25.0f));
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 25.0f));
    }

    private void lookAtEntity(ClientPlayerEntity player, net.minecraft.entity.Entity entity) {
        Vec3d target = entity.getPos();
        double dx = target.x - player.getX(); double dy = target.y + entity.getHeight() / 2 - player.getEyeY(); double dz = target.z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        player.setYaw(targetYaw + (float)(Math.random()*3-1.5)); 
        player.setPitch(targetPitch + (float)(Math.random()*3-1.5));
    }
}
