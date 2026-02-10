package com.isometricrender.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

public class GuiSlider extends GuiButton {
    private final double minValue;
    private final double maxValue;
    private double value;
    private boolean dragging = false;
    private final String prefix;
    private final String suffix;
    private final SliderCallback callback;
    
    public interface SliderCallback {
        void onValueChanged(GuiSlider slider);
    }
    
    public GuiSlider(int id, int x, int y, int width, int height, 
                     String prefix, String suffix,
                     double minValue, double maxValue, double initialValue,
                     boolean visible, boolean enabled, SliderCallback callback) {
        super(id, x, y, width, height, "");
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = Math.max(minValue, Math.min(maxValue, initialValue));
        this.visible = visible;
        this.enabled = enabled;
        this.callback = callback;
        updateDisplayString();
    }
    
    public double getValue() {
        return value;
    }
    
    private void updateDisplayString() {
        this.displayString = value == (int) value 
            ? String.format("%s%d%s", prefix, (int) value, suffix)
            : String.format("%s%.1f%s", prefix, value, suffix);
    }
    
    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }
    
    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;
        
        if (this.dragging) {
            float percentage = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
            percentage = Math.max(0.0F, Math.min(1.0F, percentage));
            
            value = minValue + (maxValue - minValue) * percentage;
            updateDisplayString();
            
            if (callback != null) {
                callback.onValueChanged(this);
            }
        }
        
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
        float sliderPos = (float)((value - minValue) / (maxValue - minValue));
        int sliderX = this.x + (int)(sliderPos * (this.width - 8));
        this.drawTexturedModalRect(sliderX, this.y, 0, 66, 4, 20);
        this.drawTexturedModalRect(sliderX + 4, this.y, 196, 66, 4, 20);
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            float percentage = (float)(mouseX - (this.x + 4)) / (float)(this.width - 8);
            percentage = Math.max(0.0F, Math.min(1.0F, percentage));
            
            value = minValue + (maxValue - minValue) * percentage;
            updateDisplayString();
            
            if (callback != null) {
                callback.onValueChanged(this);
            }
            
            this.dragging = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
}
