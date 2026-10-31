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
    // Существующие
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

    // Визуалы
    public boolean fullBright = false;
    public boolean playerESP = false;
    public boolean xray = false;
    public boolean itemESP = false;
    public boolean chestESP = false;
    public boolean glowESP = false;
    public boolean tracers = false;
    public boolean trajectory = false;
    public boolean criticals = false;
    public boolean hitParticles = false;
    public boolean damageNumbers = false;
    public boolean hitCounter = false;
    public boolean lowHp = false;

    // НОВЫЕ МОДУЛИ v3.0
    public boolean holeESP = false;
    public boolean autoTrap = false;

    // Умный AutoMine
    public boolean smartOreSelection = true;   // true = копать всё ценное по приоритету
    public int     minOrePriority    = 0;      // 0 = все, 30 = без угля, 90 = только алмаз/изумруд
    public int     minHpToMine       = 6;      // не копать, если HP ниже

    // Руды для X-Ray
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
    public int currentHitCount = 0;

    private List<BlockPos> xrayBlocks = new ArrayList<>();
    private List<BlockPos> holeBlocks = new ArrayList<>();
    private int xrayTimer = 0;
    private int holeTimer = 0;

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

    // Очередь незаконченной жилы
    private final Map<BlockPos, Long> veinBlocks = new HashMap<>();

    // Умное поведение при застревании
    private BlockPos currentBreakTarget = null;
    private int totalStuckTicks = 0;
    private int stuckEscalationLevel = 0;
    private Vec3d stuckStartPos = null;
    private BlockPos alternateApproachTarget = null;
    private BlockPos lastTargetBlock = null;

    private int stuckTicks = 0;
    private Vec3d lastPos = Vec3d.ZERO;
    private int strafeTicks = 0;
    private boolean strafeLeft = false;

    private Random random = new Random();
    private String lastLoggedState = "";

    public List<DamageNumber> damageNumbersList = new ArrayList<>();

    // Приоритеты руд
    private static final Map<Block, Integer> ORE_PRIORITY = new HashMap<>();
    static {
        ORE_PRIORITY.put(Blocks.DIAMOND_ORE,            100);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_DIAMOND_ORE,  100);
        ORE_PRIORITY.put(Blocks.EMERALD_ORE,             95);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_EMERALD_ORE,   95);
        ORE_PRIORITY.put(Blocks.ANCIENT_DEBRIS,          90);
        ORE_PRIORITY.put(Blocks.GOLD_ORE,                75);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_GOLD_ORE,      75);
        ORE_PRIORITY.put(Blocks.NETHER_GOLD_ORE,         70);
        ORE_PRIORITY.put(Blocks.IRON_ORE,                60);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_IRON_ORE,      60);
        ORE_PRIORITY.put(Blocks.LAPIS_ORE,               55);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_LAPIS_ORE,     55);
        ORE_PRIORITY.put(Blocks.REDSTONE_ORE,            50);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_REDSTONE_ORE,  50);
        ORE_PRIORITY.put(Blocks.NETHER_QUARTZ_ORE,       45);
        ORE_PRIORITY.put(Blocks.COPPER_ORE,              40);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COPPER_ORE,    40);
        ORE_PRIORITY.put(Blocks.COAL_ORE,                30);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COAL_ORE,      30);
    }

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
        this.trajectory = other.trajectory;
        this.criticals = other.criticals;
        this.hitParticles = other.hitParticles;
        this.damageNumbers = other.damageNumbers;
        this.hitCounter = other.hitCounter;
        this.lowHp = other.lowHp;
        this.holeESP = other.holeESP;
        this.autoTrap = other.autoTrap;

        this.xrayDiamond = other.xrayDiamond;
        this.xrayIron = other.xrayIron;
        this.xrayGold = other.xrayGold;
        this.xrayEmerald = other.xrayEmerald;
        this.xrayCoal = other.xrayCoal;
        this.xrayRedstone = other.xrayRedstone;
        this.xrayLapis = other.xrayLapis;

        this.targetOreIndex = other.targetOreIndex;

        this.smartOreSelection = other.smartOreSelection;
        this.minOrePriority    = other.minOrePriority;
        this.minHpToMine       = other.minHpToMine;
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
        damageNumbersList.removeIf(dn -> currentTime - dn.spawnTime > 1000);

        if (xray) {
            xrayTimer++;
            if (xrayTimer >= 20) {
                updateXrayBlocks(client, player);
                xrayTimer = 0;
            }
        }

        if (holeESP) {
            holeTimer++;
            if (holeTimer >= 20) {
                updateHoleBlocks(client, player);
                holeTimer = 0;
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
        if (autoTrap) doAutoTrap(client, player);

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

    private void updateHoleBlocks(MinecraftClient client, ClientPlayerEntity player) {
        holeBlocks.clear();
        BlockPos pPos = player.getBlockPos();
        int radius = 10;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (!client.world.getBlockState(pos).isAir()) continue;
                    if (!client.world.getBlockState(pos.up()).isAir()) continue;
                    if (!client.world.getBlockState(pos.up(2)).isAir()) continue;

                    Block floor = client.world.getBlockState(pos.down()).getBlock();
                    if (floor != Blocks.OBSIDIAN && floor != Blocks.BEDROCK) continue;

                    Block n1 = client.world.getBlockState(pos.north()).getBlock();
                    Block s1 = client.world.getBlockState(pos.south()).getBlock();
                    Block e1 = client.world.getBlockState(pos.east()).getBlock();
                    Block w1 = client.world.getBlockState(pos.west()).getBlock();

                    int solidCount = 0;
                    if (n1 == Blocks.OBSIDIAN || n1 == Blocks.BEDROCK) solidCount++;
                    if (s1 == Blocks.OBSIDIAN || s1 == Blocks.BEDROCK) solidCount++;
                    if (e1 == Blocks.OBSIDIAN || e1 == Blocks.BEDROCK) solidCount++;
                    if (w1 == Blocks.OBSIDIAN || w1 == Blocks.BEDROCK) solidCount++;

                    if (solidCount >= 2) holeBlocks.add(pos);
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

    private void doAutoTrap(MinecraftClient client, ClientPlayerEntity player) {
        if (killAuraTarget == null || !killAuraTarget.isAlive()) return;
        if (player.getPos().distanceTo(killAuraTarget.getPos()) > 5.0) return;

        int obbySlot = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                obbySlot = i;
                break;
            }
        }
        if (obbySlot == -1) return;
        if (player.getInventory().selectedSlot != obbySlot) {
            player.getInventory().selectedSlot = obbySlot;
            return;
        }

        BlockPos targetPos = killAuraTarget.getBlockPos();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction dir : dirs) {
            BlockPos placePos = targetPos.offset(dir);
            if (!client.world.getBlockState(placePos).isAir()) continue;
            if (!client.world.getBlockState(placePos.up()).isAir()) continue;

            BlockPos neighbor = placePos.down();
            if (client.world.getBlockState(neighbor).isAir()) continue;

            Vec3d hitVec = Vec3d.ofCenter(neighbor).add(0, 0.5, 0);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, neighbor, false);
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
            player.swingHand(Hand.MAIN_HAND);
            return;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //               УМНЫЙ AUTO MINE
    // ═══════════════════════════════════════════════════════════
    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (escapeTicks > 0) {
            escapeTicks--;
            updateState("Отступаю от привата...");
            client.options.forwardKey.setPressed(true);
            client.options.backKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            return;
        }

        if (player.getHealth() < minHpToMine) {
            updateState("Мало HP (" + (int) player.getHealth() + ") — пауза");
            client.options.forwardKey.setPressed(false);
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

        long now = System.currentTimeMillis();
        veinBlocks.entrySet().removeIf(e ->
            client.world.getBlockState(e.getKey()).isAir()
            || now - e.getValue() > 60_000L
            || blacklistedBlocks.containsKey(e.getKey()));

        if (!veinBlocks.isEmpty()) {
            BlockPos bestVein = null;
            double bestVDist = 30.0;
            for (BlockPos vb : veinBlocks.keySet()) {
                double d = player.getPos().distanceTo(Vec3d.ofCenter(vb));
                if (d < bestVDist) { bestVDist = d; bestVein = vb; }
            }
            if (bestVein != null) {
                targetBlock = bestVein;
                targetTimer = 0;
            }
        }

        if (targetBlock == null
            || client.world.getBlockState(targetBlock).isAir()
            || blacklistedBlocks.containsKey(targetBlock)) {
            updateState(smartOreSelection ? "Умный скан руд..." : "Сканирую руду");
            targetBlock = smartOreSelection
                ? findBestOre(client, player)
                : findNearestOre(client, player);
            targetTimer = 0;

            if (targetBlock != null && !targetBlock.equals(lastTargetBlock)) {
                resetStuckState();
                lastTargetBlock = targetBlock;
            }
        }

        if (targetBlock != null) {
            targetTimer++;
            if (targetTimer > 600) {
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                veinBlocks.clear();
                resetStuckState();
                return;
            }

            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(targetCenter);

            if (dist <= 4.0) {
                updateState("Добываю " + blockName(client.world.getBlockState(targetBlock).getBlock()));
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                lookAt(player, targetBlock);

                // Блок сломан — успех
                if (client.world.getBlockState(targetBlock).isAir()) {
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                    if (smartOreSelection) addVeinBlocks(client, targetBlock);
                    veinBlocks.remove(targetBlock);
                    targetBlock = null;
                    resetStuckState();
                    actionDelay = 3 + random.nextInt(5);
                    return;
                }

                // Считаем тики на конкретном блоке
                if (!targetBlock.equals(currentBreakTarget)) {
                    currentBreakTarget = targetBlock;
                    totalStuckTicks = 0;
                }
                totalStuckTicks++;

                // ВСЕГДА копаем
                breakBlock(client, player, targetBlock);

                // ── 2-4 сек: обкусываем соседей ──
                if (totalStuckTicks > 40 && totalStuckTicks <= 100) {
                    if (stuckEscalationLevel < 1) {
                        stuckEscalationLevel = 1;
                        updateState("Обнажаю руду — копаю рядом");
                    }
                    if (totalStuckTicks % 5 == 0) {
                        for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                            BlockPos adj = targetBlock.offset(d);
                            Block adjBlock = client.world.getBlockState(adj).getBlock();
                            if (adjBlock == Blocks.AIR || adjBlock == Blocks.BEDROCK
                                || adjBlock == Blocks.BARRIER || adjBlock == Blocks.OBSIDIAN) continue;
                            float hardness = adjBlock.getHardness();
                            if (hardness >= 0 && hardness < 4.0f) {
                                lookAt(player, adj);
                                breakBlock(client, player, adj);
                                return;
                            }
                        }
                    }
                    return;
                }

                // ── 4-10 сек: обход с другой стороны, продолжая бить ──
                if (totalStuckTicks > 100 && totalStuckTicks <= 200) {
                    if (stuckEscalationLevel < 2) {
                        stuckEscalationLevel = 2;
                        updateState("Меняю угол — обхожу");
                        alternateApproachTarget = findAlternateApproachPosition(client, player, targetBlock);
                    }
                    if (alternateApproachTarget != null) {
                        lookAt(player, alternateApproachTarget);
                        client.options.forwardKey.setPressed(true);
                        if (player.horizontalCollision && player.isOnGround()) player.jump();
                    }
                    return;
                }

                // ── >10 сек: не берётся — короткий блэклист и ищем другую ──
                if (totalStuckTicks > 200) {
                    updateState("Руда не берётся — ищу другую");
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                    veinBlocks.remove(targetBlock);
                    targetBlock = null;
                    resetStuckState();
                }
                return;
            }

            // ───── Навигация (твоя, без изменений) ─────
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
                resetStuckState();
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

        // ───── Туннель (твой, без изменений) ─────
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

    // ═══════════════════════════════════════════════════════════
    //                     ХЕЛПЕРЫ
    // ═══════════════════════════════════════════════════════════
    private int orePriority(Block b) {
        return ORE_PRIORITY.getOrDefault(b, 0);
    }

    private BlockPos findBestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;

        for (int x = -15; x <= 15; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -15; z <= 15; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (blacklistedBlocks.containsKey(pos)) continue;

                    Block b = client.world.getBlockState(pos).getBlock();
                    int prio = orePriority(b);
                    if (prio <= 0 || prio < minOrePriority) continue;

                    double dist = player.getPos().distanceTo(Vec3d.ofCenter(pos));
                    if (dist > 25.0) continue;

                    int score = prio * 10 - (int) (dist * 3);
                    if (isOreExposed(client, pos)) score += 15;

                    if (score > bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private boolean isOreExposed(MinecraftClient client, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (client.world.getBlockState(pos.offset(dir)).isAir()) return true;
        }
        return false;
    }

    private void addVeinBlocks(MinecraftClient client, BlockPos center) {
        Block origin = client.world.getBlockState(center).getBlock();
        int prio = orePriority(origin);
        if (prio < 45) return;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos n = center.add(dx, dy, dz);
                    if (client.world.getBlockState(n).getBlock() != origin) continue;
                    if (blacklistedBlocks.containsKey(n)) continue;
                    veinBlocks.putIfAbsent(n, System.currentTimeMillis());
                }
            }
        }
    }

    private BlockPos findAlternateApproachPosition(MinecraftClient client, ClientPlayerEntity player, BlockPos ore) {
        BlockPos best = null;
        double bestScore = -1.0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) < 2) continue;
                BlockPos p = ore.add(dx, 0, dz);
                if (!client.world.getBlockState(p).isAir()) continue;
                if (!client.world.getBlockState(p.up()).isAir()) continue;
                if (client.world.getBlockState(p.down()).isAir()) continue;
                if (blacklistedBlocks.containsKey(p)) continue;

                double oreDist = Vec3d.ofCenter(p).distanceTo(Vec3d.ofCenter(ore));
                if (oreDist > 4.5) continue;

                double playerDist = player.getPos().distanceTo(Vec3d.ofCenter(p));
                double score = 10.0 - playerDist;
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
        }
        return best;
    }

    private void resetStuckState() {
        currentBreakTarget = null;
        breakStuckTicks = 0;
        totalStuckTicks = 0;
        stuckEscalationLevel = 0;
        stuckStartPos = null;
        alternateApproachTarget = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
        }
    }

    private String blockName(Block b) {
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return "алмаз";
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return "изумруд";
        if (b == Blocks.ANCIENT_DEBRIS) return "древние обломки";
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return "золото";
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return "железо";
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return "лазурит";
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return "редстоун";
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return "медь";
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return "уголь";
        if (b == Blocks.NETHER_QUARTZ_ORE) return "кварц";
        if (b == Blocks.STONE) return "камень";
        if (b == Blocks.DEEPSLATE) return "глубинносланец";
        if (b == Blocks.DIRT) return "земля";
        if (b == Blocks.GRAVEL) return "гравий";
        return "блок";
    }

    private void breakBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        Block b = client.world.getBlockState(pos).getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.BARRIER || b == Blocks.END_PORTAL_FRAME) {
            blacklistedBlocks.put(pos, System.currentTimeMillis());
            return;
        }

        Direction face = getBreakFace(player, pos);
        client.interactionManager.updateBlockBreakingProgress(pos, face);
        player.swingHand(Hand.MAIN_HAND);
    }

    private Direction getBreakFace(ClientPlayerEntity player, BlockPos pos) {
        Vec3d eye = player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;

        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
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

                    float damage = 5.0f;
                    if (damageNumbers) damageNumbersList.add(new DamageNumber(target.getPos(), damage, false));
                    if (hitCounter) currentHitCount++;
                }
            }
        } else {
            if (hitCounter) currentHitCount = 0;
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

            if (holeESP) {
                for (BlockPos pos : holeBlocks) {
                    Box box = new Box(pos).expand(0.002);
                    drawFullBox(matrices, lines, box, 0.0f, 1.0f, 0.5f, 1.0f);
                }
            }

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

            if (trajectory) {
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null && player.isUsingItem() && player.getActiveItem().getItem() instanceof BowItem) {
                    Vec3d start = player.getEyePos();
                    float pitch = player.getPitch();
                    float yaw = player.getYaw();
                    float vel = 3.0f;

                    Vec3d dir = new Vec3d(
                        -MathHelper.sin(yaw * (float)Math.PI / 180.0f) * MathHelper.cos(pitch * (float)Math.PI / 180.0f),
                        -MathHelper.sin(pitch * (float)Math.PI / 180.0f),
                        MathHelper.cos(yaw * (float)Math.PI / 180.0f) * MathHelper.cos(pitch * (float)Math.PI / 180.0f)
                    );

                    Vec3d pos = start;
                    Vec3d velVec = dir.multiply(vel);

                    for (int i = 0; i < 100; i++) {
                        Vec3d nextPos = pos.add(velVec);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)pos.x, (float)pos.y, (float)pos.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0, 1, 0);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)nextPos.x, (float)nextPos.y, (float)nextPos.z).color(1.0f, 0.0f, 0.0f, 1.0f).normal(0, 1, 0);
                        pos = nextPos;
                        velVec = velVec.multiply(0.99).add(0, -0.05, 0);

                        if (MinecraftClient.getInstance().world.getBlockState(BlockPos.ofFloored(pos)).isSolid()) break;
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

        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);

        lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y2, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);

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

    public static class DamageNumber {
        public Vec3d pos;
        public float damage;
        public boolean isCrit;
        public long spawnTime;

        public DamageNumber(Vec3d pos, float damage, boolean isCrit) {
            this.pos = pos;
            this.damage = damage;
            this.isCrit = isCrit;
            this.spawnTime = System.currentTimeMillis();
        }
    }
}
