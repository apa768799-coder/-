package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;
    public boolean antiAfk = false;
    public boolean autoArmor = false;
    public boolean scaffold = false;
    public boolean velocity = false;
    public boolean chestStealer = false;
    
    public boolean autoTotem = false;
    public boolean noFall = false;
    public boolean autoRespawn = false;
    public boolean sprint = false;
    
    // ВИЗУАЛЫ
    public boolean fullBright = false;
    public boolean playerESP = false;
    public boolean xray = false;
    public boolean itemESP = false;
    public boolean chestESP = false;
    public boolean glowESP = false; 
    public boolean tracers = false; 

    // ВЫБОР РУД ДЛЯ X-RAY
    public boolean xrayDiamond = true;
    public boolean xrayIron = true;
    public boolean xrayGold = true;
    public boolean xrayEmerald = true;
    public boolean xrayCoal = false;
    public boolean xrayRedstone = false;
    public boolean xrayLapis = false;

    public int targetOreIndex = 0;
    public String[] oreNames = {"Алмаз", "Железо", "Золото", "Уголь", "Изумруд"};

    public String currentState = "Ожидание";
    public BlockPos targetBlock = null;
    private ItemEntity targetItem = null;
    private BlockPos targetChest = null;
    
    public LivingEntity killAuraTarget = null;

    private List<BlockPos> xrayBlocks = new ArrayList<>();
    private int xrayTimer = 0;

    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int afkTicks = 0;
    private int escapeTicks = 0;
    private int actionDelay = 0;
    private int targetItemTimer = 0;
    
    private int targetTimer = 0;
    private BlockPos currentlyBreaking = null;
    private int breakStuckTicks = 0;
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();
    
    private int stuckTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;
    private int strafeTicks = 0;
    private boolean strafeLeft = false;

    private Random random = new Random();
    private String lastLoggedState = "";

    public void copyFrom(Modules other) {
        this.autoMine = other.autoMine;
        this.autoFish = other.autoFish;
        this.killAura = other.killAura;
        this.antiAfk = other.antiAfk;
        this.autoArmor = other.autoArmor;
        this.scaffold = other.scaffold;
        this.velocity = other.velocity;
        this.chestStealer = other.chestStealer;
        this.autoTotem = other.autoTotem;
        this.noFall = other.noFall;
        this.autoRespawn = other.autoRespawn;
        this.sprint = other.sprint;
        
        this.fullBright = other.fullBright;
        this.playerESP = other.playerESP;
        this.xray = other.xray;
        this.itemESP = other.itemESP;
        this.chestESP = other.chestESP;
        this.glowESP = other.glowESP;
        this.tracers = other.tracers;

        this.xrayDiamond = other.xrayDiamond;
        this.xrayIron = other.xrayIron;
        this.xrayGold = other.xrayGold;
        this.xrayEmerald = other.xrayEmerald;
        this.xrayCoal = other.xrayCoal;
        this.xrayRedstone = other.xrayRedstone;
        this.xrayLapis = other.xrayLapis;

        this.targetOreIndex = other.targetOreIndex;
    }

    public boolean isXrayBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA) return true;
        if (b == Blocks.CHEST || b == Blocks.ENDER_CHEST || b == Blocks.TRAPPED_CHEST) return true;
        if (xrayDiamond && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (xrayIron && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (xrayGold && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE)) return true;
        if (xrayEmerald && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return true;
        if (xrayCoal && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE)) return true;
        if (xrayRedstone && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE)) return true;
        if (xrayLapis && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE)) return true;
        return false; 
    }

    private void updateState(String newState) {
        this.currentState = newState;
        if (!newState.equals(lastLoggedState)) {
            SmartClient.addLog(newState);
            lastLoggedState = newState;
        }
    }

    public void triggerRetreat() {
        this.escapeTicks = 40; 
        this.targetBlock = null;
        this.currentlyBreaking = null;
        this.breakStuckTicks = 0;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) player.setYaw(player.getYaw() + 180.0f + (float)(Math.random() * 30 - 15)); 
    }

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (actionDelay > 0) { actionDelay--; return; }
        if (strafeTicks > 0) strafeTicks--;

        long currentTime = System.currentTimeMillis();
        blacklistedBlocks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 20000);

        if (xray) {
            xrayTimer++;
            if (xrayTimer >= 20) { 
                updateXrayBlocks(client, player);
                xrayTimer = 0;
            }
        }

        if (glowESP && client.world != null) {
            for (Entity e : client.world.getEntities()) {
                if (e instanceof PlayerEntity && e != player) e.setGlowing(true);
                if (e instanceof MobEntity) e.setGlowing(true);
            }
        } else if (client.world != null) {
            for (Entity e : client.world.getEntities()) {
                if (e.isGlowing()) e.setGlowing(false);
            }
        }

        if (fullBright) client.options.getGamma().setValue(10.0);
        else client.options.getGamma().setValue(1.0);

        if (velocity && player.hurtTime > 0) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x * 0.2, vel.y, vel.z * 0.2);
        }

        if (sprint && player.input.movementForward > 0 && player.getHungerManager().getFoodLevel() > 6 && !player.isSneaking()) {
            player.setSprinting(true);
        }

        if (noFall && player.fallDistance > 2.0f) player.fallDistance = 0.0f; 

        if (player.getHungerManager().getFoodLevel() < 15) {
            doAutoEat(client, player);
            return; 
        }

        if (antiAfk) {
            afkTicks++;
            if (afkTicks > 100) {
                player.setYaw(player.getYaw() + (float)(Math.random() * 4 - 2));
                afkTicks = 0;
            }
        }

        if (autoTotem) doAutoTotem(client, player);
        if (autoArmor) doAutoArmor(client, player);
        if (chestStealer) doChestStealer(client, player);
        if (scaffold) doScaffold(client, player);
        if (autoMine) doAutoMine(client, player);
        if (autoFish) doAutoFish(client, player);
        if (killAura) doKillAura(client, player);

        if (autoRespawn && client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen) {
            player.requestRespawn();
            client.setScreen(null);
        }
    }

    private void updateXrayBlocks(MinecraftClient client, ClientPlayerEntity player) {
        xrayBlocks.clear();
        BlockPos pPos = player.getBlockPos();
        int radius = 25; 
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    Block b = client.world.getBlockState(pos).getBlock();
                    boolean isTarget = false;
                    if (xrayDiamond && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) isTarget = true;
                    if (xrayIron && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) isTarget = true;
                    if (xrayGold && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE)) isTarget = true;
                    if (xrayEmerald && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) isTarget = true;
                    if (xrayCoal && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE)) isTarget = true;
                    if (xrayRedstone && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE)) isTarget = true;
                    if (xrayLapis && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE)) isTarget = true;
                    if (isTarget) xrayBlocks.add(pos);
                }
            }
        }
    }

    private void doScaffold(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isOnGround() || player.isTouchingWater() || player.isInLava()) return;
        BlockPos below = player.getBlockPos().down();
        if (client.world.getBlockState(below).isAir() || client.world.getBlockState(below).getBlock() == Blocks.LAVA) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof BlockItem) {
                    if (player.getInventory().selectedSlot != i) player.getInventory().selectedSlot = i;
                    BlockPos neighbor = below.offset(player.getHorizontalFacing().getOpposite());
                    if (!client.world.getBlockState(neighbor).isAir() && client.world.getBlockState(neighbor).getBlock() != Blocks.LAVA) {
                        client.options.sneakKey.setPressed(true);
                        float prevYaw = player.getYaw();
                        float prevPitch = player.getPitch();
                        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(0, 0.5, 0);
                        double dx = hitVec.x - player.getX();
                        double dy = hitVec.y - player.getEyeY();
                        double dz = hitVec.z - player.getZ();
                        double dist = Math.sqrt(dx*dx + dz*dz);
                        float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
                        float targetPitch = (float)-Math.toDegrees(Math.atan2(dy, dist));
                        player.setYaw(targetYaw);
                        player.setPitch(targetPitch);
                        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, neighbor, false);
                        client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                        player.swingHand(Hand.MAIN_HAND);
                        player.setYaw(prevYaw);
                        player.setPitch(prevPitch);
                        return;
                    }
                }
            }
        } else {
            if (client.options.sneakKey.isPressed()) client.options.sneakKey.setPressed(false);
        }
    }

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (escapeTicks > 0) {
            escapeTicks--;
            updateState("Отступаю от привата...");
            client.options.forwardKey.setPressed(true); 
            client.options.backKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            return;
        }

        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isEmpty()) emptySlots++;
        }
        if (emptySlots == 0) {
            autoMine = false;
            updateState("Инвентарь полон! Выключаю AutoMine");
            return;
        }

        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) {
            targetItem = findNearestItem(client, player);
            targetItemTimer = 0;
        }

        if (targetItem != null) {
            targetItemTimer++;
            double dist = player.getPos().distanceTo(targetItem.getPos());
            if (targetItemTimer > 40 || (dist < 1.5 && targetItemTimer > 15)) {
                targetItem = null;
                targetItemTimer = 0;
            } else {
                updateState("Сбор лута");
                if (dist > 1.2) {
                    lookAtEntity(player, targetItem);
                    client.options.forwardKey.setPressed(true);
                    client.options.jumpKey.setPressed(false);
                } else {
                    client.options.forwardKey.setPressed(false);
                    client.options.jumpKey.setPressed(false);
                }
                return; 
            }
        }

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir() || blacklistedBlocks.containsKey(targetBlock)) {
            updateState("Сканирую руду");
            targetBlock = findNearestOre(client, player);
            targetTimer = 0;
        }

        if (targetBlock != null) {
            targetTimer++;
            if (targetTimer > 600) { 
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                return;
            }

            Vec3d eyePos = player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(targetCenter);

            if (dist <= 4.0) {
                updateState("Добываю " + oreNames[targetOreIndex]);
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                lookAt(player, targetBlock);
                
                if (client.world.getBlockState(targetBlock).isAir()) {
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                    targetBlock = null;
                    actionDelay = 3 + random.nextInt(5);
                } else {
                    breakBlock(client, player, targetBlock);
                }
                return;
            }

            int dy = targetBlock.getY() - player.getBlockPos().getY();
            
            if (dy >= 2) {
                updateState("Ступенька ВВЕРХ");
                BlockPos upFront = player.getBlockPos().offset(player.getHorizontalFacing()).up();
                lookAt(player, upFront);
                breakBlock(client, player, upFront);
                if (client.world.getBlockState(upFront).isAir() && player.horizontalCollision) {
                    client.options.jumpKey.setPressed(true);
                }
                return;
            }
            
            if (dy <= -2) {
                updateState("Ступенька ВНИЗ");
                BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
                lookAt(player, front);
                breakBlock(client, player, front);
                if (client.world.getBlockState(front).isAir()) client.options.forwardKey.setPressed(true);
                return;
            }

            updateState("Иду к руде");
            lookAt(player, targetBlock);

            BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
            BlockPos frontUp = front.up();

            if (client.world.getBlockState(front.down()).getBlock() == Blocks.LAVA) {
                updateState("Лава! Отмена");
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                return;
            }

            if (!client.world.getBlockState(front).isAir() || !client.world.getBlockState(frontUp).isAir()) {
                updateState("Копаю путь");
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                
                BlockPos breakTarget = !client.world.getBlockState(front).isAir() ? front : frontUp;
                lookAt(player, breakTarget);
                breakBlock(client, player, breakTarget);
                return;
            }

            client.options.forwardKey.setPressed(true);
            if (player.horizontalCollision && player.isOnGround() && client.world.getBlockState(front).isAir()) {
                client.options.jumpKey.setPressed(true);
            } else {
                client.options.jumpKey.setPressed(false);
            }
            return;
        }

        updateState("Руд нет, копаю туннель");
        BlockPos tunnelFront = player.getBlockPos().offset(player.getHorizontalFacing());
        BlockPos tunnelFrontUp = tunnelFront.up();

        if (client.world.getBlockState(tunnelFront.down()).getBlock() == Blocks.LAVA) {
            player.setYaw(player.getYaw() + 90.0f); 
            return;
        }

        if (!client.world.getBlockState(tunnelFront).isAir()) {
            lookAt(player, tunnelFront);
            breakBlock(client, player, tunnelFront);
        } else if (!client.world.getBlockState(tunnelFrontUp).isAir()) {
            lookAt(player, tunnelFrontUp);
            breakBlock(client, player, tunnelFrontUp);
        } else {
            client.options.forwardKey.setPressed(true);
        }
    }

    private void breakBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        Block b = client.world.getBlockState(pos).getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.END_PORTAL_FRAME) {
            blacklistedBlocks.put(pos, System.currentTimeMillis());
            return;
        }

        if (pos.equals(currentlyBreaking)) {
            breakStuckTicks++;
            if (breakStuckTicks > 100) { 
                updateState("Приват! Разворот");
                blacklistedBlocks.put(pos, System.currentTimeMillis()); 
                if (targetBlock != null) blacklistedBlocks.put(targetBlock, System.currentTimeMillis()); 
                triggerRetreat(); 
                return;
            }
        } else {
            currentlyBreaking = pos;
            breakStuckTicks = 0;
        }

        client.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        player.swingHand(Hand.MAIN_HAND);
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 25.0; 

        Block t1 = switch (targetOreIndex) {
            case 0 -> Blocks.DIAMOND_ORE; case 1 -> Blocks.IRON_ORE; case 2 -> Blocks.GOLD_ORE; case 3 -> Blocks.COAL_ORE; default -> Blocks.EMERALD_ORE;
        };
        Block t2 = switch (targetOreIndex) {
            case 0 -> Blocks.DEEPSLATE_DIAMOND_ORE; case 1 -> Blocks.DEEPSLATE_IRON_ORE; case 2 -> Blocks.DEEPSLATE_GOLD_ORE; case 3 -> Blocks.DEEPSLATE_COAL_ORE; default -> Blocks.DEEPSLATE_EMERALD_ORE;
        };

        for (int x = -15; x <= 15; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -15; z <= 15; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (blacklistedBlocks.containsKey(pos)) continue;

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

    private ItemEntity findNearestItem(MinecraftClient client, ClientPlayerEntity player) {
        return client.world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(5.0), 
            e -> e.isAlive() && !e.getStack().isEmpty())
            .stream()
            .min(Comparator.comparingDouble(e -> player.distanceTo(e)))
            .orElse(null);
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (attackCooldown > 0) return;

        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(3.8), 
            e -> e != player && e.isAlive() && !(e instanceof ArmorStandEntity))
            .stream()
            .sorted(Comparator.comparing((LivingEntity e) -> !(e instanceof PlayerEntity))
                    .thenComparingDouble(LivingEntity::getHealth))
            .findFirst()
            .orElse(null);

        killAuraTarget = target;

        if (target != null) {
            if (player.distanceTo(target) > 3.0) return;

            Vec3d eye = player.getEyePos();
            Vec3d targetPos = target.getEyePos();
            BlockHitResult hit = client.world.raycast(new RaycastContext(eye, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            
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
                if (Math.abs(MathHelper.wrapDegrees(player.getYaw() - getYawToEntity(player, target))) < 15) {
                    client.interactionManager.attackEntity(player, target);
                    player.swingHand(Hand.MAIN_HAND);
                    attackCooldown = 6 + random.nextInt(4); 
                }
            }
        }
    }

    private void doAutoTotem(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, i < 9 ? i + 36 : i, 40, SlotActionType.SWAP, player);
                return;
            }
        }
    }

    private void doAutoEat(MinecraftClient client, ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.contains(DataComponentTypes.FOOD)) {
                updateState("Кушаю...");
                if (player.getInventory().selectedSlot != i) player.getInventory().selectedSlot = i;
                client.options.useKey.setPressed(true);
                if (player.getHungerManager().getFoodLevel() >= 20) {
                    client.options.useKey.setPressed(false);
                }
                return;
            }
        }
    }

    private void doAutoArmor(MinecraftClient client, ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.contains(DataComponentTypes.EQUIPPABLE)) {
                EquippableComponent equip = stack.get(DataComponentTypes.EQUIPPABLE);
                if (equip != null && equip.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    EquipmentSlot slot = equip.slot();
                    ItemStack current = player.getEquippedStack(slot);
                    if (current.isEmpty() || stack.getMaxDamage() > current.getMaxDamage()) {
                        int slotId = i < 9 ? i + 36 : i; 
                        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player);
                        return;
                    }
                }
            }
        }
    }

    private void doChestStealer(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen) {
            net.minecraft.screen.GenericContainerScreenHandler handler = ((net.minecraft.client.gui.screen.ingame.GenericContainerScreen) client.currentScreen).getScreenHandler();
            for (int i = 0; i < handler.getInventory().size(); i++) {
                if (!handler.getSlot(i).getStack().isEmpty()) {
                    client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, player);
                    return;
                }
            }
            player.closeHandledScreen();
            return;
        }
        if (targetChest == null || client.world.getBlockState(targetChest).isAir() || !(client.world.getBlockState(targetChest).getBlock() instanceof ChestBlock)) {
            targetChest = findNearestChest(client, player);
        }
        if (targetChest != null && player.getPos().distanceTo(Vec3d.ofCenter(targetChest)) < 4.0) {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(targetChest), Direction.UP, targetChest, false);
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
            targetChest = null;
        }
    }

    private BlockPos findNearestChest(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (client.world.getBlockState(pos).getBlock() instanceof ChestBlock) return pos;
                }
            }
        }
        return null;
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
            if (fishTimer <= 0) {
                client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                player.swingHand(Hand.MAIN_HAND);
                fishTimer = 20 + random.nextInt(10);
            }
        }
        if (fishTimer > 0) fishTimer--;
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
            Vec3d eye = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getEyePos() : camPos;

            // X-RAY (Полные боксы)
            if (xray) {
                for (BlockPos pos : xrayBlocks) {
                    Block b = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
                    float r = 1.0f, g = 1.0f, bl = 1.0f;
                    if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) { r = 0.0f; g = 1.0f; bl = 1.0f; } 
                    else if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) { r = 0.0f; g = 1.0f; bl = 0.0f; } 
                    else if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE) { r = 1.0f; g = 1.0f; bl = 0.0f; } 
                    else if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) { r = 0.6f; g = 0.6f; bl = 0.6f; } 
                    else if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) { r = 0.0f; g = 0.0f; bl = 1.0f; } 
                    else if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) { r = 1.0f; g = 0.0f; bl = 0.0f; } 
                    else if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) { r = 0.2f; g = 0.2f; bl = 0.2f; }
                    
                    Box box = new Box(pos).expand(0.002);
                    drawFullBox(matrices, lines, box, r, g, bl, 1.0f);
                    
                    if (tracers) {
                        Vec3d targetVec = Vec3d.ofCenter(pos);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(r, g, bl, 1.0f).normal(0, 1, 0);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)targetVec.x, (float)targetVec.y, (float)targetVec.z).color(r, g, bl, 1.0f).normal(0, 1, 0);
                    }
                }
            }

            // Player ESP
            if (playerESP && MinecraftClient.getInstance().world != null) {
                for (PlayerEntity p : MinecraftClient.getInstance().world.getPlayers()) {
                    if (p != MinecraftClient.getInstance().player) {
                        Box box = p.getBoundingBox().expand(0.1);
                        drawFullBox(matrices, lines, box, 1.0f, 0.0f, 0.0f, 1.0f);
                        if (tracers) {
                            Vec3d targetVec = p.getPos();
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0, 1, 0);
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)targetVec.x, (float)targetVec.y, (float)targetVec.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0, 1, 0);
                        }
                    }
                }
            }

            // Chest ESP (ИСПРАВЛЕНО: Сканирование блоков вокруг игрока, а не blockEntities)
            if (chestESP && MinecraftClient.getInstance().world != null) {
                BlockPos pPos = MinecraftClient.getInstance().player.getBlockPos();
                int radius = 20;
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos pos = pPos.add(x, y, z);
                            Block b = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
                            if (b == Blocks.CHEST || b == Blocks.ENDER_CHEST || b == Blocks.TRAPPED_CHEST) {
                                Box box = new Box(pos).expand(0.002);
                                drawFullBox(matrices, lines, box, 1.0f, 0.5f, 0.0f, 1.0f);
                                if (tracers) {
                                    Vec3d targetVec = Vec3d.ofCenter(pos);
                                    lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(1.0f, 0.5f, 0.0f, 1.0f).normal(0, 1, 0);
                                    lines.vertex(matrices.peek().getPositionMatrix(), (float)targetVec.x, (float)targetVec.y, (float)targetVec.z).color(1.0f, 0.5f, 0.0f, 1.0f).normal(0, 1, 0);
                                }
                            }
                        }
                    }
                }
            }

            // Item ESP
            if (itemESP && MinecraftClient.getInstance().world != null) {
                for (Entity e : MinecraftClient.getInstance().world.getEntities()) {
                    if (e instanceof ItemEntity) {
                        Box box = e.getBoundingBox().expand(0.1);
                        drawFullBox(matrices, lines, box, 1.0f, 1.0f, 0.0f, 1.0f);
                        if (tracers) {
                            Vec3d targetVec = e.getPos();
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(1.0f, 1.0f, 0.0f, 1.0f).normal(0, 1, 0);
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)targetVec.x, (float)targetVec.y, (float)targetVec.z).color(1.0f, 1.0f, 0.0f, 1.0f).normal(0, 1, 0);
                        }
                    }
                }
            }
        }
        matrices.pop();
    }

    private void drawFullBox(MatrixStack matrices, VertexConsumer lines, Box box, float r, float g, float b, float a) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Нижняя грань
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);

        // Верхняя грань
        lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);

        // Вертикальные стойки
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
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
        
        float maxTurnSpeed = 3.0f + random.nextFloat() * 1.5f;
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), maxTurnSpeed)); 
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), maxTurnSpeed));
    }

    private void lookAtEntity(ClientPlayerEntity player, Entity entity) {
        Vec3d target = entity.getPos();
        double dx = target.x - player.getX(); 
        double dy = target.y + entity.getHeight() / 2 - player.getEyeY(); 
        double dz = target.z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float dPitch = targetPitch - player.getPitch();
        
        float maxTurnSpeed = 3.0f + random.nextFloat() * 1.5f;
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), maxTurnSpeed)); 
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), maxTurnSpeed));
    }

    private float getYawToEntity(ClientPlayerEntity player, Entity entity) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
    }
}
