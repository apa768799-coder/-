package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
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
    public boolean autoMine = false;
    public boolean killAura = false;
    public boolean triggerBot = false;
    public boolean autoDodge = false;
    public boolean autoCrystal = false;
    public boolean surround = false;
    public boolean autoTrap = false;
    public boolean nuker = false;
    public boolean spider = false;
    
    public boolean xray = false;
    public boolean blockESP = false;
    public boolean playerESP = false;
    public boolean targetHUD = false;
    public boolean entityBox = false;
    public boolean freeLook = false;
    
    public boolean scaffold = false;
    public boolean sprint = false;
    public boolean noFall = false;
    public boolean elytraHelper = false;
    public boolean antiAfk = false;

    public boolean autoTotem = false;
    public boolean autoArmor = false;
    public boolean autoEat = false;
    public boolean autoFish = false;
    public boolean autoCraft = false;
    
    public boolean autoBuy = false;
    public boolean autoSell = false;

    public float killAuraRange = 3.5f;
    public String killAuraMode = "FunTime"; 
    public float reachDistance = 5.0f; 
    public float velocityPercent = 0.0f; 
    public int minHpToMine = 8;
    
    public float triggerBotRange = 4.0f;
    public int nukerRadius = 4;
    public String spiderMode = "Vanilla";

    private final Random random = new Random();
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();
    private final List<BlockPos> xrayBlocks = new ArrayList<>();
    private final List<BlockPos> espBlocks = new ArrayList<>();
    
    private BlockPos targetBlock = null;
    public LivingEntity targetHudEntity = null; 
    
    private int xrayTimer = 0;
    private int actionDelay = 0;
    private int attackCooldown = 0;
    private int regionEscapeTicks = 0;
    private long lastChatWarningTime = 0;
    private int jumpTicks = 0;

    private static final Map<Block, Integer> ORE_PRIORITY = new HashMap<>();
    static {
        ORE_PRIORITY.put(Blocks.DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.ANCIENT_DEBRIS, 90);
        ORE_PRIORITY.put(Blocks.GOLD_ORE, 75);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_GOLD_ORE, 75);
    }

    public Modules() {}

    public void copyFrom(Modules other) {
        this.autoMine = other.autoMine; this.killAura = other.killAura; this.triggerBot = other.triggerBot;
        this.autoDodge = other.autoDodge; this.autoCrystal = other.autoCrystal; this.surround = other.surround;
        this.autoTrap = other.autoTrap; this.nuker = other.nuker; this.spider = other.spider;
        this.xray = other.xray; this.blockESP = other.blockESP; this.playerESP = other.playerESP;
        this.targetHUD = other.targetHUD; this.entityBox = other.entityBox; this.freeLook = other.freeLook; 
        this.scaffold = other.scaffold; this.sprint = other.sprint; this.noFall = other.noFall; 
        this.elytraHelper = other.elytraHelper; this.antiAfk = other.antiAfk; this.autoTotem = other.autoTotem; 
        this.autoArmor = other.autoArmor; this.autoEat = other.autoEat; this.autoFish = other.autoFish; 
        this.autoCraft = other.autoCraft; this.autoBuy = other.autoBuy; this.autoSell = other.autoSell;
        this.killAuraRange = other.killAuraRange; this.killAuraMode = other.killAuraMode;
        this.reachDistance = other.reachDistance; this.velocityPercent = other.velocityPercent;
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
        if (jumpTicks > 0) jumpTicks--;

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
        if (nuker) doNuker(client, player);
        if (autoTrap && targetHudEntity != null) doAutoTrap(client, player);
        if (killAura) doKillAura(client, player);
        if (triggerBot) doTriggerBot(client, player);
        if (autoCrystal) doAutoCrystal(client, player);
        if (autoDodge) doAutoDodge(client, player);
        if (scaffold) doScaffold(client, player);
        if (surround) doSurround(client, player);
        if (spider) doSpider(client, player);
        if (elytraHelper) doElytraHelper(client, player);
        if (autoArmor) doAutoArmor(client, player);
        if (autoTotem) doAutoTotem(client, player);
        if (autoFish) doAutoFish(client, player);
        
        if (xray || blockESP) {
            xrayTimer++;
            if (xrayTimer >= 20) {
                updateXrayBlocks(client, player);
                if (blockESP) updateBlockESP(client, player);
                xrayTimer = 0;
            }
        }
    }

    // ДОБАВЛЕНО: Метод для миксина X-Ray
    public boolean isXrayBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA || b == Blocks.BEDROCK) return true;
        return ORE_PRIORITY.containsKey(b) || b == Blocks.CHEST || b == Blocks.ENDER_CHEST || b == Blocks.SPAWNER;
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f || attackCooldown > 0) return;
        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(killAuraRange + reachDistance), e -> e != player && e.isAlive()).stream().findFirst().orElse(null);
        targetHudEntity = target;
        if (target != null) {
            lookAtEntity(player, target);
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
            attackCooldown = killAuraMode.equals("Legit") ? 12 : 5;
        }
    }

    private void doTriggerBot(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f || attackCooldown > 0) return;
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (target instanceof LivingEntity && target != player && player.distanceTo(target) <= triggerBotRange) {
                client.interactionManager.attackEntity(player, (LivingEntity) target);
                player.swingHand(Hand.MAIN_HAND);
                attackCooldown = 5;
            }
        }
    }

    private void doAutoCrystal(MinecraftClient client, ClientPlayerEntity player) {
        if (attackCooldown > 0) return;
        for (EndCrystalEntity crystal : client.world.getEntitiesByClass(EndCrystalEntity.class, player.getBoundingBox().expand(4.5), e -> true)) {
            lookAtEntity(player, crystal);
            client.interactionManager.attackEntity(player, crystal);
            player.swingHand(Hand.MAIN_HAND);
            attackCooldown = 5;
            return;
        }
    }

    private void doAutoDodge(MinecraftClient client, ClientPlayerEntity player) {
        for (Entity e : client.world.getEntities()) {
            if (e instanceof PersistentProjectileEntity) {
                if (player.getPos().distanceTo(e.getPos()) < 4.0) {
                    player.setVelocity(player.getVelocity().add(random.nextBoolean() ? 0.4 : -0.4, 0, 0));
                }
            }
        }
    }

    private void doAutoTrap(MinecraftClient client, ClientPlayerEntity player) {
        if (targetHudEntity == null || player.getPos().distanceTo(targetHudEntity.getPos()) > 5.0) return;
        BlockPos targetPos = targetHudEntity.getBlockPos();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos pos = targetPos.offset(dir);
            if (client.world.getBlockState(pos).isAir()) {
                for (int i = 0; i < 9; i++) {
                    if (player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                        player.getInventory().selectedSlot = i;
                        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos.down()), Direction.UP, pos.down(), false);
                        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                        return;
                    }
                }
            }
        }
    }

    private void doNuker(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        for (int x = -nukerRadius; x <= nukerRadius; x++) {
            for (int y = -nukerRadius; y <= nukerRadius; y++) {
                for (int z = -nukerRadius; z <= nukerRadius; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (!client.world.getBlockState(pos).isAir() && client.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
                        client.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
                        player.swingHand(Hand.MAIN_HAND);
                        return;
                    }
                }
            }
        }
    }

    private void doSpider(MinecraftClient client, ClientPlayerEntity player) {
        if (player.horizontalCollision) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x, spiderMode.equals("Matrix") ? 0.42 : 0.2, vel.z);
        }
    }

    private void doElytraHelper(MinecraftClient client, ClientPlayerEntity player) {
        if (!player.isOnGround() && player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            if (player.fallDistance > 1.0f && !player.isGliding() && jumpTicks == 0) {
                client.options.jumpKey.setPressed(true);
                jumpTicks = 10;
            } else {
                client.options.jumpKey.setPressed(false);
            }
        }
    }

    // ИСПРАВЛЕНО: Метод для брони в 1.21.4
    private void doAutoArmor(MinecraftClient client, ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof ArmorItem) {
                EquipmentSlot slot = LivingEntity.getPreferredEquipmentSlot(stack);
                ItemStack current = player.getEquippedStack(slot);
                
                if (current.isEmpty() || stack.getMaxDamage() > current.getMaxDamage()) {
                    int slotId = i < 9 ? i + 36 : i;
                    client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player);
                    return;
                }
            }
        }
    }

    private void doAutoTotem(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        if (player.getHealth() < 10.0f || player.getOffHandStack().isEmpty()) {
            for (int i = 0; i < 36; i++) {
                if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    client.interactionManager.clickSlot(player.playerScreenHandler.syncId, i < 9 ? i + 36 : i, 40, SlotActionType.SWAP, player);
                    return;
                }
            }
        }
    }

    public void renderHUD(DrawContext context, float tickDelta) {
        if (targetHUD && targetHudEntity != null) {
            int x = context.getScaledWindowWidth() / 2 + 10;
            int y = context.getScaledWindowHeight() / 2 + 10;
            context.fill(x, y, x + 100, y + 40, new Color(0, 0, 0, 150).getRGB());
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, targetHudEntity.getName().getString(), x + 5, y + 5, -1);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "HP: " + (int)targetHudEntity.getHealth(), x + 5, y + 20, 0xFF0000);
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
                for (BlockPos pos : xrayBlocks) drawFullBox(matrices, lines, new Box(pos).expand(0.002), 0f, 1f, 1f, 1f);
            }
            if (playerESP || entityBox) {
                for (PlayerEntity p : MinecraftClient.getInstance().world.getPlayers()) {
                    if (p != MinecraftClient.getInstance().player) drawFullBox(matrices, lines, p.getBoundingBox().expand(0.1), 1f, 0f, 0f, 1f);
                }
            }
        }
        matrices.pop();
    }

    // Заглушки, чтобы не было ошибок компиляции
    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {}
    private void doScaffold(MinecraftClient client, ClientPlayerEntity player) {}
    private void doSurround(MinecraftClient client, ClientPlayerEntity player) {}
    private void doAutoFish(MinecraftClient client, ClientPlayerEntity player) {}
    private void updateXrayBlocks(MinecraftClient client, ClientPlayerEntity player) {}
    private void updateBlockESP(MinecraftClient client, ClientPlayerEntity player) {}
    private void drawFullBox(MatrixStack matrices, VertexConsumer lines, Box box, float r, float g, float b, float a) {}
    
    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F));
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, dist)));
    }
    private void lookAtEntity(ClientPlayerEntity player, Entity entity) { lookAt(player, entity.getBlockPos()); }
}
