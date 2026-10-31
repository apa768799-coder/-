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
    // Существующие модули
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

    public boolean holeESP = false;
    public boolean autoTrap = false;

    // Настройки умного AutoMine
    public boolean smartOreSelection = true;   // true = копать всё ценное
    public int     minOrePriority    = 0;      // 0 = все, 30 = без угля, 90 = только алмаз
    public int     minHpToMine       = 6;      // пауза при HP ниже

    // Настройки X-Ray (для миксина)
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

    // Для ESP (рамки)
    private List<BlockPos> xrayBlocks = new ArrayList<>();
    private List<BlockPos> holeBlocks = new ArrayList<>();
    private int xrayTimer = 0;
    private int holeTimer = 0;

    // Таймеры и кулдауны
    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int afkTicks = 0;
    private int escapeTicks = 0;
    private int actionDelay = 0;
    private int targetItemTimer = 0;
    private int targetTimer = 0;
    
    // Умный AutoMine: Состояние и жилы
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();
    private final Map<BlockPos, Long> veinBlocks = new HashMap<>();
    private BlockPos currentBreakTarget = null;
    private int totalStuckTicks = 0;
    private int stuckEscalationLevel = 0;
    private BlockPos lastTargetBlock = null;

    private Random random = new Random();
    private String lastLoggedState = "";

    public List<DamageNumber> damageNumbersList = new ArrayList<>();

    // Приоритеты руд
    private static final Map<Block, Integer> ORE_PRIORITY = new HashMap<>();
    static {
        ORE_PRIORITY.put(Blocks.DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_DIAMOND_ORE, 100);
        ORE_PRIORITY.put(Blocks.EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_EMERALD_ORE, 95);
        ORE_PRIORITY.put(Blocks.ANCIENT_DEBRIS, 90);
        ORE_PRIORITY.put(Blocks.GOLD_ORE, 75);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_GOLD_ORE, 75);
        ORE_PRIORITY.put(Blocks.NETHER_GOLD_ORE, 70);
        ORE_PRIORITY.put(Blocks.IRON_ORE, 60);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_IRON_ORE, 60);
        ORE_PRIORITY.put(Blocks.LAPIS_ORE, 55);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_LAPIS_ORE, 55);
        ORE_PRIORITY.put(Blocks.REDSTONE_ORE, 50);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_REDSTONE_ORE, 50);
        ORE_PRIORITY.put(Blocks.NETHER_QUARTZ_ORE, 45);
        ORE_PRIORITY.put(Blocks.COPPER_ORE, 40);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COPPER_ORE, 40);
        ORE_PRIORITY.put(Blocks.COAL_ORE, 30);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COAL_ORE, 30);
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
        this.minOrePriority = other.minOrePriority;
        this.minHpToMine = other.minHpToMine;
    }

    // Логика для X-Ray миксина
    public boolean isXrayBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA) return true;
        if (b == Blocks.CHEST || b == Blocks.ENDER_CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.SPAWNER) return true;
        if (xrayDiamond && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (xrayIron && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (xrayGold && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE)) return true;
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
        this.currentBreakTarget = null;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) player.setYaw(player.getYaw() + 180.0f + (float)(Math.random() * 30 - 15)); 
    }

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (actionDelay > 0) { actionDelay--; return; }

        long currentTime = System.currentTimeMillis();
        blacklistedBlocks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 20000);
        damageNumbersList.removeIf(dn -> currentTime - dn.spawnTime > 1000);

        // Отрисовка ESP для X-ray (не влияет на прозрачность камня, просто рамки)
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

    // ═══════════════════════════════════════════════════════════
    //               УМНЫЙ AUTO MINE (МАШИНА СОСТОЯНИЙ)
    // ═══════════════════════════════════════════════════════════
    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (escapeTicks > 0) {
            escapeTicks--;
            updateState("Отступаю от привата...");
            client.options.forwardKey.setPressed(true); 
            client.options.jumpKey.setPressed(false);
            return;
        }

        if (player.getHealth() < minHpToMine) {
            updateState("Мало HP (" + (int) player.getHealth() + ") — пауза");
            client.options.forwardKey.setPressed(false);
            return;
        }

        if (player.getInventory().getEmptySlot() == -1) {
            autoMine = false;
            updateState("Инвентарь полон! Выключаю AutoMine");
            return;
        }

        // Авто-выбор кирки
        if (!(player.getMainHandStack().getItem() instanceof PickaxeItem)) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
            }
        }

        // Сбор дропа (если валяется рядом)
        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) {
            targetItem = findNearestItem(client, player);
            targetItemTimer = 0;
        }
        if (targetItem != null) {
            targetItemTimer++;
            double dist = player.getPos().distanceTo(targetItem.getPos());
            if (targetItemTimer > 40 || (dist < 1.5 && targetItemTimer > 15)) {
                targetItem = null;
            } else {
                updateState("Сбор лута");
                if (dist > 1.2) {
                    lookAtEntity(player, targetItem);
                    client.options.forwardKey.setPressed(true);
                } else {
                    client.options.forwardKey.setPressed(false);
                }
                return;
            }
        }

        // Чистим очередь жил
        long now = System.currentTimeMillis();
        veinBlocks.entrySet().removeIf(e ->
            client.world.getBlockState(e.getKey()).isAir() || now - e.getValue() > 60000L);

        // Если есть незаконченная жила — идём к ней
        if (!veinBlocks.isEmpty()) {
            BlockPos bestVein = null;
            double bestVDist = 30.0;
            for (BlockPos vb : veinBlocks.keySet()) {
                double d = player.getPos().distanceTo(Vec3d.ofCenter(vb));
                if (d < bestVDist) { bestVDist = d; bestVein = vb; }
            }
            if (bestVein != null) targetBlock = bestVein;
        }

        // Ищем цель
        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir() || blacklistedBlocks.containsKey(targetBlock)) {
            updateState(smartOreSelection ? "Умный скан руд..." : "Сканирую руду");
            targetBlock = smartOreSelection ? findBestOre(client, player) : findNearestOre(client, player);
            targetTimer = 0;

            // Сброс застревания при смене цели
            if (targetBlock != null && !targetBlock.equals(lastTargetBlock)) {
                resetStuckState();
                lastTargetBlock = targetBlock;
            }
        }

        // Если цель найдена — обрабатываем
        if (targetBlock != null) {
            targetTimer++;
            if (targetTimer > 600) { // 30 секунд на одну руду
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                resetStuckState();
                return;
            }

            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(targetCenter);

            if (dist <= 4.0) {
                // МЫ БЛИЗКО. Теперь думаем, как копать.
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                
                // 1. Проверяем прямую видимость
                Vec3d eye = player.getEyePos();
                BlockHitResult hit = client.world.raycast(new RaycastContext(
                        eye, targetCenter, RaycastContext.ShapeType.COLLIDER, 
                        RaycastContext.FluidHandling.NONE, player));

                if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetBlock)) {
                    // Видим руду напрямую (или она уже сломана)
                    if (client.world.getBlockState(targetBlock).isAir()) {
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                        if (smartOreSelection) addVeinBlocks(client, targetBlock);
                        veinBlocks.remove(targetBlock);
                        targetBlock = null;
                        resetStuckState();
                        actionDelay = 3 + random.nextInt(5);
                        return;
                    }

                    updateState("Добываю " + blockName(client.world.getBlockState(targetBlock).getBlock()));
                    lookAt(player, targetBlock);

                    if (!targetBlock.equals(currentBreakTarget)) {
                        currentBreakTarget = targetBlock;
                        totalStuckTicks = 0;
                    }
                    totalStuckTicks++;

                    breakBlock(client, player, targetBlock);

                    // Если долго не ломается — обкусываем соседей
                    if (totalStuckTicks > 40 && totalStuckTicks <= 100) {
                        if (stuckEscalationLevel < 1) {
                            stuckEscalationLevel = 1;
                            updateState("Обнажаю руду — копаю рядом");
                        }
                        if (totalStuckTicks % 5 == 0) {
                            for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                                BlockPos adj = targetBlock.offset(d);
                                Block adjBlock = client.world.getBlockState(adj).getBlock();
                                if (adjBlock == Blocks.AIR || adjBlock == Blocks.BEDROCK || adjBlock == Blocks.OBSIDIAN) continue;
                                if (adjBlock.getHardness() >= 0 && adjBlock.getHardness() < 4.0f) {
                                    lookAt(player, adj);
                                    breakBlock(client, player, adj);
                                    return;
                                }
                            }
                        }
                        return;
                    }

                    // >10 сек: не берётся
                    if (totalStuckTicks > 200) {
                        updateState("Руда не берётся — ищу другую");
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                        veinBlocks.remove(targetBlock);
                        targetBlock = null;
                        resetStuckState();
                    }
                    return;
                    
                } else {
                    // На пути есть блок! (Потолок, стена). Копаем его.
                    BlockPos blockingBlock = hit.getBlockPos();
                    updateState("Пробиваю путь к руде");
                    lookAt(player, blockingBlock);
                    breakBlock(client, player, blockingBlock);
                    return;
                }
            }

            // ───── Навигация (если руда далеко) ─────
            int dy = targetBlock.getY() - player.getBlockPos().getY();
            
            // Ступенька ВВЕРХ
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
            
            // Ступенька ВНИЗ
            if (dy <= -2) {
                updateState("Ступенька ВНИЗ");
                BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
                lookAt(player, front);
                breakBlock(client, player, front);
                if (client.world.getBlockState(front).isAir()) client.options.forwardKey.setPressed(true);
                return;
            }

            // Идём напрямую
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
                BlockPos breakTarget = !client.world.getBlockState(front).isAir() ? front : frontUp;
                lookAt(player, breakTarget);
                breakBlock(client, player, breakTarget);
                return;
            }

            client.options.forwardKey.setPressed(true);
            if (player.horizontalCollision && player.isOnGround()) client.options.jumpKey.setPressed(true);
            else client.options.jumpKey.setPressed(false);
            return;
        }

        // ───── Туннель (если руд нет) ─────
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

    // ───── Хелперы ─────

    private void resetStuckState() {
        currentBreakTarget = null;
        totalStuckTicks = 0;
        stuckEscalationLevel = 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.jumpKey.setPressed(false);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
        }
    }

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
        if (orePriority(origin) < 45) return; // Медь и уголь не считаем жилой

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos n = center.add(dx, dy, dz);
                    if (client.world.getBlockState(n).getBlock() == origin && !blacklistedBlocks.containsKey(n)) {
                        veinBlocks.putIfAbsent(n, System.currentTimeMillis());
                    }
                }
            }
        }
    }

    private String blockName(Block b) {
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) return "алмаз";
        if (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE) return "изумруд";
        if (b == Blocks.ANCIENT_DEBRIS) return "обломки";
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE) return "золото";
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return "железо";
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return "лазурит";
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return "редстоун";
        if (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE) return "медь";
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return "уголь";
        return "блок";
    }

    private void breakBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        Block b = client.world.getBlockState(pos).getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.BARRIER) {
            blacklistedBlocks.put(pos, System.currentTimeMillis());
            return;
        }

        // Вычисляем правильную грань для удара
        Direction face = getBreakFace(player, pos);
        client.interactionManager.updateBlockBreakingProgress(pos, face);
        player.swingHand(Hand.MAIN_HAND);
    }

    private Direction getBreakFace(ClientPlayerEntity player, BlockPos pos) {
        Vec3d eye = player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x, dy = eye.y - center.y, dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        // Старый метод для поиска конкретной руды по индексу (если smartOreSelection = false)
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

    // Остальные методы (updateXrayBlocks, updateHoleBlocks, doScaffold, doAutoTrap, 
    // findNearestItem, doKillAura, doAutoTotem, doAutoEat, doAutoArmor, doChestStealer, 
    // findNearestChest, doAutoFish, renderESP, drawFullBox, lookAt, lookAtEntity, getYawToEntity)
    // остаются БЕЗ ИЗМЕНЕНИЙ из твоего предыдущего файла.

    // ... (вставь их сюда, они у тебя уже есть, я не буду занимать 300 строк повторением) ...
