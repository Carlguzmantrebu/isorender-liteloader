package com.isometricrender.render;

import com.isometricrender.AreaSelection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class WorldRenderer {
    private final World world;
    private final Minecraft mc;
    
    private int fbo = -1;
    private int fboTexture = -1;
    private int fboDepth = -1;
    private int fboWidth = -1;
    private int fboHeight = -1;
    
    public WorldRenderer(World world) {
        this.world = world;
        this.mc = Minecraft.getMinecraft();
    }
    
    public BufferedImage render(AreaSelection selection, float scale, float rotation, float slant, int resolution)
 {
        ensureFBO(resolution);
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, resolution, resolution);
        
        setupProjection(selection, scale, rotation);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        
        renderBlocks(selection);
        renderTileEntities(selection);
        renderEntities(selection);
        
        BufferedImage image = captureFramebuffer(resolution);
        
        restoreState();
        
        return image;
    }
    
    private void ensureFBO(int size) {
        if (fbo != -1 && fboWidth == size && fboHeight == size) {
            return;
        }
        
        cleanup();
        
        fbo = GL30.glGenFramebuffers();
        fboTexture = GL11.glGenTextures();
        fboDepth = GL30.glGenRenderbuffers();
        fboWidth = fboHeight = size;
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fboTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, size, 0, 
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
            GL11.GL_TEXTURE_2D, fboTexture, 0);
        
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fboDepth);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, size, size);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 
            GL30.GL_RENDERBUFFER, fboDepth);
    }
    
    private void setupProjection(AreaSelection selection, float scale, float rotation) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        int maxDim = Math.max(selection.getSizeX(), Math.max(selection.getSizeY(), selection.getSizeZ()));
        float orthoSize = maxDim / scale;
        GL11.glOrtho(-orthoSize, orthoSize, -orthoSize, orthoSize, -1000, 1000);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glRotatef(slant, 1, 0, 0);
        GL11.glRotatef(rotation, 0, 1, 0);
        
        float cx = (selection.getMinX() + selection.getMaxX()) * 0.5f;
        float cy = (selection.getMinY() + selection.getMaxY()) * 0.5f;
        float cz = (selection.getMinZ() + selection.getMaxZ()) * 0.5f;
        GL11.glTranslatef(-cx, -cy, -cz);
    }
    
    private void renderBlocks(AreaSelection selection) {
        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = selection.getMinX(), maxX = selection.getMaxX();
        int minY = selection.getMinY(), maxY = selection.getMaxY();
        int minZ = selection.getMinZ(), maxZ = selection.getMaxZ();
        
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    pos.setPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    
                    if (state.getBlock() != Blocks.AIR) {
                        try {
                            dispatcher.renderBlock(state, pos, world, buf);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        
        tess.draw();
    }
    
    private void renderTileEntities(AreaSelection selection) {
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        int minX = selection.getMinX(), maxX = selection.getMaxX();
        int minY = selection.getMinY(), maxY = selection.getMaxY();
        int minZ = selection.getMinZ(), maxZ = selection.getMaxZ();
        
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    pos.setPos(x, y, z);
                    TileEntity te = world.getTileEntity(pos);
                    
                    if (te != null) {
                        try {
                            GL11.glPushMatrix();
                            dispatcher.render(te, x, y, z, 0.0f, -1, 1.0f);
                            GL11.glPopMatrix();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }
    
    private void renderEntities(AreaSelection selection) {
        RenderManager manager = mc.getRenderManager();
        
        AxisAlignedBB aabb = new AxisAlignedBB(
            selection.getMinX(), selection.getMinY(), selection.getMinZ(),
            selection.getMaxX() + 1, selection.getMaxY() + 1, selection.getMaxZ() + 1
        );
        
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, aabb);
        
        for (Entity entity : entities) {
            try {
                GL11.glPushMatrix();
                manager.renderEntity(entity, 
                    entity.posX, entity.posY, entity.posZ,
                    entity.rotationYaw, 0.0f, false);
                GL11.glPopMatrix();
            } catch (Exception ignored) {}
        }
    }
    
    private BufferedImage captureFramebuffer(int size) {
        int pixelCount = size * size;
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixelCount * 4);
        GL11.glReadPixels(0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        
        int srcIdx = 0;
        for (int y = size - 1; y >= 0; y--) {
            int dstIdx = y * size;
            for (int x = 0; x < size; x++, dstIdx++) {
                int r = buffer.get(srcIdx++) & 0xFF;
                int g = buffer.get(srcIdx++) & 0xFF;
                int b = buffer.get(srcIdx++) & 0xFF;
                int a = buffer.get(srcIdx++) & 0xFF;
                pixels[dstIdx] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        
        return image;
    }
    
    private void restoreState() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        mc.getFramebuffer().bindFramebuffer(true);
    }
    
    public void exportImage(BufferedImage image) {
        if (image == null) return;
        
        File screenshotsDir = new File(mc.mcDataDir, "screenshots");
        screenshotsDir.mkdirs();
        
        File output = new File(screenshotsDir, "isometric_" + System.currentTimeMillis() + ".png");
        
        try {
            ImageIO.write(image, "PNG", output);
            if (mc.player != null) {
                mc.player.sendChatMessage("§aImage saved: " + output.getName());
            }
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendChatMessage("§cFailed to save image");
            }
        }
    }
    
    public void cleanup() {
        if (fbo != -1) {
            GL30.glDeleteFramebuffers(fbo);
            fbo = -1;
        }
        if (fboTexture != -1) {
            GL11.glDeleteTextures(fboTexture);
            fboTexture = -1;
        }
        if (fboDepth != -1) {
            GL30.glDeleteRenderbuffers(fboDepth);
            fboDepth = -1;
        }
        fboWidth = fboHeight = -1;
    }
}
