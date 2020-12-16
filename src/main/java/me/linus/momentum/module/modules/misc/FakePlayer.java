package me.linus.momentum.module.modules.misc;

import com.mojang.authlib.GameProfile;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import me.linus.momentum.setting.mode.Mode;
import net.minecraft.client.entity.EntityOtherPlayerMP;

import java.util.UUID;

/**
 * @author linustouchtips
 * @since 11/26/2020
 */

public class FakePlayer extends Module {
    public FakePlayer() {
        super("FakePlayer", Category.MISC, "Creates a fake motionless player");
    }

    private static final Mode name = new Mode("Name", "linustouchtips24", "popbob", "Fit", "GrandOlive", "S8N", "Papa_Quill");
    public static Checkbox inventory = new Checkbox("Copy Inventory", true);

    @Override
    public void setup() {
        addSetting(name);
        addSetting(inventory);
    }

    public void onEnable() {
        if (nullCheck())
            return;

        String fakeName = "None";
        switch (name.getValue()) {
            case 0:
                fakeName = "linustouchtips24";
                break;
            case 1:
                fakeName = "popbob";
                break;
            case 2:
                fakeName = "Fit";
                break;
            case 3:
                fakeName = "GrandOlive";
                break;
            case 4:
                fakeName = "S8N";
                break;
            case 5:
                fakeName = "Papa_Quill";
                break;
        }

        EntityOtherPlayerMP fakePlayer = new EntityOtherPlayerMP(mc.world, new GameProfile(UUID.fromString("873e2766-9254-49bc-89d7-5d4d585ad29d"), fakeName));
        fakePlayer.copyLocationAndAnglesFrom(mc.player);
        mc.world.addEntityToWorld(69420, fakePlayer);

        if (inventory.getValue())
            fakePlayer.inventory.copyInventory(mc.player.inventory);
    }

    @Override
    public void onDisable() {
        mc.world.removeEntityFromWorld(69420);
    }
}