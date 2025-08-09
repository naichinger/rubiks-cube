package com.naichinger;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;

import java.awt.Event;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.FloatBuffer;
import java.util.ArrayList;

// window
import javax.swing.JFrame;

// openGL
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec2f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
// GLSL 			
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class BasicFrame extends JFrame implements GLEventListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 123L;

	final private int width = 800; // width and ...
	final private int height = 800; // ... height of window

	private float[] vertexData = {
			-1, -1, 0,
			1, -1, 0,
			1, 1, 0,

			1, 1, 0,
			-1, 1, 0,
			-1, -1, 0
	};

	private ShaderState st;

	TransformationNode rootNode;
	TransformationNode cubeRotationNode;
	TransformationNode[][] models;

	TransformationNode[][][][] referenceModel;

	public static Matrix4f projection;
	public static Matrix4f view;
	public static Matrix4f model;

	Vec2f mousePos = new Vec2f();

	public BasicFrame() {
		super("CG LAB");

		// select OpenGL core profile 3.x, with x >= 1
		// Programmable Shader (PSP) Only Profile
		GLProfile profile = GLProfile.get(GLProfile.GL3);

		// create rendering canvas
		GLCapabilities capabilities = new GLCapabilities(profile);
		GLCanvas canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);

		// to refresh the display regularly we need an animator
		// the FPSAnimator refreshes the display based on a target FPS
		FPSAnimator animator = new FPSAnimator(canvas, 60);
		animator.start();

		// create window using Swing
		this.setName("CG LAB");
		this.getContentPane().add(canvas);

		this.setSize(width, height);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.setResizable(true);
		canvas.requestFocusInWindow();
	}

	long frames = 0;

	// called repeatedly
	@Override
	public void display(GLAutoDrawable drawable) {
		// get the OpenGL 3.x graphics context (needs to align with the chosen
		// GLProfile)
		GL3 gl = drawable.getGL().getGL3();

		// clear the frame buffer
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		st.useProgram(gl, true);

		Matrix4f rot = new Matrix4f().setToRotationEuler(new Vec3f(0, 0.01f,0));
		Matrix4f rotNeg = new Matrix4f().setToRotationEuler(new Vec3f(0,-0.01f,0));

		int j = 2;
		for (int i = 0; i < 21; i++) {
			if (models[j][i] != null) {
				models[j][i].setMatrix(new Matrix4f().mul(rot, models[j][i].getMatrix()));
			}
			if (models[j + 1][i] != null) {
				// models[j+1][i].setMatrix(new Matrix4f().mul(rotNeg,
				// models[j+1][i].getMatrix()));
			}
		}

		rootNode.render(gl, new Matrix4f(), new Matrix4f());

		st.useProgram(gl, false);
		gl.glFlush();
		frames += 1;
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	// called once and immediately after the OpenGL context is initialized.
	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3(); // get the OpenGL 3.x graphics context
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // color for clearing the framebuffer, used by glClear

		initShaders(gl);

		FloatBuffer vertexBufferData = GLBuffers.newDirectFloatBuffer(vertexData);

		int[] vbID = new int[1];
		gl.glGenBuffers(1, vbID, 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbID[0]);
		gl.glBufferData(GL_ARRAY_BUFFER, vertexBufferData.capacity() * Float.BYTES, vertexBufferData, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

		rootNode = new TransformationNode(st);

		Matrix4f scaleMatrix = new Matrix4f().setToScale(0.15f, 0.15f, 0.15f);
		Matrix4f translateMatrix = new Matrix4f().setToTranslation(new Vec3f(0, 0, 0.1f));

		rootNode.setMatrix(new Matrix4f().mul(translateMatrix, scaleMatrix));

		cubeRotationNode = new TransformationNode(st);

		rootNode.addChild(cubeRotationNode);

		models = new TransformationNode[6][21];

		referenceModel = new TransformationNode[3][3][3][3];

		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int i = -1; i <= 1; i += 2) {

					// z
					ModelNode zSurface = new ModelNode(st, vbID, new Vec3f(0, (i + 1) / 2, (-i + 1) / 2));
					TransformationNode zTranslate = new TransformationNode(st);
					zTranslate.setMatrix(new Matrix4f().setToTranslation(2.5f * x, 2.5f * y, 3.5f * i));

					models[(i + 1) / 2][3 * (y + 1) + (x + 1)] = zTranslate;
					if (y != 0) { // on the edge
						models[2 + (y + 1) / 2][9 + 3 * (i + 1) / 2 + (x + 1)] = zTranslate;
					}
					if (x != 0) { // on the edge
						models[4 + (x + 1) / 2][9 + 6 + 3 * (i + 1) / 2 + (y + 1)] = zTranslate;
					}
					referenceModel[x+1][y+1][i+1][2] = zTranslate;

					// y
					ModelNode ySurface = new ModelNode(st, vbID, new Vec3f(1, 1, (i + 1) / 2));
					TransformationNode yTranslate = new TransformationNode(st);
					Matrix4f yTranslateMatrix = new Matrix4f().setToTranslation(2.5f * x, 3.5f * i, 2.5f * y);
					Matrix4f yRotateMatrix = new Matrix4f().setToRotationAxis((float) Math.toRadians(90),
							new Vec3f(1, 0, 0));
					yTranslate.setMatrix(new Matrix4f().mul(yTranslateMatrix, yRotateMatrix));

					if (y != 0) { // on the edge
						models[(y + 1) / 2][9 + 3 * (i + 1) / 2 + (x + 1)] = yTranslate;
					}
					models[2 + (i + 1) / 2][3 * (y + 1) + (x + 1)] = yTranslate;
					if (x != 0) { // on the edge
						models[4 + (x + 1) / 2][9 + 3 * (i + 1) / 2 + (y + 1)] = yTranslate;
					}
					referenceModel[x+1][y+1][i+1][1] = yTranslate;

					// x
					ModelNode xSurface = new ModelNode(st, vbID, new Vec3f(1, 0.5f * (i + 1) / 2, 0));
					TransformationNode xTranslate = new TransformationNode(st);
					Matrix4f xTranslateMatrix = new Matrix4f().setToTranslation(3.5f * i, 2.5f * x, 2.5f * y);
					Matrix4f xRotateMatrix = new Matrix4f().setToRotationAxis((float) Math.toRadians(90),
							new Vec3f(0, 1, 0));
					xTranslate.setMatrix(new Matrix4f().mul(xTranslateMatrix, xRotateMatrix));

					if (y != 0) { // on the edge
						// models[(y + 1) / 2][9 + 6 + 3 * (y + 1) / 2 + (x + 1)] = xTranslate;
						models[(y + 1) / 2][9 + 6 + 3 * (i + 1) / 2 + (x + 1)] = xTranslate;
					}
					if (x != 0) { // on the edge
						models[2 + (x + 1) / 2][9 + 6 + 3 * (i + 1) / 2 + (y + 1)] = xTranslate;
					}
					models[4 + (i + 1) / 2][3 * (y + 1) + (x + 1)] = xTranslate;
					referenceModel[x+1][y+1][i+1][0] = xTranslate;
					
					xTranslate.addChild(xSurface);
					yTranslate.addChild(ySurface);
					zTranslate.addChild(zSurface);

					cubeRotationNode.addChild(xTranslate);
					cubeRotationNode.addChild(yTranslate);
					cubeRotationNode.addChild(zTranslate);

				}
			}
		}



		// if(false) {
		// 	models[2][9] = null;
		// 	models[2][10] = null;
		// 	models[2][10] = null;
		// }

		// if(true)
		// 	return;

		// Matrix4f rot = new Matrix4f().setToRotationEuler(new Vec3f(0, 0, (float) Math.toRadians(90)));
		// for (int i = 0; i < models[0].length; i++) {
		// 	models[0][i].setMatrix(new Matrix4f().mul(rot, models[0][i].getMatrix()));
		// }

		// // 9x front
		// // 3x edge
		// // 3x edge
		// // 3x edge
		// // 3x edge

		// // 0 zf 1 zb 2 yu 3 yd 4 xl 5 xr

		// if (false) {
		// 	int offset = 9;
		// 	TransformationNode[] tmp = new TransformationNode[3];
		// 	for (int i = 0; i < 3; i++) {
		// 		tmp[i] = models[2][offset + i];
		// 	}
		// 	for (int i = 0; i < 4; i++) {
		// 		for (int j = 0; j < tmp.length; j++) {
		// 			TransformationNode t = models[2 + (i + 1) % 4][offset + j];
		// 			models[2 + (i + 1) % 4][offset + j] = tmp[j];
		// 			tmp[j] = t;
		// 		}
		// 	}
		

		// Matrix4f rot45 = new Matrix4f().setToRotationEuler(new Vec3f(0, (float) Math.toRadians(45), 0));
		// for (int i = 0; i < models[0].length; i++) {
		// 	models[3][i].setMatrix(new Matrix4f().mul(rot45, models[3][i].getMatrix()));
		// }
		// }
		// System.out.println();


	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL3 gl = drawable.getGL().getGL3();

		// set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);
	}

	private void initShaders(GL3 gl) {
		if (!gl.hasGLSL()) {
			System.err.println("No GLSL available!");
			return;
		}

		final ShaderCode vp = ShaderCode.create(gl, GL3.GL_VERTEX_SHADER, this.getClass(), "shader", null, "simple",
				true);
		vp.compile(gl, System.err);

		final ShaderCode fp = ShaderCode.create(gl, GL3.GL_FRAGMENT_SHADER, this.getClass(), "shader", null, "simple",
				true);
		fp.compile(gl, System.err);

		final ShaderProgram sp = new ShaderProgram();
		sp.add(gl, vp, System.err);
		sp.add(gl, fp, System.err);

		st = new ShaderState();
		st.setVerbose(true);
		st.attachShaderProgram(gl, sp, false);
	}

	public void main() {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mousePos = new Vec2f(e.getX(), e.getY());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Vec2f mouseReleasePos = new Vec2f(e.getX(), e.getY());
		Vec2f mouseDiff = new Vec2f().minus(mouseReleasePos, mousePos);
		mousePos = mouseReleasePos;

		Matrix4f rotate = new Matrix4f()
				.setToRotationEuler(new Vec3f(-mouseDiff.y() / 100.f, -mouseDiff.x() / 100.f, 0));
		rotate.mul(cubeRotationNode.getMatrix());

		cubeRotationNode.setMatrix(rotate);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

}
