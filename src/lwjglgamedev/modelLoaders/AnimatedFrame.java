package lwjglgamedev.modelLoaders;

import java.util.Arrays;
import org.joml.Matrix4f;

public class AnimatedFrame {
	
	// this class is from https://github.com/lwjglgamedev/lwjglbook

    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f();
            
    private final Matrix4f[] localJointMatrices;

    private final Matrix4f[] jointMatrices;

    // Size the arrays to the number of joints the model actually uses, so the
    // count uploaded matches the jointsMatrix[4] uniform in the vertex shader
    public AnimatedFrame(int numJoints) {
        localJointMatrices = new Matrix4f[numJoints];
        Arrays.fill(localJointMatrices, IDENTITY_MATRIX);

        jointMatrices = new Matrix4f[numJoints];
        Arrays.fill(jointMatrices, IDENTITY_MATRIX);
    }
    
    public Matrix4f[] getLocalJointMatrices() {
        return localJointMatrices;
    }

    public Matrix4f[] getJointMatrices() {
        return jointMatrices;
    }

    public void setMatrix(int pos, Matrix4f localJointMatrix, Matrix4f invJointMatrix) {
        localJointMatrices[pos] = localJointMatrix;
        Matrix4f mat = new Matrix4f(localJointMatrix);
        mat.mul(invJointMatrix);
        jointMatrices[pos] = mat;
    }
}
