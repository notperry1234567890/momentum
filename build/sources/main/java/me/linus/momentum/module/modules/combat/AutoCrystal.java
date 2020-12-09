package me.linus.momentum.module.modules.combat;

import me.linus.momentum.event.events.packet.PacketReceiveEvent;
import me.linus.momentum.event.events.packet.PacketSendEvent;
import me.linus.momentum.event.events.render.Render3DEvent;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.checkbox.SubCheckbox;
import me.linus.momentum.setting.mode.SubMode;
import me.linus.momentum.setting.slider.SubSlider;
import me.linus.momentum.util.client.MathUtil;
import me.linus.momentum.util.client.Timer;
import me.linus.momentum.util.client.friend.FriendManager;
import me.linus.momentum.util.combat.CrystalUtil;
import me.linus.momentum.util.combat.EnemyUtil;
import me.linus.momentum.util.render.GeometryMasks;
import me.linus.momentum.util.render.RenderUtil;
import me.linus.momentum.util.world.EntityUtil;
import me.linus.momentum.util.world.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linustouchtips
 * @since 11/24/2020
 */

public class AutoCrystal extends Module {
    public AutoCrystal() {
        super("AutoCrystal", Category.COMBAT, "Automatically places and explodes crystals");
    }

    Timer breakTimer = new Timer();
    public static EntityPlayer lastTarget;
    public static EntityPlayer currentTarget;
    private long placeSystemTime;
    private boolean switchCooldown = false;
    private BlockPos render;
    private Entity renderEnt;

    // break settings
    public static Checkbox explode = new Checkbox("Break", true);
    public static SubSlider breakRange = new SubSlider(explode, "Break Range", 0.0D, 5.0D, 7.0D, 0);
    public static SubSlider breakDelay = new SubSlider(explode, "Break Delay", 0.0D, 20.0D, 60.0D, 0);
    public static SubSlider breakAttempts = new SubSlider(explode, "Break Attempts", 0.0D, 1.0D, 5.0D, 0);
    public static SubCheckbox antiDeSync = new SubCheckbox(explode, "Anti-DeSync", true);
    public static SubCheckbox syncBreak = new SubCheckbox(explode, "Sync Break", true);
    public static SubCheckbox unload = new SubCheckbox(explode, "Unload Crystal", true);
    public static SubCheckbox rotate = new SubCheckbox(explode, "Rotate", false);
    public static SubCheckbox packetBreak = new SubCheckbox(explode, "Packet Break", true);
    public static SubCheckbox spoofBreak = new SubCheckbox(explode, "Spoof Break", false);
    public static SubMode breakHand = new SubMode(explode, "BreakHand", "MainHand", "OffHand", "Both", "GhostHand");

    // pause settings
    public static Checkbox pause = new Checkbox("Pause", true);
    public static SubSlider pauseHealth = new SubSlider(pause, "Pause Health", 0.0D, 7.0D, 36.0D, 0);
    public static SubMode pauseMode = new SubMode(pause, "Mode", "Place", "Break", "Both");

    // place settings
    public static Checkbox place = new Checkbox("Place", true);
    public static SubSlider placeRange = new SubSlider(place, "Place Range", 0.0D, 5.0D, 7.0D, 0);
    public static SubSlider enemyRange = new SubSlider(place, "Enemy Range", 0.0D, 5.0D, 7.0D, 0);
    public static SubSlider placeDelay = new SubSlider(place, "Place Delay", 0.0D, 40.0D, 600.0D, 0);
    public static SubSlider minDamage = new SubSlider(place, "Minimum Damage", 0.0D, 7.0D, 36.0D, 0);
    public static SubCheckbox autoSwitch = new SubCheckbox(place, "Auto-Switch", false);
    public static SubCheckbox multiPlace = new SubCheckbox(place, "MultiPlace", false);

    // faceplace settings
    public static Checkbox facePlace = new Checkbox("Face-Place", true);
    public static SubSlider facePlaceHealth = new SubSlider(facePlace, "Face-Place Health", 0.0D, 16.0D, 36.0D, 0);
    public static SubCheckbox armorMelt = new SubCheckbox(facePlace, "Armor Melt", false);
    public static SubSlider armorDurability = new SubSlider(facePlace, "Armor Durability", 0.0D, 15.0D, 100.0D, 0);

    // calculation settings
    public static Checkbox calculations = new Checkbox("Calculations", true);
    public static SubMode placeCalc = new SubMode(calculations, "Place Calculation", "Ideal", "Actual");
    public static SubMode damageCalc = new SubMode(calculations, "Damage Calculation", "Full", "Semi");

    // logic settings
    public static Checkbox logic = new Checkbox("Logic", true);
    public static SubMode logicMode = new SubMode(logic, "Crystal Logic", "Break -> Place", "Place -> Break");
    public static SubMode blockCalc = new SubMode(logic, "Block Logic", "Normal", "1.13+");

    // hole settings
    public static Checkbox hole = new Checkbox("Hole", false);
    public static SubCheckbox multiPlaceInHole = new SubCheckbox(hole, "MultiPlace in Hole", false);

    // prediction settings
    public static Checkbox prediction = new Checkbox("Prediction", false);
    public static SubSlider predictionRange = new SubSlider(prediction, "Range", 0.0D, 5.0D, 10.0D, 0);

    // render settings
    public static Checkbox renderCrystal = new Checkbox("Render", true);
    public static SubSlider r = new SubSlider(renderCrystal, "Red", 0.0D, 250.0D, 255.0D, 0);
    public static SubSlider g = new SubSlider(renderCrystal, "Green", 0.0D, 0.0D, 255.0D, 0);
    public static SubSlider b = new SubSlider(renderCrystal, "Blue", 0.0D, 250.0D, 255.0D, 0);
    public static SubSlider a = new SubSlider(renderCrystal, "Alpha", 0.0D, 30.0D, 255.0D, 0);
    public static SubCheckbox renderDamage = new SubCheckbox(renderCrystal, "Render Damage", true);
    public static SubCheckbox outline = new SubCheckbox(renderCrystal, "Outline", false);

    @Override
    public void setup() {
        addSetting(explode);
        addSetting(place);
        addSetting(facePlace);
        addSetting(pause);
        addSetting(calculations);
        addSetting(hole);
        addSetting(prediction);
        addSetting(renderCrystal);
        addSetting(logic);

        placeSystemTime = -1L;
        switchCooldown = false;
    }

    @Override
    public void onUpdate() {
        currentTarget = EntityUtil.getClosestPlayer(enemyRange.getValue());

        if (logicMode.getValue() == 1) {
            if (prediction.getValue()) {
                placePredictionCrystal();
            } else {
                placeDamageCrystal();
            }

            breakCrystal();
        } else {
            breakCrystal();

            if (prediction.getValue()) {
                placePredictionCrystal();
            } else {
                placeDamageCrystal();
            }
        }
    }

    public void breakCrystal() {
        final EntityEnderCrystal crystal = (EntityEnderCrystal) mc.world.loadedEntityList.stream().filter(entity -> entity instanceof EntityEnderCrystal).map(entity -> entity).min(Comparator.comparing(c -> mc.player.getDistance(c))).orElse(null);
        if (crystal != null && mc.player.getDistance(crystal) <= breakRange.getValue() && breakTimer.passed((long) breakDelay.getValue())) {
            if (pause.getValue() && PlayerUtil.getHealth() <= pauseHealth.getValue() && pauseMode.getValue() == 0) {
                return;
            }

            if (rotate.getValue()) {
                CrystalUtil.lookAtPacket(crystal.posX, crystal.posY, crystal.posZ, mc.player);
            }

            if (explode.getValue()) {
                for (int i = 0; i < breakAttempts.getValue(); i++) {
                    CrystalUtil.attackCrystal(crystal, packetBreak.getValue());
                }
            }

            if (syncBreak.getValue()) {
                CrystalUtil.getSwingArm(breakHand.getValue());
                crystal.setDead();
                if (unload.getValue()) {
                    mc.world.removeAllEntities();
                    mc.world.getLoadedEntityList();
                }
            }
        }

        breakTimer.reset();

        if (multiPlaceInHole.getValue() && PlayerUtil.isInHole())
            multiPlace.toggleValue();

        if (!multiPlace.getValue()) {
            return;
        }
    }

    public void placeDamageCrystal() {
        int crystalSlot = (mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL) ? mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.inventory.getStackInSlot(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }

        boolean offhand = false;
        if (mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            offhand = true;
        } else if (crystalSlot == -1) {
            return;
        }

        Entity entity = null;
        Entity lastTarget = null;
        BlockPos finalPos = null;
        final List<BlockPos> blocks = findCrystalBlocks();
        final List<Entity> entities = new ArrayList<>();
        entities.addAll(mc.world.playerEntities.stream().collect(Collectors.toList()));
        double damage = 0.5;
        for (final Entity entityTarget : entities) {
            if (entityTarget != mc.player) {
                if (((EntityLivingBase) entityTarget).getHealth() <= 0.0f && !(damageCalc.getValue() == 1))
                    continue;

                if (mc.player.getDistanceSq(entityTarget) > enemyRange.getValue() * enemyRange.getValue())
                    continue;

                if (FriendManager.isFriend(entityTarget.getName()) && FriendManager.isFriendModuleEnabled())
                    continue;


                for (final BlockPos blockPos : blocks) {
                    if (!CrystalUtil.canBlockBeSeen(blockPos) && mc.player.getDistanceSq(blockPos) > placeRange.getValue() * placeRange.getValue())
                        continue;

                    final double targetDistanceSq = entityTarget.getDistanceSq(blockPos);
                    if (targetDistanceSq > 56.2)
                        continue;

                    final double calcDamage;
                    if (placeCalc.getValue() == 1) {
                        calcDamage = CrystalUtil.calculateDamage(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ(), entityTarget);
                    } else {
                        calcDamage = CrystalUtil.calculateDamage(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5, entityTarget);
                    }

                    int minDamagePlace;
                    if (EnemyUtil.getArmor((EntityPlayer) entityTarget, armorMelt.getValue(), armorDurability.getValue())) {
                        minDamagePlace = 2;
                    } else {
                        minDamagePlace = (int) minDamage.getValue();
                    } if (calcDamage < minDamagePlace && ((EntityLivingBase) entityTarget).getHealth() + ((EntityLivingBase) entityTarget).getAbsorptionAmount() > facePlaceHealth.getValue())
                        continue;
                      if (calcDamage <= damage && !(damageCalc.getValue() == 1))
                        continue;

                    final double selfDamage = CrystalUtil.calculateDamage(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5, (Entity) mc.player);
                    if (PlayerUtil.getHealth() - selfDamage <= pauseHealth.getValue() && pause.getValue())
                        continue;
                    if (selfDamage  > calcDamage)
                        continue;

                    damage = calcDamage;
                    finalPos = blockPos;
                    entity = entityTarget;
                    lastTarget = entityTarget;
                }
            }
        }

        if (damage == 0.5) {
            this.render = null;
            this.renderEnt = null;
            CrystalUtil.resetRotation();
            return;
        }

        this.render = finalPos;
        this.renderEnt = entity;

        if (true) {
            if (!offhand && mc.player.inventory.currentItem != crystalSlot) {
                if (autoSwitch.getValue()) {
                    mc.player.inventory.currentItem = crystalSlot;
                }

                CrystalUtil.resetRotation();
                this.switchCooldown = true;
                return;
            }
        }

        if (place.getValue()) {
            CrystalUtil.lookAtPacket(finalPos.getX() + 0.5, finalPos.getY() - 0.5, finalPos.getZ() + 0.5, mc.player);
            final RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ), new Vec3d(finalPos.getX() + 0.5, finalPos.getY() - 0.5, finalPos.getZ() + 0.5));

            EnumFacing f;
            if (result == null || result.sideHit == null) {
                f = EnumFacing.UP;
            } else {
                f = result.sideHit;
            } if (switchCooldown) {
                switchCooldown = false;
                return;
            }

            if (System.nanoTime() / 1000000L - this.placeSystemTime >= placeDelay.getValue()) {
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(finalPos, f, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
                placeSystemTime = System.nanoTime() / 1000000L;
            }
        }
    }

    /**
     * this function places where the player is/is going to, litteraly so dogshit idk why anybody would use this
     */

    public void placePredictionCrystal() {
        int crystalSlot = (mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL) ? mc.player.inventory.currentItem : -1;
        if (crystalSlot == -1) {
            for (int l = 0; l < 9; ++l) {
                if (mc.player.inventory.getStackInSlot(l).getItem() == Items.END_CRYSTAL) {
                    crystalSlot = l;
                    break;
                }
            }
        }

        boolean offhand = false;
        if (mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL) {
            offhand = true;
        } else if (crystalSlot == -1) {
            return;
        }

        Entity entity;
        Entity lastTarget = null;
        BlockPos finalPos;
        final List<Entity> entities = new ArrayList<>();
        entities.addAll(mc.world.playerEntities.stream().collect(Collectors.toList()));
        for (final Entity entityTarget : entities) {
            if (entityTarget != mc.player) {
                if (((EntityLivingBase) entityTarget).getHealth() + ((EntityLivingBase) entityTarget).getAbsorptionAmount() <= 0.0f && !(damageCalc.getValue() == 1))
                    continue;

                if (mc.player.getDistanceSq(entityTarget) > enemyRange.getValue() * enemyRange.getValue())
                    continue;

                if (FriendManager.isFriend(entityTarget.getName()) && FriendManager.isFriendModuleEnabled())
                    continue;

                BlockPos blockPos = findCrystalBlocks().stream().filter(blockPos1 -> CrystalUtil.calculateDamage(blockPos1.x, blockPos1.y, blockPos1.z, mc.player) > pauseHealth.getValue()).filter(blockPos1 -> mc.player.getDistanceSq(blockPos1) > placeRange.getValue()).min(Comparator.comparing(blockPos1 -> entityTarget.getDistanceSq(blockPos1))).orElse(null);

                finalPos = blockPos;
                entity = entityTarget;
                lastTarget = entityTarget;

                this.render = finalPos;
                this.renderEnt = entity;

                if (true) {
                    if (!offhand && mc.player.inventory.currentItem != crystalSlot) {
                        if (autoSwitch.getValue()) {
                            mc.player.inventory.currentItem = crystalSlot;
                        }

                        CrystalUtil.resetRotation();
                        this.switchCooldown = true;
                        return;
                    }
                }

                if (place.getValue()) {
                    CrystalUtil.lookAtPacket(finalPos.getX() + 0.5, finalPos.getY() - 0.5, finalPos.getZ() + 0.5, mc.player);
                    final RayTraceResult result = mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + mc.player.getEyeHeight(), mc.player.posZ), new Vec3d(finalPos.getX() + 0.5, finalPos.getY() - 0.5, finalPos.getZ() + 0.5));

                    EnumFacing f;
                    if (result == null || result.sideHit == null) {
                        f = EnumFacing.UP;
                    } else {
                        f = result.sideHit;
                    } if (switchCooldown) {
                        switchCooldown = false;
                        return;
                    }

                    if (System.nanoTime() / 1000000L - this.placeSystemTime >= placeDelay.getValue()) {
                        mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(finalPos, f, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
                        placeSystemTime = System.nanoTime() / 1000000L;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRender3D(RenderWorldLastEvent eventRender) {
        if (renderCrystal.getValue() && render != null) {
            RenderUtil.drawVanillaBoxFromBlockPos(render, (float) r.getValue() / 255f, (float) g.getValue() / 255f, (float) b.getValue() / 255f, (float) a.getValue() / 255f);

            double damage = CrystalUtil.calculateDamage(render.getX() + .5, render.getY() + 1, render.getZ() + .5, renderEnt);
            double damageRounded = MathUtil.roundAvoid(damage, 1);

            if (outline.getValue())
                RenderUtil.drawBoundingBoxBlockPos(render, 1.0f, new Color((int) r.getValue(), (int) g.getValue(),  (int)b.getValue(), 144));

            if (renderDamage.getValue())
                RenderUtil.drawNametagFromBlockPos(render, String.valueOf(damageRounded));
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof SPacketSoundEffect && antiDeSync.getValue()) {
            final SPacketSoundEffect packet = (SPacketSoundEffect) event.getPacket();
            if (packet.getCategory() == SoundCategory.BLOCKS && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
                for (Entity e : mc.world.loadedEntityList) {
                    if (e instanceof EntityEnderCrystal) {
                        if (e.getDistance(packet.getX(), packet.getY(), packet.getZ()) <= 6.0f) {
                            e.setDead();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof CPacketPlayerDigging) {
            final CPacketPlayerDigging packet = (CPacketPlayerDigging) event.getPacket();
            if (packet.getAction().equals(CPacketUseEntity.Action.ATTACK) && spoofBreak.getValue()) {
                for (Entity e : mc.world.loadedEntityList) {
                    if (e instanceof EntityEnderCrystal) {
                        if (e.getDistance(packet.getPosition().x, packet.getPosition().y, packet.getPosition().z) <= 6.0f) {
                            e.setDead();
                        }
                    }
                }
            }
        }
    }

    /**
     * TODO: move all these calculations to  for some reason moving them causes crashes, so they stay here for now
     */

    private boolean canPlaceCrystal(final BlockPos blockPos) {
        final BlockPos boost = blockPos.add(0, 1, 0);
        final BlockPos boost2 = blockPos.add(0, 2, 0);
        if (blockCalc.getValue() == 1) return (mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
        else return (mc.world.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || mc.world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN) && mc.world.getBlockState(boost).getBlock() == Blocks.AIR && mc.world.getBlockState(boost2).getBlock() == Blocks.AIR && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
    }


    private List<BlockPos> findCrystalBlocks() {
        NonNullList positions = NonNullList.create();
        positions.addAll(this.getSphere(CrystalUtil.getPlayerPos(), (float) placeRange.getValue(), (int) placeRange.getValue(), false, true, 0).stream().filter(this::canPlaceCrystal).collect(Collectors.toList()));
        return (List<BlockPos>) positions;
    }

    public List<BlockPos> getSphere(final BlockPos loc, final float r, final int h, final boolean hollow, final boolean sphere, final int plus_y) {
        final List<BlockPos> circleblocks = new ArrayList<>();
        final int cx = loc.getX();
        final int cy = loc.getY();
        final int cz = loc.getZ();
        for (int x = cx - (int) r; x <= cx + r; ++x) {
            for (int z = cz - (int) r; z <= cz + r; ++z) {
                for (int y = sphere ? (cy - (int) r) : cy; y < (sphere ? (cy + r) : ((float) (cy + h))); ++y) {
                    final double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? ((cy - y) * (cy - y)) : 0);
                    if (dist < r * r && (!hollow || dist >= (r - 1.0f) * (r - 1.0f))) {
                        final BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                }
            }
        }

        return circleblocks;
    }

    @Override
    public String getHUDData() {
        if (currentTarget != null)
            return currentTarget.getName();
        else
            return "None";
    }
}