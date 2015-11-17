package jp.co.cyberagent.android.gpuimage.sample;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

/**
 * Created by ryohei on 15/11/17.
 */
public class FilterNodeGroup extends GPUImageFilter {

    private List<FilterNode> nodes = new ArrayList<>();

    private FilterNode inputNode;
    private FilterNode outputNode;

    public FilterNodeGroup() {

    }

    public void addNode(FilterNode node) {
        nodes.add(node);
    }

    public void setInputNode(FilterNode inputNode) {
        this.inputNode = inputNode;
    }

    public void setOutputNode(FilterNode outputNode) {
        this.outputNode = outputNode;
    }

    @Override
    public void onOutputSizeChanged(int width, int height) {
        super.onOutputSizeChanged(width, height);

        for (FilterNode node : nodes) {
            node.onOutputSizeChanged(width, height);
        }
    }

    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        for (FilterNode node : nodes) {
            node.useNextFrame();
        }

        outputNode.setDrawBuffer(cubeBuffer, textureBuffer);
        inputNode.setFirstTexture(textureId);
    }
}
