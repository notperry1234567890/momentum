package me.linus.momentum.util.player.rotation;

import me.linus.momentum.event.events.player.RotationEvent;
import me.linus.momentum.mixin.MixinInterface;
import me.linus.momentum.util.client.MathUtil;
import me.linus.momentum.util.world.EntityUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * @author linustouchtips
 * @since 12/29/2020
 */

public class RotationUtil implements MixinInterface {

    /**
     * proper rotation spoofing system
     */

    public static Rotation rotationStep(Rotation currentRotation, Rotation targetRotation, float rotationStep) {
        float yawDifference = ((targetRotation.yaw - currentRotation.yaw) % 360.0f + 540.0f) % 360.0f - 180.0f;
        float pitchDifference = ((targetRotation.pitch - currentRotation.pitch) % 360.0f + 540.0f) % 360.0f - 180.0f;
        return new Rotation(currentRotation.yaw + (yawDifference > rotationStep ? rotationStep : Math.max(yawDifference, -rotationStep)), currentRotation.pitch + (pitchDifference > rotationStep ? rotationStep : Math.max(pitchDifference, -rotationStep)), Rotation.RotationMode.Packet, targetRotation.rotationPriority);
    }

    // override vanilla packet sending here, we replace them with our own custom values
    public static void updateRotationPackets(RotationEvent event) {

        if (mc.player.isSprinting() != mc.player.serverSprintState) {
            if (mc.player.isSprinting()) 
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SPRINTING));
            else 
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SPRINTING));

            mc.player.serverSprintState = mc.player.isSprinting();
        }
        
        if (mc.player.isSneaking() != mc.player.serverSneakState) {
            if (mc.player.isSneaking())
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
            else
                mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
            
            mc.player.serverSneakState = mc.player.isSneaking();
        }
        
        double updatedPosX = mc.player.posX - mc.player.lastReportedPosX;
        double updatedPosY = mc.player.getEntityBoundingBox().minY - mc.player.lastReportedPosY;
        double updatedPosZ = mc.player.posZ - mc.player.lastReportedPosZ;
        
        double updatedRotationYaw = event.getYaw() - mc.player.lastReportedYaw;
        double updatedRotationPitch = event.getPitch() - mc.player.lastReportedPitch;
        
        mc.player.positionUpdateTicks++;
        
        boolean positionUpdate = updatedPosX * updatedPosX + updatedPosY * updatedPosY + updatedPosZ * updatedPosZ > 9.0E-4D || mc.player.positionUpdateTicks >= 20;
        boolean rotationUpdate = updatedRotationYaw != 0.0D || updatedRotationPitch != 0.0D;
        
        if (mc.player.isRiding()) {
            mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.motionX, -999.0D, mc.player.motionZ, event.getYaw(), event.getPitch(), mc.player.onGround));
            positionUpdate = false;
        }
        
        else if (positionUpdate && rotationUpdate) 
            mc.player.connection.sendPacket(new CPacketPlayer.PositionRotation(mc.player.posX, mc.player.getEntityBoundingBox().minY, mc.player.posZ, event.getYaw(), event.getPitch(), mc.player.onGround));
        else if (positionUpdate)
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.getEntityBoundingBox().minY, mc.player.posZ, mc.player.onGround));
        else if (rotationUpdate) 
            mc.player.connection.sendPacket(new CPacketPlayer.Rotation(event.getYaw(), event.getPitch(), mc.player.onGround));
        else if (mc.player.prevOnGround != mc.player.onGround) 
            mc.player.connection.sendPacket(new CPacketPlayer(mc.player.onGround));

        if (positionUpdate) {
            mc.player.lastReportedPosX = mc.player.posX;
            mc.player.lastReportedPosY = mc.player.getEntityBoundingBox().minY;
            mc.player.lastReportedPosZ = mc.player.posZ;
            mc.player.positionUpdateTicks = 0;
        }

        if (rotationUpdate) {
            mc.player.lastReportedYaw = event.getYaw();
            mc.player.lastReportedPitch = event.getPitch();
        }

        mc.player.prevOnGround = mc.player.onGround;
        mc.player.autoJumpEnabled = mc.player.mc.gameSettings.autoJump;
    }

    public static float[] getAngles(Entity entity) {
       return MathUtil.calcAngle(EntityUtil.interpolateEntityTime(mc.player, mc.getRenderPartialTicks()), EntityUtil.interpolateEntityTime(entity, mc.getRenderPartialTicks()));
    }

    public static float[] getPositionAngles(BlockPos blockPos) {
        return MathUtil.calcAngle(EntityUtil.interpolateEntityTime(mc.player, mc.getRenderPartialTicks()), new Vec3d(blockPos));
    }

    public static float[] getTileAngles(TileEntity tileEntity) {
        return MathUtil.calcAngle(new Vec3d(tileEntity.getPos().x, tileEntity.getPos().y, tileEntity.getPos().z), new Vec3d(tileEntity.getPos().x, tileEntity.getPos().y, tileEntity.getPos().z));
    }

    public static void resetYaw(double rotation) {
        mc.player.rotationYaw = MathUtil.clamp(mc.player.rotationYaw, (float) -rotation, (float) rotation);
    }

    public static void resetPitch(double rotation) {
        mc.player.rotationPitch = MathUtil.clamp(mc.player.rotationPitch, (float) -rotation, (float) rotation);
    }

    public static String getFacing() {
        switch (MathHelper.floor((double) (mc.player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7) {
            case 0:
                return "South";
            case 1:
                return "South West";
            case 2:
                return "West";
            case 3:
                return "North West";
            case 4:
                return "North";
            case 5:
                return "North East";
            case 6:
                return "East";
            case 7:
                return "South East";
        }

        return "";
    }

    public static String getTowards() {
        switch (MathHelper.floor((double) (mc.player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7) {
            case 0:
                return "+Z";
            case 1:
                return "-X +Z";
            case 2:
                return "-X";
            case 3:
                return "-X -Z";
            case 4:
                return "-Z";
            case 5:
                return "+X -Z";
            case 6:
                return "+X";
            case 7:
                return "+X +Z";
        }

        return "";
    }
}
