package com.naichinger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RubiksCube extends SceneGraphNode {

	private final int N;
	private final RubiksCubeElement[][][] cube;

	private boolean rotationOngoing = false;
	private int rotationAxis = -1;
	private int rotationIndex = -1;
	private float rotationTimePassed = 0;
	private float rotationDuration = 500_000_000f; // 0.5s

	private record RubiksCubeElement(TransformationNode node, TransformationNode ongoingRotationNode) {
	}

	public RubiksCube(int n, ShaderState st, int[] vbID) {
		super(st);

		N = n;
		int HALF_N = N / 2;

		cube = new RubiksCubeElement[N][N][N];

		Vec3f greyColorVector = new Vec3f(0.2f, 0.2f, 0.2f);

		for (int y = -HALF_N; y <= HALF_N; y++) {
			for (int x = -HALF_N; x <= HALF_N; x++) {
				for (int z = -HALF_N; z <= HALF_N; z++) {
					TransformationNode ongoingRotationNode = new TransformationNode(st);
					TransformationNode cubeNode = new TransformationNode(st);

					cubeNode.setMatrix(new Matrix4f().setToTranslation(new Vec3f(x * 2.05f, y * 2.05f, z * 2.05f)));
					this.addChild(ongoingRotationNode);
					ongoingRotationNode.addChild(cubeNode);

					cube[x + HALF_N][y + HALF_N][z + HALF_N] = new RubiksCubeElement(cubeNode, ongoingRotationNode);

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
						cubeNode.addChild(zTranslate);

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
						cubeNode.addChild(yTranslate);

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
						cubeNode.addChild(xTranslate);
					}
				}
			}
		}
		// rotateArrZ0();
	}

	public void startRotation(int axis, int idx) {
		if (rotationOngoing)
			return;
		if (axis < 0 || axis >= 3) {
			return;
		}
		if (idx < 0 || idx >= N) {
			return;
		}
		rotationTimePassed = 0;
		rotationAxis = axis;
		rotationIndex = idx;
		rotationOngoing = true;
	}

	private void finishRotation() {
		if (!rotationOngoing) {
			return;
		}
		Vec3f rotationVector = new Vec3f();
		rotationVector.set(rotationAxis, (float) Math.toRadians(-90));
		Matrix4f rot = new Matrix4f().setToRotationEuler(rotationVector);
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				int x, y, z;
				if (rotationAxis == 0) {
					x = rotationIndex;
					y = i;
					z = j;
				} else if (rotationAxis == 1) {
					x = i;
					y = rotationIndex;
					z = j;
				} else {
					x = i;
					y = j;
					z = rotationIndex;
				}
				cube[x][y][z].ongoingRotationNode().setMatrix(new Matrix4f());
				cube[x][y][z].node().setMatrix(new Matrix4f().mul(rot, cube[x][y][z].node().getMatrix()));
			}
		}
		switch (rotationAxis) {
			case 0:
				rotateCubeArrayX(rotationIndex);
				break;
			case 1:
				rotateCubeArrayY(rotationIndex);
				break;
			case 2:
				rotateCubeArrayZ(rotationIndex);
				break;
		}
		rotationOngoing = false;
	}

	public void updateCube(float timeStepNano) {
		if (!rotationOngoing)
			return;

		rotationTimePassed += timeStepNano;
		if (rotationTimePassed >= rotationDuration) {
			finishRotation();
			return;
		}

		Vec3f rotationVector = new Vec3f();
		rotationVector.set(rotationAxis, 1f);
		float rotationProgress = timeStepNano / rotationDuration;
		float rotationStep = (float) Math.toRadians(-90) * rotationProgress;
		Matrix4f rotationMatrix = new Matrix4f().setToRotationEuler(rotationVector.mul(rotationStep));

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				int x, y, z;
				if (rotationAxis == 0) {
					x = rotationIndex;
					y = i;
					z = j;
				} else if (rotationAxis == 1) {
					x = i;
					y = rotationIndex;
					z = j;
				} else {
					x = i;
					y = j;
					z = rotationIndex;
				}
				cube[x][y][z].ongoingRotationNode()
						.setMatrix(new Matrix4f()
						.mul(rotationMatrix, cube[x][y][z].ongoingRotationNode().getMatrix()));
			}
		}
	}

	private void rotateCubeArrayX(int idx) {
		for (int j = 0; j < N / 2; j++) {
			for (int i = j; i < N - 1 - j; i++) {
				var t = cube[idx][j][i];
				cube[idx][j][i] = cube[idx][N - i - 1][j];
				cube[idx][N - i - 1][j] = cube[idx][N - 1 - j][N - i - 1];
				cube[idx][N - 1 - j][N - i - 1] = cube[idx][i][N - 1 - j];
				cube[idx][i][N - 1 - j] = t;
			}
		}
	}

	private void rotateCubeArrayY(int idx) {
		for (int j = 0; j < N / 2; j++) {
			for (int i = j; i < N - 1 - j; i++) {
				var t = cube[i][idx][j];
				cube[i][idx][j] = cube[j][idx][N - i - 1];
				cube[j][idx][N - i - 1] = cube[N - i - 1][idx][N - 1 - j];
				cube[N - i - 1][idx][N - 1 - j] = cube[N - 1 - j][idx][i];
				cube[N - 1 - j][idx][i] = t;
			}
		}
	}

	private void rotateCubeArrayZ(int idx) {
		for (int j = 0; j < N / 2; j++) {
			for (int i = j; i < N - 1 - j; i++) {
				var t = cube[j][i][idx];
				cube[j][i][idx] = cube[N - i - 1][j][idx];
				cube[N - i - 1][j][idx] = cube[N - 1 - j][N - i - 1][idx];
				cube[N - 1 - j][N - i - 1][idx] = cube[i][N - 1 - j][idx];
				cube[i][N - 1 - j][idx] = t;
			}
		}
	}
}