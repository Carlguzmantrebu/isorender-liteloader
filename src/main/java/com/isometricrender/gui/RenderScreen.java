
package com.isometricrender.gui;
import com.isometricrender.AreaSelection;
import com.isometricrender.render.WorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
public class RenderScreen extends GuiScreen {
    private long lastChangeTime = 0;
    private static final long RENDER_DEBOUNCE_MS = 50; // Reducido a 50ms para respuesta más rápida
    private boolean isRendering = false;

    
    private final AreaSelection selection;
    private WorldRenderer renderer;
    private BufferedImage renderedImage;
    private int glTextureId = -1;
    
    // Valores actuales
    private float scale = 3.0f;
    private float rotation = 45.0f;
    private float slant = 35.264f;
    private int resolution = 1024;
    
    // Valores default
    private static final float DEFAULT_SCALE = 3.0f;
    private static final float DEFAULT_ROTATION = 45.0f;
    private static final float DEFAULT_SLANT = 35.264f;
    private static final int DEFAULT_RESOLUTION = 1024;
    
    private int previewX, previewY, previewSize;
    private int panelX, panelWidth = 280;  // AUMENTADO A 280px
    
    private long lastRenderTime = 0;
    private static final long RENDER_DELAY_MS = 100;
    private boolean renderPending = false;
    
    // Componentes UI
    private GuiTextField scaleTextField;
    private GuiTextField rotationTextField;
    private GuiTextField slantTextField;
    private GuiTextField customResTextField;
    
    // Sliders custom
    private CustomSlider scaleSlider;
    private CustomSlider rotationSlider;
    private CustomSlider slantSlider;
    
    public RenderScreen(AreaSelection selection) {
        this.selection = selection;
    }
    
    @Override
    public void initGui() {
        // Calcular áreas
        panelX = this.width - panelWidth - 10;
        previewSize = Math.min(panelX - 40, this.height - 80);
        previewX = 20;
        previewY = 40;
        
        // Sección Transform Options (empieza en Y=30)
        int currentY = 40;
        
        // Scale: Label [TextField] [Slider]
        scaleTextField = new GuiTextField(100, this.fontRenderer, panelX + 70, currentY + 15, 50, 14);
        scaleTextField.setText(String.format("%.1f", scale));
        scaleSlider = new CustomSlider(0, panelX + 125, currentY + 15, 145, 14, "", "", 0.5, 10.0, scale, false, false);
        
        currentY += 35;
        
        // Rotation
        rotationTextField = new GuiTextField(101, this.fontRenderer, panelX + 70, currentY + 15, 50, 14);
        rotationTextField.setText(String.format("%.0f", rotation));
        rotationSlider = new CustomSlider(1, panelX + 125, currentY + 15, 145, 14, "", "", 0.0, 360.0, rotation, false, false);
        
        currentY += 35;
        
        // Slant
        slantTextField = new GuiTextField(102, this.fontRenderer, panelX + 70, currentY + 15, 50, 14);
        slantTextField.setText(String.format("%.1f", slant));
        slantSlider = new CustomSlider(2, panelX + 125, currentY + 15, 145, 14, "", "", 0.0, 90.0, slant, false, false);
        
        currentY += 50;
        
        // Sección Presets
        this.buttonList.add(new GuiButton(10, panelX + 10, currentY, 130, 18, "Dimetric"));
        this.buttonList.add(new GuiButton(11, panelX + 145, currentY, 130, 18, "Isometric"));
        
        currentY += 25;
        this.buttonList.add(new GuiButton(12, panelX + 10, currentY, 265, 18, "Reset View"));
        
        currentY += 45;
        
        // Sección Export
        this.buttonList.add(new GuiButton(20, panelX + 10, currentY, 85, 18, "1024"));
        this.buttonList.add(new GuiButton(21, panelX + 100, currentY, 85, 18, "2048"));
        this.buttonList.add(new GuiButton(22, panelX + 190, currentY, 85, 18, "4096"));
        
        currentY += 28;
        
        // Custom resolution
        customResTextField = new GuiTextField(103, this.fontRenderer, panelX + 70, currentY, 60, 14);
        customResTextField.setText(String.valueOf(resolution));
        this.buttonList.add(new GuiButton(23, panelX + 135, currentY - 2, 140, 18, "Export"));
        
        currentY += 28;
        this.buttonList.add(new GuiButton(24, panelX + 10, currentY, 265, 18, "Copy to Clipboard"));
        
        currentY += 35;
        this.buttonList.add(new GuiButton(25, panelX + 10, currentY, 265, 20, "Close"));
        
        // Inicializar renderer
        renderer = new WorldRenderer(this.mc.world);
        scheduleRender();
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 10: // Dimetric
                setRotation(30f);
                setSlant(30f);
                break;
            case 11: // Isometric
                setRotation(45f);
                setSlant(35.264f);
                break;
            case 12: // Reset View
                setScale(DEFAULT_SCALE);
                setRotation(DEFAULT_ROTATION);
                setSlant(DEFAULT_SLANT);
                setResolution(DEFAULT_RESOLUTION);
                break;
            case 20: // 1024
                setResolution(1024);
                break;
            case 21: // 2048
                setResolution(2048);
                break;
            case 22: // 4096
                setResolution(4096);
                break;
            case 23: // Export
                if (renderedImage != null) {
                    renderer.exportImage(renderedImage);
                }
                break;
            case 24: // Copy to Clipboard
                copyToClipboard();
                break;
            case 25: // Close
                cleanup();
                this.mc.displayGuiScreen(null);
                break;
        }
    }
    
    private void setScale(float value) {
        scale = Math.max(0.5f, Math.min(10f, value));
        scaleSlider.setSliderValue(scale);
        scaleTextField.setText(String.format("%.1f", scale));
        scheduleRender();
    }
    
    private void setRotation(float value) {
        rotation = value % 360f;
        rotationSlider.setSliderValue(rotation);
        rotationTextField.setText(String.format("%.0f", rotation));
        scheduleRender();
    }
    
    private void setSlant(float value) {
        slant = Math.max(0f, Math.min(90f, value));
        slantSlider.setSliderValue(slant);
        slantTextField.setText(String.format("%.1f", slant));
        scheduleRender();
    }
    
    private void setResolution(int value) {
        resolution = Math.max(128, Math.min(8192, value));
        customResTextField.setText(String.valueOf(resolution));
        scheduleRender();
    }
    
    private void scheduleRender() {
        renderPending = true;
    }
    
    private void copyToClipboard() {
        if (renderedImage == null) return;
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(renderedImage, "PNG", baos);
            
            // Crear imagen compatible para clipboard
            BufferedImage clipboardImage = new BufferedImage(
                renderedImage.getWidth(), 
                renderedImage.getHeight(), 
                BufferedImage.TYPE_INT_ARGB
            );
            clipboardImage.setData(renderedImage.getData());
            
            // Copiar al clipboard
            Transferable transferable = new TransferableImage(clipboardImage);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(transferable, null);
            
            System.out.println("[IsometricRender] Image copied to clipboard");
        } catch (Exception e) {
            System.out.println("[IsometricRender] Failed to copy: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void updateScreen() {
        scaleTextField.updateCursorCounter();
        rotationTextField.updateCursorCounter();
        slantTextField.updateCursorCounter();
        customResTextField.updateCursorCounter();
        
        // Leer valores de sliders
        float newScale = (float) scaleSlider.getSliderValue();
        float newRotation = (float) rotationSlider.getSliderValue();
        float newSlant = (float) slantSlider.getSliderValue();
        
        if (newScale != scale) {
            scale = newScale;
            scaleTextField.setText(String.format("%.1f", scale));
            scheduleRender();
        }
        if (newRotation != rotation) {
            rotation = newRotation;
            rotationTextField.setText(String.format("%.0f", rotation));
            scheduleRender();
        }
        if (newSlant != slant) {
            slant = newSlant;
            slantTextField.setText(String.format("%.1f", slant));
            scheduleRender();
        }
        
        // Renderizado
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
        try {
            renderedImage = renderer.render(selection, scale, rotation, slant, resolution);
            if (renderedImage != null) {
                uploadTexture();
            }
        } catch (Exception e) {
            System.out.println("[IsometricRender] Render error: " + e.getMessage());
            e.printStackTrace();
        }
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        scaleTextField.mouseClicked(mouseX, mouseY, mouseButton);
        rotationTextField.mouseClicked(mouseX, mouseY, mouseButton);
        slantTextField.mouseClicked(mouseX, mouseY, mouseButton);
        customResTextField.mouseClicked(mouseX, mouseY, mouseButton);
        
        scaleSlider.mousePressed(this.mc, mouseX, mouseY);
        rotationSlider.mousePressed(this.mc, mouseX, mouseY);
        slantSlider.mousePressed(this.mc, mouseX, mouseY);
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        scaleSlider.mouseReleased(mouseX, mouseY);
        rotationSlider.mouseReleased(mouseX, mouseY);
        slantSlider.mouseReleased(mouseX, mouseY);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        
        if (scaleTextField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                setScale(Float.parseFloat(scaleTextField.getText()));
            } catch (NumberFormatException ignored) {}
        }
        if (rotationTextField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                setRotation(Float.parseFloat(rotationTextField.getText()));
            } catch (NumberFormatException ignored) {}
        }
        if (slantTextField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                setSlant(Float.parseFloat(slantTextField.getText()));
            } catch (NumberFormatException ignored) {}
        }
        if (customResTextField.textboxKeyTyped(typedChar, keyCode)) {
            try {
                setResolution(Integer.parseInt(customResTextField.getText()));
            } catch (NumberFormatException ignored) {}
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Fondo
        this.drawDefaultBackground();
        
        // Título centrado arriba
        String title = "ISOMETRIC RENDER";
        this.drawCenteredString(this.fontRenderer, "\u00A7e\u00A7l" + title, this.width / 2, 15, 0xFFFFFF);
        
        // Panel lateral negro (ahora más ancho)
        drawRect(panelX, 35, panelX + panelWidth, this.height - 10, 0xDD000000);
        drawRect(panelX, 35, panelX + 1, this.height - 10, 0xFF555555);
        
        // Sección Transform Options
        int currentY = 45;
        drawCenteredString(fontRenderer, "\u00A7e\u00A7lTransform Options", panelX + panelWidth / 2, currentY, 0xFFFFFF);
        
        currentY += 20;
        this.fontRenderer.drawString("Scale:", panelX + 10, currentY + 4, 0xCCCCCC);
        scaleTextField.drawTextBox();
        scaleSlider.drawButton(this.mc, mouseX, mouseY, partialTicks);
        
        currentY += 35;
        this.fontRenderer.drawString("Rotation:", panelX + 10, currentY + 4, 0xCCCCCC);
        rotationTextField.drawTextBox();
        rotationSlider.drawButton(this.mc, mouseX, mouseY, partialTicks);
        
        currentY += 35;
        this.fontRenderer.drawString("Slant:", panelX + 10, currentY + 4, 0xCCCCCC);
        slantTextField.drawTextBox();
        slantSlider.drawButton(this.mc, mouseX, mouseY, partialTicks);
        
        // Sección Presets
        currentY += 40;
        drawCenteredString(fontRenderer, "\u00A77Presets", panelX + panelWidth / 2, currentY, 0xFFFFFF);
        
        // Sección Export
        currentY += 115;
        drawCenteredString(fontRenderer, "\u00A7e\u00A7lExport", panelX + panelWidth / 2, currentY, 0xFFFFFF);
        
        currentY += 20;
        this.fontRenderer.drawString("Custom:", panelX + 10, currentY + 4, 0xCCCCCC);
        customResTextField.drawTextBox();
        
        // Dibujar botones
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Preview area
        drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF555555);
        drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF222222);
        
        if (glTextureId != -1) {
            drawTexture(glTextureId, previewX, previewY, previewSize, previewSize);
        } else {
            this.drawCenteredString(this.fontRenderer, "Rendering...", previewX + previewSize/2, previewY + previewSize/2, 0xAAAAAA);
        }
    }
    
    private void drawTexture(int texId, int x, int y, int width, int height) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1, 1, 1, 1);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(x, y + height, 0).tex(0, 1).endVertex();
        buf.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        buf.pos(x + width, y, 0).tex(1, 0).endVertex();
        buf.pos(x, y, 0).tex(0, 0).endVertex();
        tess.draw();
        
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
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
    
    // Clase helper para transferir imagen al clipboard
    private static class TransferableImage implements Transferable {
        private final BufferedImage image;
        
        public TransferableImage(BufferedImage image) {
            this.image = image;
        }
        
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
        
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) {
            return image;
        }
    }
}
