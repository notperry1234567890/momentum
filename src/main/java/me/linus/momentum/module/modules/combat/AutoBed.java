package me.linus.momentum.module.modules.combat;

import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.checkbox.SubCheckbox;
import me.linus.momentum.setting.mode.SubMode;
import me.linus.momentum.setting.slider.SubSlider;
import me.linus.momentum.util.client.external.MessageUtil;
import me.linus.momentum.util.client.system.Timer;
import me.linus.momentum.util.client.friend.FriendManager;
import me.linus.momentum.util.combat.BedUtil;
import me.linus.momentum.util.combat.EnemyUtil;
import me.linus.momentum.util.combat.RotationUtil;
import me.linus.momentum.util.render.RenderUtil;
import me.linus.momentum.util.world.EntityUtil;
import me.linus.momentum.util.world.InventoryUtil;
import me.linus.momentum.util.world.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linustouchtips & bon
 * @since 12/20/2020
 * idk if it works well, prob not :P - linus
 */

public class AutoBed extends Module {
    public AutoBed() {
        super("AutoBed", Category.COMBAT, "Automatically places and explodes beds");
    }

    public static Checkbox explode = new Checkbox("Break", true);
    public static SubSlider breakDelay = new SubSlider(explode, "Break Delay", 0.0D, 20.0D, 60.0D, 0);
    public static SubSlider breakRange = new SubSlider(explode, "Break Range", 0.0D, 5.0D, 7.0D, 1);
    public static SubCheckbox unload = new SubCheckbox(explode, "Unload Bed", false);
    public static SubMode rotate = new SubMode(explode, "Rotate", "Spoof", "Legit", "None");

    public static Checkbox place = new Checkbox("Place", true);
    public static SubSlider placeDelay = new SubSlider(place, "Place Delay", 0.0D, 40.0D, 600.0D, 0);
    public static SubSlider placeRange = new SubSlider(place, "Place Range", 0.0D, 5.0D, 7.0D, 1);
    public static SubSlider enemyRange = new SubSlider(place, "Enemy Range", 0.0D, 5.0D, 15.0D, 1);
    public static SubCheckbox airPlace = new SubCheckbox(place, "Air Place", true);
    public static SubCheckbox autoSwitch = new SubCheckbox(place, "Auto-Switch", false);
    public static SubCheckbox spoofAngles = new SubCheckbox(place, "Spoof Angles", true);

    public static Checkbox pause = new Checkbox("Pause", true);
    public static SubSlider pauseHealth = new SubSlider(pause, "Pause Health", 0.0D, 7.0D, 36.0D, 0);
    public static SubCheckbox whenOverworld = new SubCheckbox(pause, "Nether Check", true);
    public static SubCheckbox whenMining = new SubCheckbox(pause, "When Mining", false);
    public static SubCheckbox whenEating = new SubCheckbox(pause, "When Eating", false);

    public static Checkbox logic = new Checkbox("Logic", true);
    public static SubMode logicMode = new SubMode(logic, "Logic", "Break -> Place", "Place -> Break");

    public static Checkbox renderBed = new Checkbox("Render", true);
    public static SubSlider r = new SubSlider(renderBed, "Red", 0.0D, 250.0D, 255.0D, 0);
    public static SubSlider g = new SubSlider(renderBed, "Green", 0.0D, 0.0D, 255.0D, 0);
    public static SubSlider b = new SubSlider(renderBed, "Blue", 0.0D, 250.0D, 255.0D, 0);
    public static SubSlider a = new SubSlider(renderBed, "Alpha", 0.0D, 50.0D, 255.0D, 0);

    @Override
    public void setup() {
        addSetting(explode);
        addSetting(place);
        addSetting(pause);
        addSetting(logic);
        addSetting(renderBed);
    }

    Timer breakTimer = new Timer();
    Timer placeTimer = new Timer();
    Entity currentTarget;
    BlockPos currentBlock;
    double diffXZ;
    float rotVar;
    boolean nowTop = false;

    @Override
    public void onUpdate() {
        if (nullCheck())
            return;

        if (mc.player.dimension == 0 & whenOverworld.getValue()) {
            MessageUtil.sendClientMessage("You are not in the nether!");
            return;
        }

        currentTarget = EntityUtil.getClosestPlayer(enemyRange.getValue());
        diffXZ = mc.player.getPositionVector().distanceTo(currentTarget.getPositionVector());

        switch (logicMode.getValue()) {
            case 0:
                breakBed();
                placeBed();
                break;
            case 1:
                placeBed();
                breakBed();
                break;
        }
    }

    public void breakBed() {
        TileEntityBed bed = (TileEntityBed) mc.world.loadedTileEntityList.stream().filter(e -> e instanceof TileEntityBed).filter(e -> mc.player.getDistance(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ()) <= breakRange.getValue()).sorted(Comparator.comparing(e -> mc.player.getDistance(e.getPos().getX(), e.getPos().getY(), e.getPos().getZ()))).findFirst().orElse(null);

        if (bed != null && breakTimer.passed((long) breakDelay.getValue())) {
            if (pause.getValue() && PlayerUtil.getHealth() <= pauseHealth.getValue())
                return;

            switch (rotate.getValue()) {
                case 0:
                    RotationUtil.lookAtPacket(bed.getPos().x, bed.getPos().y, bed.getPos().z, mc.player);
                    break;
                case 1:
                    RotationUtil.lookAtLegitTile(bed);
                    break;
            }

            if (explode.getValue())
                BedUtil.attackBed(bed.getPos());

            if (unload.getValue()) {
                mc.world.removeAllEntities();
                mc.world.getLoadedEntityList();
            }
        }

        breakTimer.reset();
    }

    public void placeBed() {
        if (diffXZ >= placeRange.getValue())
            return;

        if (autoSwitch.getValue())
            mc.player.inventory.currentItem = InventoryUtil.getHotbarItemSlot(Items.BED);

        Entity entity = null;
        List<Entity> entities = new ArrayList<>();
        entities.addAll(mc.world.playerEntities.stream().collect(Collectors.toList()));

        for (Entity entityTarget : entities) {
            if (entityTarget != mc.player) {
                if (EnemyUtil.getHealth((EntityPlayer) entityTarget) <= 0)
                    return;

                if (mc.player.getDistanceSq(entityTarget) > enemyRange.getValue() * enemyRange.getValue())
                    continue;

                if (FriendManager.isFriend(entityTarget.getName()) && FriendManager.isFriendModuleEnabled())
                    continue;

                entity = entityTarget;
            }
        }

        currentTarget = entity;
        currentBlock = BedUtil.getBedPosition((EntityPlayer) currentTarget, nowTop, rotVar);

        if (place.getValue() && placeTimer.passed((long) (800 + placeDelay.getValue())))
            BedUtil.placeBed(currentBlock, EnumFacing.DOWN, rotVar, nowTop, spoofAngles.getValue());
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent eventRender) {
        if (currentBlock != null && renderBed.getValue())
            RenderUtil.drawBoxBlockPos(currentBlock, new Color((int) r.getValue(), (int) g.getValue(),  (int) b.getValue(), (int) a.getValue()));
    }

    @Override
    public String getHUDData() {
        if (currentTarget != null)
            return " " + currentTarget.getName();
        else
            return " None";
    }
}