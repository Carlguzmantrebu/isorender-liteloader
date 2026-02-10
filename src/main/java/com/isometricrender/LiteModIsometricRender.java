package com.isometricrender;

import com.isometricrender.gui.RenderScreen;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.Tickable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

public class LiteModIsometricRender implements LiteMod, OutboundChatFilter, Tickable {
    public static final Logger LOGGER = LogManager.getLogger("IsometricRender");
    private boolean tabPressed = false;
    
    @Override
    public String getName() {
        return "IsometricRender";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public void init(File configPath) {
        LOGGER.info("Isometric Render v1.0.0 initialized");
        LOGGER.info("Use /isorender area <coords> to render areas");
        LOGGER.info("Press TAB twice to autocomplete coordinates");
    }
    
    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
    }
    
    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {
        if (minecraft.currentScreen instanceof GuiChat) {
            boolean tabDown = Keyboard.isKeyDown(Keyboard.KEY_TAB);
            
            if (tabDown && !tabPressed) {
                tabPressed = true;
                TabCompleter.handleTabComplete((GuiChat) minecraft.currentScreen);
            } else if (!tabDown) {
                tabPressed = false;
            }
        } else {
            tabPressed = false;
        }
    }
    
    @Override
    public boolean onSendChatMessage(String message) {
        if (!message.startsWith("/isorender") && !message.startsWith("/iso")) {
            return true; // Let it pass
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        
        try {
            String[] parts = message.substring(1).split(" ");
            
            if (parts[0].equals("iso")) {
                mc.player.sendMessage(new TextComponentString("§7Did you mean §e/isorender§7?"));
                return false;
            }
            
            if (parts.length == 1) {
                mc.player.sendMessage(new TextComponentString("§e=== Isometric Render ==="));
                mc.player.sendMessage(new TextComponentString("§a/isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
                mc.player.sendMessage(new TextComponentString("§7Press TAB twice to autocomplete"));
                return false;
            }
            
            if ("area".equals(parts[1])) {
                if (parts.length != 8) {
                    mc.player.sendMessage(new TextComponentString("§cUsage: /isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
                    return false;
                }
                
                int x1 = Integer.parseInt(parts[2]);
                int y1 = Integer.parseInt(parts[3]);
                int z1 = Integer.parseInt(parts[4]);
                int x2 = Integer.parseInt(parts[5]);
                int y2 = Integer.parseInt(parts[6]);
                int z2 = Integer.parseInt(parts[7]);
                
                BlockPos pos1 = new BlockPos(x1, y1, z1);
                BlockPos pos2 = new BlockPos(x2, y2, z2);
                
                AreaSelection selection = new AreaSelection(pos1, pos2);
                
                int blocks = selection.getSizeX() * selection.getSizeY() * selection.getSizeZ();
                mc.player.sendMessage(new TextComponentString("§aOpening renderer... (" + blocks + " blocks)"));
                
                LOGGER.info("Opening GUI with selection: " + pos1 + " to " + pos2);
                
                mc.addScheduledTask(() -> {
                    try {
                        LOGGER.info("Displaying RenderScreen...");
                        mc.displayGuiScreen(new RenderScreen(selection));
                        LOGGER.info("RenderScreen displayed successfully");
                    } catch (Exception e) {
                        LOGGER.error("Failed to open RenderScreen", e);
                        mc.player.sendMessage(new TextComponentString("§cFailed to open GUI: " + e.getMessage()));
                    }
                });
            } else {
                mc.player.sendMessage(new TextComponentString("§cUnknown subcommand: " + parts[1]));
            }
            
        } catch (NumberFormatException e) {
            mc.player.sendMessage(new TextComponentString("§cInvalid coordinates"));
        } catch (Exception e) {
            mc.player.sendMessage(new TextComponentString("§cError: " + e.getMessage()));
            LOGGER.error("Command error", e);
        }
        
        return false; // Don't send to server
    }
}
