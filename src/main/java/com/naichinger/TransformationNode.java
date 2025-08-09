package com.naichinger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.util.glsl.ShaderState;

public class TransformationNode extends SceneGraphNode {
    private Matrix4f matrix;

    public TransformationNode(ShaderState st) {
        super(st);
        matrix = new Matrix4f();
    }

    public void setMatrix(Matrix4f mat) {
        this.matrix = mat;
    }

    public Matrix4f getMatrix() {
        return this.matrix;
    }

    @Override
    public void render(GL3 gl, Matrix4f model, Matrix4f view) {
        super.render(gl, new Matrix4f().mul(model, matrix), view);
    }
}
