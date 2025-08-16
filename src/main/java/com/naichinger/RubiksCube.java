package com.naichinger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RubiksCube extends SceneGraphNode {

	private final int N;
	private final SceneGraphNode rootNode;
	private final TransformationNode[][][] cube;
	private Matrix4f modelMatrix;

	public RubiksCube(int n, ShaderState st, int[] vbID) {
		super(st);

		N = n;
		rootNode = new SceneGraphNode(st);

		Matrix4f scaleMatrix = new Matrix4f().setToScale(0.15f, 0.15f, 0.15f);
		Matrix4f translateMatrix = new Matrix4f().setToTranslation(new Vec3f(0, 0, 0.1f));

		modelMatrix= new Matrix4f().mul(translateMatrix, scaleMatrix);

		int N = 3;
		int HALF_N = N / 2;
		cube = new TransformationNode[N][N][N];

		Vec3f greyColorVector = new Vec3f(0.2f, 0.2f, 0.2f);

		for (int y = -HALF_N; y <= HALF_N; y++) {
			for (int x = -HALF_N; x <= HALF_N; x++) {
				for (int z = -HALF_N; z <= HALF_N; z++) {

					TransformationNode curCube = new TransformationNode(st);
					curCube.setMatrix(new Matrix4f().setToTranslation(new Vec3f(x * 2.05f, y * 2.05f, z * 2.05f)));
					rootNode.addChild(curCube);

					cube[x + HALF_N][y + HALF_N][z + HALF_N] = curCube;

					for (int i = -1; i <= 1; i += 2) {
						// z surfaces
						Vec3f zColor;
						if (i * HALF_N == z)
							zColor = new Vec3f(0, (i + 1) / 2, (-i + 1) / 2);
						else
							zColor = greyColorVector;

						ModelNode zSurface = new ModelNode(st, vbID, zColor);
						TransformationNode zTranslate = new TransformationNode(st);
						zTranslate.setMatrix(new Matrix4f().setToTranslation(0, 0, i));

						zTranslate.addChild(zSurface);
						curCube.addChild(zTranslate);

						// y surfaces
						Vec3f yColor;
						if (i * HALF_N == y)
							yColor = new Vec3f(1, 1, (i + 1) / 2);
						else
							yColor = greyColorVector;

						ModelNode ySurface = new ModelNode(st, vbID, yColor);
						TransformationNode yTranslate = new TransformationNode(st);
						Matrix4f yTranslateMatrix = new Matrix4f().setToTranslation(0, i, 0);
						Matrix4f yRotateMatrix = new Matrix4f().setToRotationAxis((float) Math.toRadians(90),
								new Vec3f(1, 0, 0));
						yTranslate.setMatrix(new Matrix4f().mul(yTranslateMatrix, yRotateMatrix));
						yTranslate.addChild(ySurface);
						curCube.addChild(yTranslate);

						// x surfaces
						Vec3f xColor;
						if (i * HALF_N == x)
							xColor = new Vec3f(1, 0.5f * (i + 1) / 2, 0);
						else
							xColor = greyColorVector;

						ModelNode xSurface = new ModelNode(st, vbID, xColor);

						TransformationNode xTranslate = new TransformationNode(st);
						Matrix4f xTranslateMatrix = new Matrix4f().setToTranslation(i, 0, 0);
						Matrix4f xRotateMatrix = new Matrix4f().setToRotationAxis((float) Math.toRadians(90),
								new Vec3f(0, 1, 0));
						xTranslate.setMatrix(new Matrix4f().mul(xTranslateMatrix, xRotateMatrix));

						xTranslate.addChild(xSurface);
						curCube.addChild(xTranslate);
					}
				}
			}
		}
		
		rotateArrZ0();
	}

	public void rotateArrZ0() {
		
		Matrix4f rot = new Matrix4f().setToRotationEuler(new Vec3f(0, 0, (float) Math.toRadians(-90)));
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				cube[i][j][0].setMatrix(new Matrix4f().mul(rot, cube[i][j][0].getMatrix()));
			}
		}

		for (int j = 0; j < N / 2; j++) {
			for (int i = j; i < N - 1 - j; i++) {
				TransformationNode t = cube[j][i][0];
				cube[j][i][0] = cube[N - i - 1][j][0];
				cube[N - i - 1][j][0] = cube[N - 1 - j][N - i - 1][0];
				cube[N - 1 - j][N - i - 1][0] = cube[i][N - 1 - j][0];
				cube[i][N - 1 - j][0] = t;
			}
		}
	}

	@Override
    public void render(GL3 gl, Matrix4f model, Matrix4f view) {
		Matrix4f transformedModelMatrix = new Matrix4f().mul(model, modelMatrix);
		rootNode.render(gl, transformedModelMatrix, view);
        super.render(gl, transformedModelMatrix, view);
    }

	public Matrix4f getModelMatrix() {
		return modelMatrix;
	}

	public void setModelMatrix(Matrix4f rootNode) {
		this.modelMatrix = rootNode;
	}

}
