package net.smartminer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Modules {
    public boolean autoMine = false;
    public boolean autoFish = false;
    public boolean killAura = false;
    public boolean fullBright = false;
    public boolean antiAfk = false;
    public boolean autoArmor = false;
    public boolean scaffold = false;
    public boolean velocity = false;
    public boolean chestStealer = false;

    public int targetOreIndex = 0;
    public String[] oreNames = {"Алмаз", "Железо", "Золото", "Уголь", "Изумруд"};

    public String currentState = "Ожидание";
    public BlockPos targetBlock = null;
    public BlockPos targetChest = null;
    private ItemEntity targetItem = null;

    private int fishTimer = 0;
    private int attackCooldown = 0;
    private int afkTicks = 0;
    private int escapeTicks = 0;
    
    private BlockPos currentlyBreaking = null;
    private int breakStuckTicks = 0;
    private final Map<BlockPos, Long> blacklistedBlocks = new HashMap<>();

    private Random random = new Random();
    private String lastLoggedState = "";

    private void updateState(String newState) {
        this.currentState = newState;
        if (!newState.equals(lastLoggedState)) {
            SmartClient.addLog(newState);
            lastLoggedState = newState;
        }
    }

    public void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (fishTimer > 0) fishTimer--;
        if (attackCooldown > 0) attackCooldown--;

        long currentTime = System.currentTimeMillis();
        // Черный список живет 15 секунд
        blacklistedBlocks.entrySet().removeIf(entry -> currentTime - entry.getValue() > 15000);

        if (fullBright) client.options.getGamma().setValue(10.0);
        else client.options.getGamma().setValue(1.0);

        if (velocity && player.hurtTime > 0) {
            Vec3d vel = player.getVelocity();
            player.setVelocity(vel.x * 0.2, vel.y, vel.z * 0.2);
        }

        if (player.isOnGround() && player.input.movementForward > 0) player.setSprinting(true);

        if (antiAfk) {
            afkTicks++;
            if (afkTicks > 100) {
                player.setYaw(player.getYaw() + (float)(Math.random() * 4 - 2));
                afkTicks = 0;
            }
        }

        if (autoArmor) doAutoArmor(client, player);
        if (chestStealer) doChestStealer(client, player);
        if (scaffold) doScaffold(client, player);
        if (autoMine) doAutoMine(client, player);
        if (autoFish) doAutoFish(client, player);
        if (killAura) doKillAura(client, player);
    }

    private void doAutoMine(MinecraftClient client, ClientPlayerEntity player) {
        if (escapeTicks > 0) {
            escapeTicks--;
            client.options.backKey.setPressed(true);
            if (escapeTicks == 0) client.options.backKey.setPressed(false);
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

        if (targetBlock == null || client.world.getBlockState(targetBlock).isAir() || blacklistedBlocks.containsKey(targetBlock)) {
            updateState("Поиск руды");
            targetBlock = findNearestOre(client, player);
            if (targetBlock != null) {
                SmartClient.addLog("Найдена руда: " + oreNames[targetOreIndex] + " на " + targetBlock.toShortString());
            }
        }

        if (targetBlock != null) {
            Vec3d eyePos = player.getEyePos();
            Vec3d targetCenter = Vec3d.ofCenter(targetBlock);
            double dist = player.getPos().distanceTo(targetCenter);

            if (dist <= 4.5) {
                updateState("Добываю " + oreNames[targetOreIndex]);
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                lookAt(player, targetBlock);
                
                if (client.world.getBlockState(targetBlock).isAir()) {
                    SmartClient.addLog("Успешно добыл: " + oreNames[targetOreIndex]);
                    blacklistedBlocks.put(targetBlock, System.currentTimeMillis());
                    targetBlock = null;
                } else {
                    breakBlock(client, player, targetBlock);
                }
                return;
            }

            BlockHitResult hit = client.world.raycast(new RaycastContext(eyePos, targetCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
            BlockPos hitBlock = hit.getBlockPos();

            if (!hitBlock.equals(targetBlock) && !client.world.getBlockState(hitBlock).isAir()) {
                updateState("Копаю стену к руде");
                client.options.forwardKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                lookAt(player, hitBlock); 
                breakBlock(client, player, hitBlock);
                return;
            }

            updateState("Иду к руде");
            lookAt(player, targetBlock);
            client.options.forwardKey.setPressed(true);

            if (player.horizontalCollision) {
                BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
                if (!client.world.getBlockState(front).isAir() && client.world.getBlockState(front.up()).isAir()) {
                    if (player.isOnGround()) client.options.jumpKey.setPressed(true);
                } else {
                    client.options.jumpKey.setPressed(false);
                }
            } else {
                client.options.jumpKey.setPressed(false);
            }
            return;
        }

        if (targetItem == null || targetItem.isRemoved() || !targetItem.isAlive()) {
            targetItem = findNearestItem(client, player);
            if (targetItem != null) {
                SmartClient.addLog("Найден дроп: " + targetItem.getStack().getName().getString());
            }
        }
        if (targetItem != null) {
            updateState("Сбор лута");
            double dist = player.getPos().distanceTo(targetItem.getPos());
            if (dist > 1.2) {
                lookAtEntity(player, targetItem);
                client.options.forwardKey.setPressed(true);
                client.options.jumpKey.setPressed(false);
            } else {
                client.options.forwardKey.setPressed(false);
            }
            return;
        }

        updateState("Копаю туннель");
        lookAt(player, player.getBlockPos().offset(player.getHorizontalFacing(), 2));
        client.options.forwardKey.setPressed(true);

        BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
        BlockPos frontUp = front.up();
        
        if (client.world.getBlockState(front.down()).getBlock() == Blocks.LAVA) {
            player.setYaw(player.getYaw() + 90.0f); 
            SmartClient.addLog("Впереди лава! Меняю направление.");
            return;
        }

        if (!client.world.getBlockState(front).isAir() && !blacklistedBlocks.containsKey(front)) {
            breakBlock(client, player, front);
            return;
        }
        
        if (!client.world.getBlockState(frontUp).isAir() && !blacklistedBlocks.containsKey(frontUp)) {
            breakBlock(client, player, frontUp);
            return;
        }

        if (player.horizontalCollision && player.isOnGround()) {
            client.options.jumpKey.setPressed(true);
        } else {
            client.options.jumpKey.setPressed(false);
        }
    }

    // ГЛАВНЫЙ ФИКС: Копаем каждый тик без остановок!
    private void breakBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        if (blacklistedBlocks.containsKey(pos)) return;

        if (pos.equals(currentlyBreaking)) {
            breakStuckTicks++;
            if (breakStuckTicks > 160) { // 8 секунд
                updateState("Приват/Препятствие! Ищу другое место");
                SmartClient.addLog("Заблокирован блок на " + pos.toShortString());
                blacklistedBlocks.put(pos, System.currentTimeMillis()); 
                if (targetBlock != null) blacklistedBlocks.put(targetBlock, System.currentTimeMillis()); 
                targetBlock = null;
                currentlyBreaking = null;
                breakStuckTicks = 0;
                escapeTicks = 40; 
                player.setYaw(player.getYaw() + 180.0f); 
                return;
            }
        } else {
            currentlyBreaking = pos;
            breakStuckTicks = 0;
        }

        // Вызываем копание КАЖДЫЙ ТИК, чтобы прогресс не сбрасывался!
        client.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        
        // Машем рукой для визуала
        if (player.age % 4 == 0) {
            player.swingHand(Hand.MAIN_HAND);
        }
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
            for (int y = -15; y <= 15; y++) {
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
        return client.world.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(3.0), 
            e -> e.isAlive() && !e.getStack().isEmpty())
            .stream()
            .min(Comparator.comparingDouble(e -> player.distanceTo(e)))
            .orElse(null);
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

    private void doScaffold(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isOnGround() || player.isTouchingWater() || player.getVelocity().y > 0.1) return;
        BlockPos below = player.getBlockPos().down();
        if (client.world.getBlockState(below).isAir()) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof BlockItem) {
                    if (player.getInventory().selectedSlot != i) player.getInventory().selectedSlot = i;
                    BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(below).add(0, 0.5, 0), Direction.UP, below, false);
                    client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                    player.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }

    private void doKillAura(MinecraftClient client, ClientPlayerEntity player) {
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return;
        if (attackCooldown > 0) return;

        LivingEntity target = client.world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(3.8), 
            e -> e != player && e.isAlive())
            .stream()
            .min(Comparator.comparingDouble(LivingEntity::getHealth))
            .orElse(null);

        if (target != null) {
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

    private float getYawToEntity(ClientPlayerEntity player, net.minecraft.entity.Entity entity) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
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

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x; double dy = target.y - eye.y; double dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float dYaw = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float dPitch = targetPitch - player.getPitch();
        
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 8.0f)); 
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 8.0f));
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
        
        player.setYaw(player.getYaw() + Math.signum(dYaw) * Math.min(Math.abs(dYaw), 8.0f)); 
        player.setPitch(player.getPitch() + Math.signum(dPitch) * Math.min(Math.abs(dPitch), 8.0f));
    }
}
