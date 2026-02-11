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
        System.out.println("[IsometricRender] onSendChatMessage called with: " + message);
        
        if (!message.startsWith("/isorender") && !message.startsWith("/iso")) {
            System.out.println("[IsometricRender] Not our command, letting it pass");
            return true;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        System.out.println("[IsometricRender] Processing command, mc=" + mc + ", mc.player=" + mc.player);
        
        try {
            String[] parts = message.substring(1).split(" ");
            System.out.println("[IsometricRender] Command parts: " + java.util.Arrays.toString(parts));
            
            if (parts[0].equals("iso")) {
                System.out.println("[IsometricRender] Detected 'iso' shortcut, suggesting full command");
                mc.player.sendMessage(new TextComponentString("§7Did you mean §e/isorender§7?"));
                return false;
            }
            
            if (parts.length == 1) {
                System.out.println("[IsometricRender] No subcommand, showing help");
                mc.player.sendMessage(new TextComponentString("§e=== Isometric Render ==="));
                mc.player.sendMessage(new TextComponentString("§a/isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
                mc.player.sendMessage(new TextComponentString("§7Press TAB twice to autocomplete"));
                return false;
            }
            
            if ("area".equals(parts[1])) {
                if (parts.length != 8) {
                    System.out.println("[IsometricRender] Wrong number of arguments: " + parts.length);
                    mc.player.sendMessage(new TextComponentString("§cUsage: /isorender area <x1> <y1> <z1> <x2> <y2> <z2>"));
                    return false;
                }
                
                System.out.println("[IsometricRender] Parsing coordinates...");
                int x1 = Integer.parseInt(parts[2]);
                int y1 = Integer.parseInt(parts[3]);
                int z1 = Integer.parseInt(parts[4]);
                int x2 = Integer.parseInt(parts[5]);
                int y2 = Integer.parseInt(parts[6]);
                int z2 = Integer.parseInt(parts[7]);
                
                System.out.println("[IsometricRender] Coordinates: (" + x1 + "," + y1 + "," + z1 + ") to (" + x2 + "," + y2 + "," + z2 + ")");
                
                BlockPos pos1 = new BlockPos(x1, y1, z1);
                BlockPos pos2 = new BlockPos(x2, y2, z2);
                
                AreaSelection selection = new AreaSelection(pos1, pos2);
                
                int blocks = selection.getSizeX() * selection.getSizeY() * selection.getSizeZ();
                System.out.println("[IsometricRender] Selection created: " + blocks + " blocks");
                mc.player.sendMessage(new TextComponentString("§aOpening renderer... (" + blocks + " blocks)"));
                
                LOGGER.info("Opening GUI with selection: " + pos1 + " to " + pos2);
                
                System.out.println("[IsometricRender] About to schedule GUI opening task");
                System.out.println("[IsometricRender] Current thread: " + Thread.currentThread().getName());
                System.out.println("[IsometricRender] mc.currentScreen BEFORE: " + mc.currentScreen);
                
                mc.addScheduledTask(() -> {
                    try {
                        System.out.println("[IsometricRender] === SCHEDULED TASK START ===");
                        System.out.println("[IsometricRender] Running on thread: " + Thread.currentThread().getName());
                        System.out.println("[IsometricRender] mc.currentScreen in scheduled task BEFORE: " + mc.currentScreen);
                        
                        LOGGER.info("Displaying RenderScreen...");
                        System.out.println("[IsometricRender] Creating RenderScreen instance...");
                        
                        RenderScreen screen = new RenderScreen(selection);
                        System.out.println("[IsometricRender] RenderScreen created: " + screen);
                        
                        System.out.println("[IsometricRender] Calling mc.displayGuiScreen(screen)...");
                        mc.displayGuiScreen(screen);
                        
                        System.out.println("[IsometricRender] mc.displayGuiScreen() returned");
                        System.out.println("[IsometricRender] mc.currentScreen AFTER: " + mc.currentScreen);
                        System.out.println("[IsometricRender] Expected screen: " + screen);
                        System.out.println("[IsometricRender] Screen match: " + (mc.currentScreen == screen));
                        
                        LOGGER.info("RenderScreen displayed successfully");
                        System.out.println("[IsometricRender] === SCHEDULED TASK END ===");
                    } catch (Exception e) {
                        System.out.println("[IsometricRender] EXCEPTION in scheduled task: " + e.getMessage());
                        LOGGER.error("Failed to open RenderScreen", e);
                        mc.player.sendMessage(new TextComponentString("§cFailed to open GUI: " + e.getMessage()));
                        e.printStackTrace();
                    }
                });
                
                System.out.println("[IsometricRender] Scheduled task added");
            } else {
                System.out.println("[IsometricRender] Unknown subcommand: " + parts[1]);
                mc.player.sendMessage(new TextComponentString("§cUnknown subcommand: " + parts[1]));
            }
            
        } catch (NumberFormatException e) {
            System.out.println("[IsometricRender] NumberFormatException: " + e.getMessage());
            mc.player.sendMessage(new TextComponentString("§cInvalid coordinates"));
        } catch (Exception e) {
            System.out.println("[IsometricRender] Exception in onSendChatMessage: " + e.getMessage());
            e.printStackTrace();
            mc.player.sendMessage(new TextComponentString("§cError: " + e.getMessage()));
            LOGGER.error("Command error", e);
        }
        
        System.out.println("[IsometricRender] Returning false (don't send to server)");
        return false;
    }
}
