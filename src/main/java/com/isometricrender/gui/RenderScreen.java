package com.isometricrender.gui;

import com.isometricrender.AreaSelection;
import com.isometricrender.render.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL12;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class RenderScreen extends GuiScreen {
    private final AreaSelection selection;
    private WorldRenderer renderer;
    private BufferedImage renderedImage;
    private int glTextureId = -1;
    
    private float scale = 3.0f;
    private float rotation = 45.0f;
    private int resolution = 512;
    
    private int previewX, previewY, previewSize;
    
    private long lastRenderTime = 0;
    private static final long RENDER_DELAY_MS = 100;
    private boolean renderPending = false;
    
    public RenderScreen(AreaSelection selection) {
        this.selection = selection;
    }
    
    @Override
    public void initGui() {
        try {
            this.buttonList.clear();
            
            int rightPanel = this.width - 160;
            
            this.buttonList.add(new GuiSlider(0, rightPanel, 40, 150, 20, "Scale: ", "", 
                1.0, 10.0, scale, true, true, slider -> {
                    scale = (float) slider.getValue();
                    scheduleRender();
                }));
            
            this.buttonList.add(new GuiSlider(1, rightPanel, 70, 150, 20, "Rotation: ", "°", 
                0.0, 360.0, rotation, true, true, slider -> {
                    rotation = (float) slider.getValue();
                    scheduleRender();
                }));
            
            this.buttonList.add(new GuiSlider(2, rightPanel, 100, 150, 20, "Resolution: ", "px", 
                256.0, 2048.0, resolution, true, true, slider -> {
                    resolution = (int) slider.getValue();
                    scheduleRender();
                }));
            
            this.buttonList.add(new GuiButton(3, rightPanel, 130, 150, 20, "Export PNG"));
            this.buttonList.add(new GuiButton(4, rightPanel, 160, 150, 20, "Close"));
            
            previewSize = Math.min(this.width - 200, this.height - 40);
            previewX = 20;
            previewY = 20;
            
            renderer = new WorldRenderer(this.mc.world);
            // No hacer updateRender() aquí - deferir hasta drawScreen()
            scheduleRender();
        } catch (Exception e) {
            e.printStackTrace();
            this.mc.displayGuiScreen(null);
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 3 && renderedImage != null) {
            renderer.exportImage(renderedImage);
        } else if (button.id == 4) {
            cleanup();
            this.mc.displayGuiScreen(null);
        }
    }
    
    private void scheduleRender() {
        renderPending = true;
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        
        if (renderPending) {
            long now = System.currentTimeMillis();
            if (now - lastRenderTime >= RENDER_DELAY_MS) {
                updateRender();
                renderPending = false;
                lastRenderTime = now;
            }
        }
    }
    
    private void updateRender() {
        if (renderer == null) return;
        
        renderedImage = renderer.render(selection, scale, rotation, resolution);
        uploadTexture();
    }
    
    private void uploadTexture() {
        if (renderedImage == null) return;
        
        if (glTextureId == -1) {
            glTextureId = GL11.glGenTextures();
        }
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        int[] pixels = ((DataBufferInt) renderedImage.getRaster().getDataBuffer()).getData();
        IntBuffer buffer = BufferUtils.createIntBuffer(pixels.length);
        buffer.put(pixels);
        buffer.flip();
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 
            renderedImage.getWidth(), renderedImage.getHeight(), 
            0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buffer);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Renderizar si hay pendientes o si no hay imagen
        if (renderPending || renderedImage == null) {
            updateRender();
            renderPending = false;
        }
        
        this.drawDefaultBackground();
        
        if (glTextureId != -1) {
            drawTexture(glTextureId, previewX, previewY, previewSize, previewSize);
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        drawString(this.fontRenderer, "Isometric Render", this.width - 160, 20, 0xFFFFFF);
    }
    
    private void drawTexture(int texId, int x, int y, int width, int height) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x, y + height, 0).tex(0, 1).endVertex();
        buf.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        buf.pos(x + width, y, 0).tex(1, 0).endVertex();
        buf.pos(x, y, 0).tex(0, 0).endVertex();
        tess.draw();
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }
    
    private void cleanup() {
        if (glTextureId != -1) {
            GL11.glDeleteTextures(glTextureId);
            glTextureId = -1;
        }
        if (renderer != null) {
            renderer.cleanup();
            renderer = null;
        }
        renderedImage = null;
    }
    
    @Override
    public void onGuiClosed() {
        cleanup();
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
