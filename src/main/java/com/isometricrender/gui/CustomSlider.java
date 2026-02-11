package com.isometricrender.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
public class CustomSlider extends GuiButton {
    private double sliderValue;
    private double minValue;
    private double maxValue;
    private boolean dragging = false;
    
    // Colores custom
    private static final int COLOR_ACTIVE = 0xFF4488FF;   // Azul
    private static final int COLOR_HANDLE = 0xFFFFFFFF;   // Blanco
    private static final int COLOR_BG = 0xFF444444;       // Gris oscuro
    private static final int COLOR_BORDER = 0xFF666666;   // Gris borde
    
    public CustomSlider(int buttonId, int x, int y, int width, int height, 
                        String prefix, String suffix, double minVal, double maxVal, 
                        double currentVal, boolean showDec, boolean drawStr) {
        super(buttonId, x, y, width, height, "");
        this.minValue = minVal;
        this.maxValue = maxVal;
        this.sliderValue = (currentVal - minVal) / (maxVal - minVal);
    }
    
    @Override
    protected int getHoverState(boolean mouseOver) {
        return 0;
    }
    
    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            if (this.dragging) {
                this.sliderValue = (double)(mouseX - (this.x + 4)) / (double)(this.width - 8);
                if (this.sliderValue < 0.0) this.sliderValue = 0.0;
                if (this.sliderValue > 1.0) this.sliderValue = 1.0;
            }
            
            // Fondo gris
            drawRect(this.x, this.y, this.x + this.width, this.y + this.height, COLOR_BG);
            // Borde
            drawRect(this.x, this.y, this.x + 1, this.y + this.height, COLOR_BORDER);
            drawRect(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, COLOR_BORDER);
            drawRect(this.x, this.y, this.x + this.width, this.y + 1, COLOR_BORDER);
            drawRect(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, COLOR_BORDER);
            // Parte azul activa
            int activeWidth = (int)((this.width - 8) * this.sliderValue);
            if (activeWidth > 0) {
                drawRect(this.x + 4, this.y + 4, this.x + 4 + activeWidth, this.y + this.height - 4, COLOR_ACTIVE);
            }
            // Handle blanco
            int handleX = this.x + 4 + activeWidth - 2;
            drawRect(handleX, this.y + 2, handleX + 4, this.y + this.height - 2, COLOR_HANDLE);
        }
    }
    
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (super.mousePressed(mc, mouseX, mouseY)) {
            this.sliderValue = (double)(mouseX - (this.x + 4)) / (double)(this.width - 8);
            if (this.sliderValue < 0.0) this.sliderValue = 0.0;
            if (this.sliderValue > 1.0) this.sliderValue = 1.0;
            this.dragging = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }
    
    public double getSliderValue() {
        return this.minValue + (this.maxValue - this.minValue) * this.sliderValue;
    }
    
    public void setSliderValue(double value) {
        this.sliderValue = (value - this.minValue) / (this.maxValue - this.minValue);
        if (this.sliderValue < 0.0) this.sliderValue = 0.0;
        if (this.sliderValue > 1.0) this.sliderValue = 1.0;
    }
}
