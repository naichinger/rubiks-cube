package com.naichinger;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.util.glsl.ShaderState;

public class SceneGraphNode {

    private List<SceneGraphNode> children;
    protected ShaderState st;

    public SceneGraphNode(ShaderState st) {
        this.st = st;
        children = new ArrayList<>();
    }

    public void addChild(SceneGraphNode node) {
        children.add(node);
    }

    void render(GL3 gl, Matrix4f model, Matrix4f view) {
        for (SceneGraphNode sceneGraphNode : children) {
            sceneGraphNode.render(gl, model, view);
        }
    }
}
