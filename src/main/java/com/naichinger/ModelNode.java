package com.naichinger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.glsl.ShaderState;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_TRIANGLES;

public class ModelNode extends SceneGraphNode {
    private int[] vertexBufferId;
    private Vec3f color;

    public ModelNode(ShaderState st, int[] vertexBufferId, Vec3f color) {
        super(st);
        this.vertexBufferId = vertexBufferId;
        this.color = color;
    }

    @Override
    void render(GL3 gl, Matrix4f model, Matrix4f view) {

        int u_modelViewMatrix = st.getUniformLocation(gl, "u_modelViewMatrix");
        
        float[] modelViewMatrix = new float[16];
        new Matrix4f().mul(view, model).get(modelViewMatrix);
        gl.glUniformMatrix4fv(u_modelViewMatrix, 1, false, modelViewMatrix, 0);

        int positionLocation = st.getAttribLocation(gl, "a_position");

        gl.glEnableVertexAttribArray(positionLocation);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferId[0]);
        gl.glVertexAttribPointer(positionLocation, 3, GL_FLOAT, false, 0, 0);

        int u_color = st.getUniformLocation(gl, "u_color");
        gl.glUniform3f(u_color, color.x(), color.y(), color.z());

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDrawArrays(GL_TRIANGLES, 0, 6);
        gl.glDisable(GL_DEPTH_TEST);

        super.render(gl, model, view);
    }
}
