package com.naichinger;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.nio.FloatBuffer;

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

public class BasicFrame extends JFrame implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {
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

	RubiksCube cube;

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
		canvas.addKeyListener(this);
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

	float currRotation = 0;

	// called repeatedly
	@Override
	public void display(GLAutoDrawable drawable) {
		// get the OpenGL 3.x graphics context (needs to align with the chosen
		// GLProfile)
		GL3 gl = drawable.getGL().getGL3();

		// clear the frame buffer
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		st.useProgram(gl, true);

		// float rotationIncrement = .01f;

		// if (false) {
		// float nintyDegreeRad = (float) Math.toRadians(90);

		// if (nintyDegreeRad - currRotation < rotationIncrement) {
		// rotationIncrement = nintyDegreeRad - currRotation;
		// }

		// Matrix4f rot = new Matrix4f().setToRotationEuler(new Vec3f(0,
		// rotationIncrement, 0));
		// Matrix4f rotNeg = new Matrix4f().setToRotationEuler(new Vec3f(0,
		// -rotationIncrement, 0));

		// for (int i = 0; i < 5; i++) {
		// for (int j = 0; j < 5; j++) {
		// cube[i][0][j].setMatrix(new Matrix4f().mul(rot, cube[i][0][j].getMatrix()));
		// }
		// }
		// }
		// currRotation += rotationIncrement;

		cube.render(gl, new Matrix4f(), new Matrix4f());

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

		cube = new RubiksCube(3, st, vbID);
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

	@Override
	public void mouseDragged(MouseEvent e) {
		Vec2f mouseReleasePos = new Vec2f(e.getX(), e.getY());
		Vec2f mouseDiff = new Vec2f().minus(mouseReleasePos, mousePos);
		mousePos = mouseReleasePos;

		Matrix4f rotate = new Matrix4f()
				.setToRotationEuler(new Vec3f(-mouseDiff.y() / 100.f, -mouseDiff.x() / 100.f, 0));
		rotate.mul(cube.getModelMatrix());

		cube.setModelMatrix(rotate);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_1) {
			System.out.println("1");
		} else if (e.getKeyCode() == KeyEvent.VK_2) {
			System.out.println("2");

		} else if (e.getKeyCode() == KeyEvent.VK_3) {
			System.out.println("3");

		} else if (e.getKeyCode() == KeyEvent.VK_4) {
			System.out.println("4");

		} else if (e.getKeyCode() == KeyEvent.VK_5) {
			System.out.println("5");

		} else if (e.getKeyCode() == KeyEvent.VK_6) {
			System.out.println("6");

		}
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
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}
}
