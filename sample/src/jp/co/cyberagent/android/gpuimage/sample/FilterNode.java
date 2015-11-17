package jp.co.cyberagent.android.gpuimage.sample;

import android.opengl.GLES20;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageTwoInputFilter;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Created by ryohei on 15/11/17.
 */
public class FilterNode {

    private final GPUImageFilter filter;
    private List<FilterNode> firstTargets = new ArrayList<>();
    private List<FilterNode> secondTargets = new ArrayList<>();
    private int firstTexture;
    private int secondTexture;
    private boolean hasReceivedFirstTexture;
    private boolean hasReceivedSecondTexture;

    private FloatBuffer mGLCubeBuffer;
    private FloatBuffer mGLTextureBuffer;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    public FilterNode(GPUImageFilter filter) {
        this.filter = filter;

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);
    }

    public void setDrawBuffer(FloatBuffer glCubeBuffer, FloatBuffer glTextureBuffer) {
        mGLCubeBuffer = glCubeBuffer;
        mGLTextureBuffer = glTextureBuffer;
    }

    public GPUImageFilter getFilter() {
        return filter;
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mFrameBuffers = new int[1];
        mFrameBufferTextures = new int[1];

        GLES20.glGenTextures(1, mFrameBufferTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void useNextFrame() {
        hasReceivedFirstTexture = false;
        hasReceivedSecondTexture = false;
    }

    public void addFirstTarget(FilterNode node) {
        firstTargets.add(node);
    }

    public void addSecondTarget(FilterNode node) {
        secondTargets.add(node);
    }

    public void setSecondTexture(final int textureId) {
        if (hasReceivedSecondTexture) {
            return;
        }

        secondTexture = textureId;
        hasReceivedSecondTexture = true;
        processOutput();
    }

    public void setFirstTexture(final int textureId) {
        if (hasReceivedFirstTexture) {
            return;
        }

        firstTexture = textureId;
        hasReceivedFirstTexture = true;
        processOutput();
    }

    private void processOutput() {
        if (!hasReceivedFirstTexture) {
            return;
        }

        if (filter instanceof GPUImageTwoInputFilter) {
            if (!hasReceivedSecondTexture) {
                return;
            }

            GPUImageTwoInputFilter twoInputFilter = (GPUImageTwoInputFilter)filter;
            setFilterSourceTexture2(twoInputFilter, secondTexture);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glClearColor(0, 0, 0, 0);

        filter.onDraw(firstTexture, mGLCubeBuffer, mGLTextureBuffer);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        int resultTexture = mFrameBufferTextures[0];

        for (FilterNode target : firstTargets) {
            target.setFirstTexture(resultTexture);
        }

        for (FilterNode target : secondTargets) {
            target.setSecondTexture(resultTexture);
        }
    }

    // private フィールドの mFilterSourceTexture2 に値をセットする
    static public void setFilterSourceTexture2(GPUImageTwoInputFilter filter, int texture) {
        try {
            Field f = filter.getClass().getDeclaredField("mFilterSourceTexture2");
            f.setAccessible(true);
            f.set(filter, texture);
        } catch (NoSuchFieldException ignore) {
        } catch (IllegalAccessException ignore) {
        }
    }
}
