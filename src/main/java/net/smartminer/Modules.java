package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
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

import java.awt.*;
import java.util.*;
import java.util.List;

public class Modules {
    // --- Флаги модулей ---
    public boolean autoMine = false;
    public boolean killAura = false;
    public boolean autoCrystal = false;
    public boolean surround = false;
    public boolean xray = false;
    public boolean scaffold = false;
    public boolean sprint = false;
    public boolean noFall = false;
    public boolean fullBright = false;
    public boolean playerESP = false;
    public boolean tracers = false;
    public boolean autoTotem = false;
    public boolean autoArmor = false;
    public boolean autoEat = false;

    // --- Настройки ---
    public float killAuraRange = 3.5f;
    public int minHpToMine = 8;
    public int minOrePriority = 30;

    // --- Внутренние переменные ---
    private final Random random = new Random();
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();
    private final List<BlockPos> xrayBlocks = new ArrayList<>();
    private BlockPos targetBlock = null;
    private int xrayTimer = 0;
    private int actionDelay = 0;
    private int attackCooldown = 0;
    private int regionEscapeTicks = 0;
    private long lastChatWarningTime = 0;

    private static final Map<Block, Integer> ORE_PRIORITY = new HashMap<>();
    static {
        ORE_PRIORITY.put(Blocks.DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.GOLD_ORE, 75);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_GOLD_ORE, 75);
        ORE_PRIORITY.put(Blocks.IRON_ORE, 60);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_IRON_ORE, 60);
        ORE_PRIORITY.put(Blocks.ANCIENT_DEBRIS, 90);
    }

    public void onChatMessage(String text) {
        if (text.contains("can't break") || text.contains("нельзя сломать") || text.contains("приват")) {
            lastChatWarningTime = System.currentTimeMillis();
            if (regionEscapeTicks <= 0) {
                regionEscapeTicks = 60;
                if (targetBlock != null) {
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis() + 9999999L);
                    targetBlock = null;
                }
            }
        }
    }

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (actionDelay > 0) { actionDelay--; return; }
        if (attackCooldown > 0) attackCooldown--;
        if (fullBright) client.options.getGamma().setValue(10.0);

        if (autoEat && player.getHungerManager().getFoodLevel() < 15) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).contains(net.minecraft.component.DataComponentTypes.FOOD)) {
                    player.getInventory().selectedSlot = i;
                    client.options.useKey.setPressed(true);
                    return;
                }
            }
        }

        if (autoMine) doAutoMine(client, player);
        if (killAura) doKillAura(client, player);
        if (autoCrystal) doAutoCrystal(client, player);
        if (scaffold) doScaffold(client, player);
        if (surround) doSurround(client, player);
        
        if (xray) {
            xrayTimer++;
            if (xrayTimer >= 20) { updateXrayBlocks(client, player); xrayTimer = 0; }
        }
    }

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (regionEscapeTicks > 0) {
            regionEscapeTicks--;
            client.options.backKey.setPressed(true);
            if (regionEscapeTicks % 10 == 0) player.setYaw(player.getYaw() + 90);
            if (regionEscapeTicks == 0) client.options.backKey.setPressed(false);
            return;
        }

        if (player.getHealth() < minHpToMine) return;

        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir()) {
            targetBlock = findBestOre(client, player);
        }

        if (targetBlock != null) {
            Vec3d center = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(center);

            if (dist <= 4.5) {
                client.options.forwardKey.setPressed(false);
                
                // Проверка видимости (Raycast)
                BlockHitResult hit = client.world.raycast(new RaycastContext(player.getEyePos(), center, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
                
                if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetBlock)) {
                    lookAt(player, targetBlock);
                    client.interactionManager.updateBlockBreakingProgress(targetBlock, Direction.UP);
                    player.swingHand(Hand.MAIN_HAND);
                    
                    if (System.currentTimeMillis() - lastChatWarningTime < 1000) {
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis() + 9999999L);
                        targetBlock = null;
                        regionEscapeTicks = 60;
                    }
                } else {
                    lookAt(player, hit.getBlockPos());
                    client.interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), Direction.UP);
                    player.swingHand(Hand.MAIN_HAND);
                }
            } else {
                lookAt(player, targetBlock);
                client.options.forwardKey.setPressed(true);
                if (player.horizontalCollision) client.options.jumpKey.setPressed(true);
            }
        } else {
            // Если руды нет, просто идем вперед
            client.options.forwardKey.setPressed(true);
        }
    }

    private BlockPos findBestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos best = null;
        int bestScore = -1;
        for (int x = -10; x <= 10; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (blacklistedBlocks.containsKey(pos)) continue;
                    int prio = ORE_PRIORITY.getOrDefault(client.world.getBlockState(pos).getBlock(), 0);
                    if (prio > minOrePriority) {
                        int score = prio - (int)player.getPos().distanceTo(Vec3d.ofCenter(pos));
                        if (score > bestScore) { bestScore = score; best = pos; }
                    }
                }
            }
        }
        return best;
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f || attackCooldown > 0) return;
        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(killAuraRange), e -> e != player && e.isAlive()).stream().findFirst().orElse(null);
        if (target != null) {
            lookAtEntity(player, target);
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            attackCooldown = 10; // Задержка удара
        }
    }

    private void doAutoCrystal(MinecraftClient client, ClientPlayerEntity player) {
        // Упрощенная логика: бьем кристаллы энда рядом
        for (EndCrystalEntity crystal : client.world.getEntitiesByClass(EndCrystalEntity.class, player.getBoundingBox().expand(4.0), e -> true)) {
            lookAtEntity(player, crystal);
            client.interactionManager.attackEntity(player, crystal);
            player.swingHand(Hand.MAIN_HAND);
            attackCooldown = 5;
            return;
        }
    }

    private void doScaffold(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isOnGround()) return;
        BlockPos below = player.getBlockPos().down();
        if (client.world.getBlockState(below).isAir()) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                    player.getInventory().selectedSlot = i;
                    BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(below.down()), Direction.UP, below.down(), false);
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                    player.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    private void doSurround(MinecraftClient client, ClientPlayerEntity player) {
        // Ставим обсидиан вокруг (очень упрощенно)
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos pos = player.getBlockPos().offset(dir);
            if (client.world.getBlockState(pos).isAir()) {
                for (int i = 0; i < 9; i++) {
                    if (player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                        player.getInventory().selectedSlot = i;
                        lookAt(player, pos.down());
                        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos.down()), Direction.UP, pos.down(), false);
                        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                        return;
                    }
                }
            }
        }
    }

    public void renderESP(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        Vec3d camPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumerProvider consumers = context.consumers();
        if (consumers != null) {
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            
            if (xray) {
                for (BlockPos pos : xrayBlocks) {
                    drawFullBox(matrices, lines, new Box(pos).expand(0.002), 0f, 1f, 1f, 1f);
                }
            }
            if (playerESP) {
                for (PlayerEntity p : MinecraftClient.getInstance().world.getPlayers()) {
                    if (p != MinecraftClient.getInstance().player) {
                        drawFullBox(matrices, lines, p.getBoundingBox().expand(0.1), 1f, 0f, 0f, 1f);
                    }
                }
            }
        }
        matrices.pop();
    }

    private void updateXrayBlocks(MinecraftClient client, ClientPlayerEntity player) {
        xrayBlocks.clear();
        BlockPos pPos = player.getBlockPos();
        for (int x = -15; x <= 15; x++) {
            for (int y = -15; y <= 15; y++) {
                for (int z = -15; z <= 15; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (ORE_PRIORITY.containsKey(client.world.getBlockState(pos).getBlock())) xrayBlocks.add(pos);
                }
            }
        }
    }

    public boolean isXrayBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA || b == Blocks.BEDROCK) return true;
        return ORE_PRIORITY.containsKey(b) || b == Blocks.CHEST || b == Blocks.SPAWNER;
    }

    private void drawFullBox(MatrixStack matrices, VertexConsumer lines, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
    }

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F));
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, dist)));
    }

    private void lookAtEntity(ClientPlayerEntity player, Entity entity) {
        lookAt(player, entity.getBlockPos());
    }
}
