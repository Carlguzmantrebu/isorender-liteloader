package com.isometricrender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.lang.reflect.Field;

public class TabCompleter {
    private static Field inputFieldField = null;
    
    static {
        try {
            for (Field f : GuiChat.class.getDeclaredFields()) {
                if (f.getType() == GuiTextField.class) {
                    inputFieldField = f;
                    inputFieldField.setAccessible(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static GuiTextField getInputField(GuiChat chatGui) {
        try {
            if (inputFieldField != null) {
                return (GuiTextField) inputFieldField.get(chatGui);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean handleTabComplete(GuiChat chatGui) {
        GuiTextField inputField = getInputField(chatGui);
        if (inputField == null) return false;
        
        String text = inputField.getText();
        
        // Match /is, /iso, /isor, /isore, etc -> /isorender
        if (text.matches("^/is[^\\s]*$")) {
            inputField.setText("/isorender ");
            return true;
        }
        
        // Match /isorender followed by nothing or partial 'area' -> /isorender area
        if (text.matches("^/isorender\\s*[a]?[r]?[e]?[a]?$")) {
            inputField.setText("/isorender area ");
            return true;
        }
        
        // /isorender area -> autocomplete with 3 coords
        if (text.startsWith("/isorender area")) {
            Minecraft mc = Minecraft.getMinecraft();
            RayTraceResult trace = mc.objectMouseOver;
            if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos lookingAt = trace.getBlockPos();
                
                String[] parts = text.split(" ");
                int numParts = parts.length - 2;
                
                if (numParts >= 6) {
                    return false;
                }
                
                String newText = text;
                if (!text.endsWith(" ")) {
                    newText += " ";
                }
                newText += lookingAt.getX() + " " + lookingAt.getY() + " " + lookingAt.getZ();
                
                inputField.setText(newText);
                return true;
            }
        }
        
        return false;
    }
}
