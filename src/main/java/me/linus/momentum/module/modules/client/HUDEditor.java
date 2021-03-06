package me.linus.momentum.module.modules.client;

import me.linus.momentum.gui.main.hud.HUD;
import me.linus.momentum.module.Module;
import me.linus.momentum.setting.checkbox.Checkbox;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * @author bon & linustouchtips
 * @since 12/01/202
 */

public class HUDEditor extends Module {
	public HUDEditor() {
		super("HUD", Category.CLIENT, "The in-game hud editor");
		this.getKeybind().setKeyCode(Keyboard.KEY_PERIOD);
	}

	public static Checkbox allowOverflow = new Checkbox("Allow Overflow", false);
	public static Checkbox colorSync = new Checkbox("Color Sync", true);
	public static Checkbox pauseGame = new Checkbox("Pause Game", false);

	public void setup() {
		addSetting(allowOverflow);
		addSetting(colorSync);
		addSetting(pauseGame);
	}
	
	public static HUD hudEditor = new HUD();

	@Override
	public void onEnable() {
		if (nullCheck())
			return;

		super.onEnable();
		mc.displayGuiScreen(hudEditor);
	}
	
	@SubscribeEvent
	public void onRender(RenderGameOverlayEvent.Text event) {
		// boost = 0;
	}
}
	

