package com.isometricrender.gui;

import com.isometricrender.AreaSelection;
import com.isometricrender.render.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.IntBuffer;

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
    
    private static void logStackTrace(String message) {
        System.out.println("[IsometricRender] STACK TRACE for: " + message);
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(stack.length, 15); i++) {
            System.out.println("[IsometricRender]   at " + stack[i].toString());
        }
    }
    
    public RenderScreen(AreaSelection selection) {
        super();
        this.selection = selection;
        System.out.println("[IsometricRender] === CONSTRUCTOR ===");
        System.out.println("[IsometricRender] Constructor called");
        System.out.println("[IsometricRender] mc instance: " + Minecraft.getMinecraft());
        System.out.println("[IsometricRender] currentScreen before: " + Minecraft.getMinecraft().currentScreen);
        logStackTrace("Constructor");
    }
    
    @Override
    public void initGui() {
        System.out.println("[IsometricRender] === INITGUI START ===");
        logStackTrace("initGui() START");
        
        try {
            System.out.println("[IsometricRender] mc: " + this.mc);
            System.out.println("[IsometricRender] width: " + this.width + ", height: " + this.height);
            System.out.println("[IsometricRender] currentScreen during initGui: " + this.mc.currentScreen);
            
            this.buttonList.clear();
            System.out.println("[IsometricRender] buttonList cleared");
            
            int rightPanel = this.width - 160;
            System.out.println("[IsometricRender] rightPanel position: " + rightPanel);
            
            this.buttonList.add(new GuiSlider(0, rightPanel, 40, 150, 20, "Scale: ", "", 
                1.0, 10.0, scale, true, true, slider -> {
                    scale = (float) slider.getValue();
                    scheduleRender();
                }));
            System.out.println("[IsometricRender] Scale slider added");
            
            this.buttonList.add(new GuiSlider(1, rightPanel, 70, 150, 20, "Rotation: ", "°", 
                0.0, 360.0, rotation, true, true, slider -> {
                    rotation = (float) slider.getValue();
                    scheduleRender();
                }));
            System.out.println("[IsometricRender] Rotation slider added");
            
            this.buttonList.add(new GuiSlider(2, rightPanel, 100, 150, 20, "Resolution: ", "px", 
                256.0, 2048.0, resolution, true, true, slider -> {
                    resolution = (int) slider.getValue();
                    scheduleRender();
                }));
            System.out.println("[IsometricRender] Resolution slider added");
            
            this.buttonList.add(new GuiButton(3, rightPanel, 130, 150, 20, "Export PNG"));
            this.buttonList.add(new GuiButton(4, rightPanel, 160, 150, 20, "Close"));
            System.out.println("[IsometricRender] Buttons added, total buttons: " + this.buttonList.size());
            
            previewSize = Math.min(this.width - 200, this.height - 40);
            previewX = 20;
            previewY = 20;
            System.out.println("[IsometricRender] Preview area: x=" + previewX + ", y=" + previewY + ", size=" + previewSize);
            
            System.out.println("[IsometricRender] === INITGUI END SUCCESS ===");
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in initGui: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        System.out.println("[IsometricRender] === ACTION PERFORMED ===");
        System.out.println("[IsometricRender] Button pressed: id=" + button.id + ", displayString=" + button.displayString);
        logStackTrace("actionPerformed");
        
        try {
            if (button.id == 3 && renderedImage != null) {
                System.out.println("[IsometricRender] Export PNG clicked");
                renderer.exportImage(renderedImage);
            } else if (button.id == 4) {
                System.out.println("[IsometricRender] Close button clicked - calling cleanup and closing");
                cleanup();
                System.out.println("[IsometricRender] Calling mc.displayGuiScreen(null)");
                this.mc.displayGuiScreen(null);
            }
            System.out.println("[IsometricRender] actionPerformed completed");
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in actionPerformed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private void scheduleRender() {
        System.out.println("[IsometricRender] scheduleRender() called");
        renderPending = true;
    }
    
    @Override
    public void updateScreen() {
        System.out.println("[IsometricRender] === UPDATESCREEN START ===");
        
        try {
            System.out.println("[IsometricRender] renderPending=" + renderPending);
            System.out.println("[IsometricRender] mc.currentScreen=" + this.mc.currentScreen);
            System.out.println("[IsometricRender] this=" + this);
            
            if (renderPending) {
                long now = System.currentTimeMillis();
                System.out.println("[IsometricRender] now=" + now + ", lastRenderTime=" + lastRenderTime + ", diff=" + (now - lastRenderTime));
                
                if (now - lastRenderTime >= RENDER_DELAY_MS) {
                    if (renderer == null) {
                        System.out.println("[IsometricRender] Creating WorldRenderer...");
                        System.out.println("[IsometricRender] mc.world=" + this.mc.world);
                        renderer = new WorldRenderer(this.mc.world);
                        System.out.println("[IsometricRender] WorldRenderer created: " + renderer);
                    }
                    
                    System.out.println("[IsometricRender] Calling updateRender()...");
                    updateRender();
                    renderPending = false;
                    lastRenderTime = now;
                    System.out.println("[IsometricRender] Render completed, lastRenderTime updated");
                } else {
                    System.out.println("[IsometricRender] Waiting for render delay...");
                }
            }
            System.out.println("[IsometricRender] === UPDATESCREEN END ===");
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in updateScreen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateRender() {
        System.out.println("[IsometricRender] === UPDATERENDER START ===");
        
        try {
            System.out.println("[IsometricRender] renderer=" + renderer);
            System.out.println("[IsometricRender] selection=" + selection);
            System.out.println("[IsometricRender] scale=" + scale + ", rotation=" + rotation + ", resolution=" + resolution);
            
            renderedImage = renderer.render(selection, scale, rotation, resolution);
            
            if (renderedImage == null) {
                System.out.println("[IsometricRender] ERROR: render returned null!");
                return;
            }
            
            System.out.println("[IsometricRender] Render success, image size: " + renderedImage.getWidth() + "x" + renderedImage.getHeight());
            System.out.println("[IsometricRender] Calling uploadTexture()...");
            uploadTexture();
            System.out.println("[IsometricRender] === UPDATERENDER END ===");
            
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in updateRender: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void uploadTexture() {
        System.out.println("[IsometricRender] uploadTexture() called");
        
        try {
            if (renderedImage == null) {
                System.out.println("[IsometricRender] uploadTexture: renderedImage is null, skipping");
                return;
            }
            
            if (glTextureId == -1) {
                glTextureId = GL11.glGenTextures();
                System.out.println("[IsometricRender] Generated new texture ID: " + glTextureId);
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
            
            System.out.println("[IsometricRender] Texture uploaded successfully! ID=" + glTextureId);
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in uploadTexture: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        System.out.println("[IsometricRender] === DRAWSCREEN START === mouseX=" + mouseX + ", mouseY=" + mouseY + ", partialTicks=" + partialTicks);
        
        try {
            this.drawDefaultBackground();
            System.out.println("[IsometricRender] drawDefaultBackground() completed");
            
            this.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF555555);
            this.drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000);
            System.out.println("[IsometricRender] Preview borders drawn");
            
            if (glTextureId != -1) {
                System.out.println("[IsometricRender] Drawing texture ID=" + glTextureId);
                drawTexture(glTextureId, previewX, previewY, previewSize, previewSize);
                System.out.println("[IsometricRender] Texture drawn");
            } else {
                System.out.println("[IsometricRender] No texture yet, drawing 'Rendering...' text");
                this.drawCenteredString(this.fontRenderer, "Rendering...", previewX + previewSize/2, previewY + previewSize/2, 0xFFFFFF);
            }
            
            System.out.println("[IsometricRender] Calling super.drawScreen()...");
            super.drawScreen(mouseX, mouseY, partialTicks);
            System.out.println("[IsometricRender] super.drawScreen() completed");
            
            this.drawString(this.fontRenderer, "Isometric Render", this.width - 160, 20, 0xFFFFFF);
            System.out.println("[IsometricRender] === DRAWSCREEN END ===");
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in drawScreen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void drawTexture(int texId, int x, int y, int width, int height) {
        try {
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
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in drawTexture: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanup() {
        System.out.println("[IsometricRender] cleanup() called");
        logStackTrace("cleanup()");
        
        try {
            if (glTextureId != -1) {
                System.out.println("[IsometricRender] Deleting texture ID=" + glTextureId);
                GL11.glDeleteTextures(glTextureId);
                glTextureId = -1;
            }
            if (renderer != null) {
                System.out.println("[IsometricRender] Cleaning up renderer");
                renderer.cleanup();
                renderer = null;
            }
            renderedImage = null;
            System.out.println("[IsometricRender] cleanup() completed");
        } catch (Exception e) {
            System.out.println("[IsometricRender] ERROR in cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onGuiClosed() {
        System.out.println("[IsometricRender] === ONGUICLOSED ===");
        System.out.println("[IsometricRender] onGuiClosed() called!");
        System.out.println("[IsometricRender] THIS IS THE KEY - WHO CALLED onGuiClosed()?");
        logStackTrace("onGuiClosed() - CRITICAL!");
        
        cleanup();
        System.out.println("[IsometricRender] === ONGUICLOSED END ===");
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        System.out.println("[IsometricRender] doesGuiPauseGame() called, returning false");
        return false;
    }
    
    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        System.out.println("[IsometricRender] onResize() called: w=" + w + ", h=" + h);
        super.onResize(mcIn, w, h);
        System.out.println("[IsometricRender] onResize() completed");
    }
    
    @Override
    public void setWorldAndResolution(Minecraft mcIn, int widthIn, int heightIn) {
        System.out.println("[IsometricRender] setWorldAndResolution() called: width=" + widthIn + ", height=" + heightIn);
        super.setWorldAndResolution(mcIn, widthIn, heightIn);
        System.out.println("[IsometricRender] setWorldAndResolution() completed");
    }
}
