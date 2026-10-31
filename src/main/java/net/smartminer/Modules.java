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
    // ==========================================
    // 1. ГЛОБАЛЬНЫЕ НАСТРОЙКИ И ФЛАГИ МОДУЛЕЙ
    // ==========================================
    
    // Основные боевые модули
    public boolean killAura = false;
    public boolean autoTrap = false;
    public boolean criticals = false;
    public boolean velocity = false;
    
    // Движение и автоматизация
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean scaffold = false;
    public boolean sprint = false;
    public boolean noFall = false;
    public boolean antiAfk = false;
    
    // Инвентарь
    public boolean autoArmor = false;
    public boolean autoTotem = false;
    public boolean chestStealer = false;
    public boolean autoRespawn = false;
    
    // Визуал и ESP
    public boolean fullBright = false;
    public boolean xray = false;
    public boolean playerESP = false;
    public boolean itemESP = false;
    public boolean chestESP = false;
    public boolean glowESP = false;
    public boolean tracers = false;
    public boolean trajectory = false;
    public boolean holeESP = false;
    public boolean damageNumbers = false;
    public boolean hitCounter = false;
    public boolean lowHp = false;
    public boolean hitParticles = false;

    // --- Настройки умного AutoMine ---
    public boolean smartOreSelection = true;   // Умный выбор руды по приоритету
    public int minOrePriority = 30;            // Минимальный приоритет (30 = без угля)
    public int minHpToMine = 8;                // Останавливаться, если HP ниже
    public boolean autoDropJunk = true;        // Выбрасывать мусор при полном инвентаре
    public boolean mineOnlyExposed = false;    // Копать только открытые руды

    // --- Настройки X-Ray ---
    public boolean xrayDiamond = true;
    public boolean xrayIron = true;
    public boolean xrayGold = true;
    public boolean xrayEmerald = true;
    public boolean xrayRedstone = false;
    public boolean xrayLapis = false;
    public boolean xrayCoal = false;

    // --- Настройки KillAura ---
    public float killAuraRange = 3.5f;
    public boolean killAuraPlayersOnly = false;
    public boolean killAuraAutoSwitch = true;
    public int killAuraCps = 12;

    // ==========================================
    // 2. ВНУТРЕННИЕ ПЕРЕМЕННЫЕ И СОСТОЯНИЯ
    // ==========================================

    public String currentState = "Ожидание";
    public int currentHitCount = 0;
    public BlockPos targetBlock = null;
    public LivingEntity killAuraTarget = null;
    public List<DamageNumber> damageNumbersList = new ArrayList<>();

    private ItemEntity targetItem = null;
    private BlockPos targetChest = null;
    private Random random = new Random();
    private String lastLoggedState = "";

    // Таймеры
    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int afkTicks = 0;
    private int escapeTicks = 0;
    private int actionDelay = 0;
    private int targetItemTimer = 0;
    private int targetTimer = 0;
    private int xrayTimer = 0;
    private int holeTimer = 0;

    // Стейт-машина AutoMine
    public enum MineState { SCANNING, MOVING, MINING, ESCAPING, LOOTING }
    private MineState mineState = MineState.SCANNING;
    private BlockPos currentBreakTarget = null;
    private int totalStuckTicks = 0;
    private int stuckEscalationLevel = 0;
    private Vec3d stuckStartPos = null;

    // Очереди и блэклисты
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();
    private final Map<BlockPos, Long> veinBlocks = new HashMap<>();
    
    // Визуальные кэши
    private List<BlockPos> xrayBlocks = new ArrayList<>();
    private List<BlockPos> holeBlocks = new ArrayList<>();

    // Чат-детектор приватов
    private long lastChatWarningTime = 0;
    private int regionEscapeTicks = 0;

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
        ORE_PRIORITY.put(Blocks.COPPER_ORE,              40);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COPPER_ORE,    40);
        ORE_PRIORITY.put(Blocks.COAL_ORE,                30);
        ORE_PRIORITY.put(Blocks.DEEPSLATE_COAL_ORE,      30);
    }

    public Modules() {
        // Инициализация при создании
    }

    // ==========================================
    // 3. СИСТЕМНЫЕ МЕТОДЫ И ИНТЕГРАЦИЯ
    // ==========================================

    public void copyFrom(Modules other) {
        this.killAura = other.killAura;
        this.autoMine = other.autoMine;
        this.autoFish = other.autoFish;
        this.scaffold = other.scaffold;
        this.sprint = other.sprint;
        this.noFall = other.noFall;
        this.antiAfk = other.antiAfk;
        this.autoArmor = other.autoArmor;
        this.autoTotem = other.autoTotem;
        this.chestStealer = other.chestStealer;
        this.autoRespawn = other.autoRespawn;
        this.fullBright = other.fullBright;
        this.xray = other.xray;
        this.playerESP = other.playerESP;
        this.itemESP = other.itemESP;
        this.chestESP = other.chestESP;
        this.glowESP = other.glowESP;
        this.tracers = other.tracers;
        this.trajectory = other.trajectory;
        this.holeESP = other.holeESP;
        this.damageNumbers = other.damageNumbers;
        this.hitCounter = other.hitCounter;
        this.smartOreSelection = other.smartOreSelection;
        this.minOrePriority = other.minOrePriority;
        this.minHpToMine = other.minHpToMine;
        this.autoDropJunk = other.autoDropJunk;
        this.xrayDiamond = other.xrayDiamond;
        this.xrayIron = other.xrayIron;
        this.xrayGold = other.xrayGold;
        this.xrayEmerald = other.xrayEmerald;
        this.xrayRedstone = other.xrayRedstone;
        this.xrayLapis = other.xrayLapis;
        this.xrayCoal = other.xrayCoal;
        this.killAuraRange = other.killAuraRange;
        this.killAuraPlayersOnly = other.killAuraPlayersOnly;
    }

    public void onChatMessage(String text) {
        String lower = text.toLowerCase();
        
        // Детект приватов и защиты
        if (lower.contains("can't break") || lower.contains("cannot break") || 
            lower.contains("нельзя сломать") || lower.contains("защищен") || 
            lower.contains("приват") || lower.contains("region")) {
            
            lastChatWarningTime = System.currentTimeMillis();
            
            if (regionEscapeTicks <= 0) {
                regionEscapeTicks = 80; 
                updateState("Обнаружен приват! Побег...");
                
                if (targetBlock != null) {
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis() + 99999999L);
                    targetBlock = null;
                }
                if (currentBreakTarget != null) {
                    blacklistedBlocks.put(currentBreakTarget, System.currentTimeMillis() + 99999999L);
                }
            }
        }
    }

    public boolean isXrayBlock(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.AIR || b == Blocks.WATER || b == Blocks.LAVA) return true;
        if (b == Blocks.CHEST || b == Blocks.ENDER_CHEST || b == Blocks.TRAPPED_CHEST) return true;
        if (xrayDiamond && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (xrayIron && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (xrayGold && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE)) return true;
        if (xrayEmerald && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return true;
        if (xrayRedstone && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE)) return true;
        if (xrayLapis && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE)) return true;
        if (xrayCoal && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE)) return true;
        return false; 
    }

    private void updateState(String newState) {
        this.currentState = newState;
        if (!newState.equals(lastLoggedState)) {
            SmartClient.addLog(newState);
            lastLoggedState = newState;
        }
    }

    // ==========================================
    // 4. ГЛАВНЫЙ ЦИКЛ (TICK)
    // ==========================================

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        // Обработка таймеров
        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;
        if (actionDelay > 0) { actionDelay--; return; }

        // Очистка блэклистов
        long now = System.currentTimeMillis();
        blacklistedBlocks.entrySet().removeIf(entry -> now - entry.getValue() > 20000);
        damageNumbersList.removeIf(dn -> now - dn.spawnTime > 1000);

        // Обновление визуальных данных
        if (xray) {
            xrayTimer++;
            if (xrayTimer >= 20) { updateXrayBlocks(client, player); xrayTimer = 0; }
        }
        if (holeESP) {
            holeTimer++;
            if (holeTimer >= 20) { updateHoleBlocks(client, player); holeTimer = 0; }
        }
        if (glowESP) {
            for (Entity e : client.world.getEntities()) {
                if (e instanceof PlayerEntity && e != player) e.setGlowing(true);
                if (e instanceof MobEntity) e.setGlowing(true);
            }
        } else {
            for (Entity e : client.world.getEntities()) {
                if (e.isGlowing()) e.setGlowing(false);
            }
        }

        // Базовые модули
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

        // Авто-еда прерывает другие действия
        if (player.getHungerManager().getFoodLevel() < 15) {
            doAutoEat(client, player);
            return; 
        }

        if (antiAfk) doAntiAfk(client, player);
        
        // Специализированные модули
        if (autoTotem) doAutoTotem(client, player);
        if (autoArmor) doAutoArmor(client, player);
        if (chestStealer) doChestStealer(client, player);
        if (autoFish) doAutoFish(client, player);
        if (scaffold) doScaffold(client, player);
        
        // Приоритетные действия
        if (autoMine) doAutoMine(client, player);
        if (killAura) doKillAura(client, player);
        if (autoTrap && killAuraTarget != null) doAutoTrap(client, player);

        if (autoRespawn && client.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen) {
            player.requestRespawn();
            client.setScreen(null);
        }
    }

    // ==========================================
    // 5. AUTO MINE (МАШИНА СОСТОЯНИЙ)
    // ==========================================

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        // 1. Проверка на побег из привата
        if (regionEscapeTicks > 0) {
            regionEscapeTicks--;
            client.options.backKey.setPressed(true); 
            client.options.forwardKey.setPressed(false);
            if (regionEscapeTicks % 10 == 0) player.setYaw(player.getYaw() + (random.nextBoolean() ? 60.0f : -60.0f));
            if (regionEscapeTicks == 0) {
                client.options.backKey.setPressed(false);
                updateState("Побег завершен. Продолжаю поиск");
            }
            return;
        }

        // 2. Проверка на HP
        if (player.getHealth() < minHpToMine) {
            updateState("Критическое HP (" + (int)player.getHealth() + ") - пауза");
            client.options.forwardKey.setPressed(false);
            return;
        }

        // 3. Проверка инвентаря
        if (player.getInventory().getEmptySlot() == -1) {
            if (autoDropJunk) {
                dropJunk(client, player);
            } else {
                autoMine = false;
                updateState("Инвентарь полон! Выключаю AutoMine");
            }
            return;
        }

        // 4. Авто-выбор кирки
        if (autoSwitchTool(client, player)) return;

        // 5. Сбор лута (Приоритет 1)
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
                updateState("Сбор дропа");
                if (dist > 1.2) {
                    lookAtEntity(player, targetItem);
                    client.options.forwardKey.setPressed(true);
                } else {
                    client.options.forwardKey.setPressed(false);
                }
                return;
            }
        }

        // 6. Очистка и проверка очереди жил
        long now = System.currentTimeMillis();
        veinBlocks.entrySet().removeIf(e -> 
            client.world.getBlockState(e.getKey()).isAir() || 
            now - e.getValue() > 60000L
        );

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

        // 7. Поиск цели
        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir() || blacklistedBlocks.containsKey(targetBlock)) {
            updateState("Сканирование руд");
            targetBlock = smartOreSelection ? findBestOre(client, player) : findNearestOre(client, player);
            targetTimer = 0;
            if (targetBlock != null) resetStuckState();
        }

        // 8. Действия с целью
        if (targetBlock != null) {
            targetTimer++;
            if (targetTimer > 600) { // Защита от бесконечного цикла
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                resetStuckState();
                return;
            }

            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(targetCenter);

            // Если мы близко (в радиусе 4 блоков)
            if (dist <= 4.0) {
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);

                // Raycast проверка видимости
                Vec3d eye = player.getEyePos();
                BlockHitResult hit = client.world.raycast(new RaycastContext(
                        eye, targetCenter, RaycastContext.ShapeType.COLLIDER, 
                        RaycastContext.FluidHandling.NONE, player));

                if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(targetBlock)) {
                    lookAt(player, targetBlock);

                    // Руда сломана?
                    if (client.world.getBlockState(targetBlock).isAir()) {
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                        if (smartOreSelection) addVeinBlocks(client, targetBlock);
                        veinBlocks.remove(targetBlock);
                        targetBlock = null;
                        resetStuckState();
                        actionDelay = 3 + random.nextInt(5);
                        return;
                    }

                    // Защита от привата (по чату)
                    if (System.currentTimeMillis() - lastChatWarningTime < 1500) {
                        updateState("Здесь приват! Ухожу...");
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis() + 99999999L);
                        targetBlock = null;
                        regionEscapeTicks = 80;
                        return;
                    }

                    // Добыча
                    if (!targetBlock.equals(currentBreakTarget)) {
                        currentBreakTarget = targetBlock;
                        totalStuckTicks = 0;
                    }
                    totalStuckTicks++;
                    
                    updateState("Добываю " + blockName(client.world.getBlockState(targetBlock).getBlock()));
                    breakBlock(client, player, targetBlock);

                    // Умное обкусывание (если застряли 2-4 сек)
                    if (totalStuckTicks > 40 && totalStuckTicks <= 100) {
                        if (stuckEscalationLevel < 1) {
                            stuckEscalationLevel = 1;
                            updateState("Обнажаю руду — копаю рядом");
                        }
                        if (totalStuckTicks % 5 == 0) {
                            for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                                BlockPos adj = targetBlock.offset(d);
                                Block adjBlock = client.world.getBlockState(adj).getBlock();
                                if (adjBlock != Blocks.AIR && adjBlock != Blocks.BEDROCK && adjBlock != Blocks.OBSIDIAN) {
                                    if (adjBlock.getHardness() >= 0 && adjBlock.getHardness() < 4.0f) {
                                        lookAt(player, adj);
                                        breakBlock(client, player, adj);
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    // Слишком долго (10 сек)
                    if (totalStuckTicks > 200) {
                        updateState("Руда не поддается — ищу другую");
                        blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                        targetBlock = null;
                        resetStuckState();
                    }
                    return;
                } else {
                    // Мешает блок, копаем его
                    BlockPos blockingBlock = hit.getBlockPos();
                    updateState("Пробиваю путь");
                    lookAt(player, blockingBlock);
                    breakBlock(client, player, blockingBlock);
                    return;
                }
            }

            // Навигация, если руда далеко
            int dy = targetBlock.getY() - player.getBlockPos().getY();
            
            // Ступеньки
            if (dy >= 2) {
                updateState("Ступенька ВВЕРХ");
                BlockPos upFront = player.getBlockPos().offset(player.getHorizontalFacing()).up();
                lookAt(player, upFront);
                breakBlock(client, player, upFront);
                if (client.world.getBlockState(upFront).isAir() && player.horizontalCollision) client.options.jumpKey.setPressed(true);
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

            // Прямое движение
            updateState("Иду к руде");
            lookAt(player, targetBlock);

            BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
            BlockPos frontUp = front.up();

            // Проверка на лаву
            if (client.world.getBlockState(front.down()).getBlock() == Blocks.LAVA) {
                updateState("Лава! Отмена");
                blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                targetBlock = null;
                return;
            }

            // Копаем препятствие
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

        // 9. Туннель (если руды нет)
        updateState("Копаю разведочный туннель");
        BlockPos tunnelFront = player.getBlockPos().offset(player.getHorizontalFacing());
        if (client.world.getBlockState(tunnelFront.down()).getBlock() == Blocks.LAVA) {
            player.setYaw(player.getYaw() + 90.0f); 
            return;
        }
        if (!client.world.getBlockState(tunnelFront).isAir()) {
            lookAt(player, tunnelFront);
            breakBlock(client, player, tunnelFront);
        } else if (!client.world.getBlockState(tunnelFront.up()).isAir()) {
            lookAt(player, tunnelFront.up());
            breakBlock(client, player, tunnelFront.up());
        } else {
            client.options.forwardKey.setPressed(true);
        }
    }

    // ==========================================
    // 6. ХЕЛПЕРЫ AUTO MINE
    // ==========================================

    private void resetStuckState() {
        currentBreakTarget = null;
        totalStuckTicks = 0;
        stuckEscalationLevel = 0;
        stuckStartPos = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
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
        if (orePriority(origin) < 45) return;

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
        if (b == Blocks.ANCIENT_DEBRIS) return "древние обломки";
        if (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE) return "золото";
        if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) return "железо";
        if (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE) return "лазурит";
        if (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE) return "редстоун";
        if (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE) return "уголь";
        return "блок";
    }

    private void breakBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        Block b = client.world.getBlockState(pos).getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.BARRIER) {
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
        double dx = eye.x - center.x, dy = eye.y - center.y, dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private BlockPos findNearestOre(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        BlockPos nearest = null;
        double bestDist = 25.0;
        Block t1 = Blocks.DIAMOND_ORE, t2 = Blocks.DEEPSLATE_DIAMOND_ORE; // Упрощенно
        
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

    private void dropJunk(MinecraftClient client, ClientPlayerEntity player) {
        for (int i = 9; i < 36; i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (st.isEmpty()) continue;
            Item item = st.getItem();
            if (item == Items.COBBLESTONE || item == Items.DIRT || item == Items.GRAVEL || 
                item == Items.ANDESITE || item == Items.DIORITE || item == Items.GRANITE) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, i, 0, SlotActionType.THROW, player);
                return;
            }
        }
    }

    private boolean autoSwitchTool(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof PickaxeItem) return false;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // 7. COMBAT (KILLAURA, AUTOTRAP)
    // ==========================================

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (attackCooldown > 0) return;

        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(killAuraRange),
            e -> e != player && e.isAlive() && !(e instanceof ArmorStandEntity) && (!killAuraPlayersOnly || e instanceof PlayerEntity))
            .stream()
            .sorted(Comparator.comparing((LivingEntity e) -> !(e instanceof PlayerEntity))
                    .thenComparingDouble(LivingEntity::getHealth))
            .findFirst()
            .orElse(null);

        killAuraTarget = target;

        if (target != null) {
            if (player.distanceTo(target) > killAuraRange) return;

            Vec3d eye = player.getEyePos();
            Vec3d targetPos = target.getEyePos();
            BlockHitResult hit = client.world.raycast(new RaycastContext(eye, targetPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target.getBlockPos())) {
                if (killAuraAutoSwitch && !(player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem)) {
                    for (int i = 0; i < 9; i++) {
                        if (player.getInventory().getStack(i).getItem() instanceof SwordItem) {
                            player.getInventory().selectedSlot = i;
                            break;
                        }
                    }
                }
                lookAtEntity(player, target);
                
                // Проверка угла обзора, чтобы не бить "спиной"
                if (Math.abs(MathHelper.wrapDegrees(player.getYaw() - getYawToEntity(player, target))) < 30) {
                    client.interactionManager.attackEntity(player, target);
                    player.swingHand(Hand.MAIN_HAND);
                    attackCooldown = 20 - killAuraCps; // CPS -> тики
                    
                    if (damageNumbers) damageNumbersList.add(new DamageNumber(target.getPos(), 5.0f, false));
                    if (hitCounter) currentHitCount++;
                }
            }
        }
    }

    private void doAutoTrap(MinecraftClient client, ClientPlayerEntity player) {
        if (killAuraTarget == null || !killAuraTarget.isAlive()) return;
        if (player.getPos().distanceTo(killAuraTarget.getPos()) > 5.0) return;

        int obbySlot = -1;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) { obbySlot = i; break; }
        }
        if (obbySlot == -1) return;
        if (player.getInventory().selectedSlot != obbySlot) { player.getInventory().selectedSlot = obbySlot; return; }

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

    // ==========================================
    // 8. ДВИЖЕНИЕ И ИНВЕНТАРЬ
    // ==========================================

    private void doScaffold(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isOnGround() || player.isTouchingWater() || player.isInLava()) return;
        BlockPos below = player.getBlockPos().down();
        
        if (client.world.getBlockState(below).isAir() || client.world.getBlockState(below).getBlock() == Blocks.LAVA) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof BlockItem) {
                    if (player.getInventory().selectedSlot != i) player.getInventory().selectedSlot = i;
                    BlockPos neighbor = below.offset(player.getHorizontalFacing().getOpposite());
                    
                    if (!client.world.getBlockState(neighbor).isAir()) {
                        client.options.sneakKey.setPressed(true);
                        float prevYaw = player.getYaw();
                        float prevPitch = player.getPitch();
                        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(0, 0.5, 0);
                        
                        double dx = hitVec.x - player.getX();
                        double dy = hitVec.y - player.getEyeY();
                        double dz = hitVec.z - player.getZ();
                        double dist = Math.sqrt(dx*dx + dz*dz);
                        
                        player.setYaw((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0f));
                        player.setPitch((float)-Math.toDegrees(Math.atan2(dy, dist)));
                        
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

    private void doAutoEat(MinecraftClient client, ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.contains(DataComponentTypes.FOOD)) {
                updateState("Прием пищи...");
                if (player.getInventory().selectedSlot != i) player.getInventory().selectedSlot = i;
                client.options.useKey.setPressed(true);
                if (player.getHungerManager().getFoodLevel() >= 20) client.options.useKey.setPressed(false);
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

    private void doAutoTotem(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, i < 9 ? i + 36 : i, 40, SlotActionType.SWAP, player);
                return;
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
        if (targetChest == null || client.world.getBlockState(targetChest).isAir()) targetChest = findNearestChest(client, player);
        if (targetChest != null && player.getPos().distanceTo(Vec3d.ofCenter(targetChest)) < 4.0) {
            BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(targetChest), Direction.UP, targetChest, false);
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
            targetChest = null;
        }
    }

    private BlockPos findNearestChest(MinecraftClient client, ClientPlayerEntity player) {
        BlockPos pPos = player.getBlockPos();
        for (int x = -4; x <= 4; x++) for (int y = -2; y <= 2; y++) for (int z = -4; z <= 4; z++) {
            BlockPos pos = pPos.add(x, y, z);
            if (client.world.getBlockState(pos).getBlock() instanceof ChestBlock) return pos;
        }
        return null;
    }

    private void doAutoFish(MinecraftClient client, ClientPlayerEntity player) {
        FishingBobberEntity hook = null;
        for (Entity e : client.world.getEntities()) {
            if (e instanceof FishingBobberEntity && ((FishingBobberEntity) e).getPlayerOwner() == player) { hook = (FishingBobberEntity) e; break; }
        }
        if (hook == null && fishTimer <= 0) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND); player.swingHand(Hand.MAIN_HAND); fishTimer = 20;
        } else if (hook != null && hook.getVelocity().y < -0.08 && fishTimer <= 0) {
            client.interactionManager.interactItem(player, Hand.MAIN_HAND); player.swingHand(Hand.MAIN_HAND); fishTimer = 20 + random.nextInt(10);
        }
        if (fishTimer > 0) fishTimer--;
    }

    private void doAntiAfk(MinecraftClient client, ClientPlayerEntity player) {
        afkTicks++;
        if (afkTicks > 100) { player.setYaw(player.getYaw() + (float)(Math.random() * 4 - 2)); afkTicks = 0; }
    }

    private ItemEntity findNearestItem(MinecraftClient client, ClientPlayerEntity player) {
        return client.world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(5.0), e -> e.isAlive() && !e.getStack().isEmpty())
            .stream().min(Comparator.comparingDouble(e -> player.distanceTo(e))).orElse(null);
    }

    // ==========================================
    // 9. ВИЗУАЛИЗАЦИЯ (ESP, RENDER)
    // ==========================================

    private void updateXrayBlocks(MinecraftClient client, ClientPlayerEntity player) {
        xrayBlocks.clear();
        BlockPos pPos = player.getBlockPos();
        int radius = 15; 
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = pPos.add(x, y, z);
                    if (isXrayBlock(client.world.getBlockState(pos))) xrayBlocks.add(pos);
                }
            }
        }
    }

    private void updateHoleBlocks(MinecraftClient client, ClientPlayerEntity player) {
        holeBlocks.clear();
        BlockPos pPos = player.getBlockPos();
        int radius = 10;
        for (int x = -radius; x <= radius; x++) for (int y = -5; y <= 5; y++) for (int z = -radius; z <= radius; z++) {
            BlockPos pos = pPos.add(x, y, z);
            if (!client.world.getBlockState(pos).isAir() || !client.world.getBlockState(pos.up()).isAir() || !client.world.getBlockState(pos.up(2)).isAir()) continue;
            Block floor = client.world.getBlockState(pos.down()).getBlock();
            if (floor != Blocks.OBSIDIAN && floor != Blocks.BEDROCK) continue;
            // ... упрощенная проверка на hole ...
            holeBlocks.add(pos);
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
            Vec3d eye = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getEyePos() : camPos;

            if (xray) {
                for (BlockPos pos : xrayBlocks) {
                    Block b = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
                    float r = 1f, g = 1f, bl = 1f;
                    if (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE) { r = 0f; g = 1f; bl = 1f; }
                    else if (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE) { r = 0.6f; g = 0.6f; bl = 0.6f; }
                    
                    drawFullBox(matrices, lines, new Box(pos).expand(0.002), r, g, bl, 1f);
                    if (tracers) {
                        Vec3d tv = Vec3d.ofCenter(pos);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(r, g, bl, 1f).normal(0, 1, 0);
                        lines.vertex(matrices.peek().getPositionMatrix(), (float)tv.x, (float)tv.y, (float)tv.z).color(r, g, bl, 1f).normal(0, 1, 0);
                    }
                }
            }

            if (playerESP) {
                for (PlayerEntity p : MinecraftClient.getInstance().world.getPlayers()) {
                    if (p != MinecraftClient.getInstance().player) {
                        drawFullBox(matrices, lines, p.getBoundingBox().expand(0.1), 1f, 0f, 0f, 1f);
                        if (tracers) {
                            Vec3d tv = p.getPos();
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)eye.x, (float)eye.y, (float)eye.z).color(1f, 0f, 0f, 1f).normal(0, 1, 0);
                            lines.vertex(matrices.peek().getPositionMatrix(), (float)tv.x, (float)tv.y, (float)tv.z).color(1f, 0f, 0f, 1f).normal(0, 1, 0);
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
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z2).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x1, y2, z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, x2, y1, z1).color(r, g, b, a).normal(0, 1, 0); lines.vertex(matrix, x2, y2, z1).color(r, g, b, a).normal(0, 1, 0);
    }

    // ==========================================
    // 10. МАТЕМАТИКА И УТИЛИТЫ
    // ==========================================

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float dPitch = targetPitch - player.getPitch();
        float speed = 3.0f + random.nextFloat() * 1.5f;
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), speed));
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), speed));
    }

    private void lookAtEntity(ClientPlayerEntity player, Entity entity) {
        Vec3d target = entity.getPos();
        double dx = target.x - player.getX(), dy = target.y + entity.getHeight() / 2 - player.getEyeY(), dz = target.z - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float dPitch = targetPitch - player.getPitch();
        float speed = 3.0f + random.nextFloat() * 1.5f;
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), speed));
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), speed));
    }

    private float getYawToEntity(ClientPlayerEntity player, Entity entity) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
    }

    public static class DamageNumber {
        public Vec3d pos; public float damage; public boolean isCrit; public long spawnTime;
        public DamageNumber(Vec3d pos, float damage, boolean isCrit) {
            this.pos = pos; this.damage = damage; this.isCrit = isCrit; this.spawnTime = System.currentTimeMillis();
        }
    }
}
