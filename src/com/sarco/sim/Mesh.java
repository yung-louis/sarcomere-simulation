package com.sarco.sim;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL30.*;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class Mesh {

	private int vao;

	private List<Integer> vboArr;

	private int vertexCount;
	private Texture texture;
	private Vector4f colour = new Vector4f(1, 1, 1, 1);

	public Mesh(float[] positions, float[] texture, float[] normals, int[] indices, int[] joints, float[] weights) {
		// if weights and joints are not set make empty arrays for them
		if (weights == null && joints == null) {
			joints = new int[positions.length * 4 / 3];
			weights = new float[positions.length * 4 / 3];
			for (int i = 0; i < positions.length; i++) {
				joints[i] = 0;
				weights[i] = 0;
			}
		}
		//if normals is null create empty float
		if(normals == null) {
			normals = new float[0];
		}
		// Initialise buffers, they are used as they are more efficient than normal
		FloatBuffer posBuffer = MemoryUtil.memAllocFloat(positions.length);
		posBuffer.put(positions).flip();
		FloatBuffer textureBuffer = MemoryUtil.memAllocFloat(texture.length);
		textureBuffer.put(texture).flip();
		FloatBuffer normalsBuffer = MemoryUtil.memAllocFloat(normals.length);
		normalsBuffer.put(normals).flip();
		FloatBuffer weightsBuffer = MemoryUtil.memAllocFloat(weights.length);
		weightsBuffer.put(weights).flip();
		IntBuffer jointBuffer = MemoryUtil.memAllocInt(joints.length);
		jointBuffer.put(joints).flip();
		IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
		indicesBuffer.put(indices).flip();
		// set the vertex count and initialise vertex buffer object array
		vertexCount = indices.length;
		vboArr = new ArrayList<>();
		// Initialise the vertex array object
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		// Create VBOs
		for (int i = 0; i < 6; i++) {
			int size = 3;
			int vbo = glGenBuffers();
			vboArr.add(vbo);
			//Select the VBO
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			// Add the vbo data based on current data
			if (i == 0) {
				glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
			} else if (i == 1) {
				size = 2;
				glBufferData(GL_ARRAY_BUFFER, textureBuffer, GL_STATIC_DRAW);
			} else if (i == 2) {
				glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
			} else if (i == 3) {
				size = 4;
				glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
			} else {
				size = 4;
				glBufferData(GL_ARRAY_BUFFER, jointBuffer, GL_STATIC_DRAW);
			}
			//Add attributes for 0-4
			if (i != 5) {
				glEnableVertexAttribArray(i);
				if (i == 4) {
					// joint indices are integers and are declared as ivec4 in the shader
					glVertexAttribIPointer(i, size, GL_INT, 0, 0);
				} else {
					glVertexAttribPointer(i, size, GL_FLOAT, false, 0, 0);
				}
			} else {
				// bind the indices
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo);
				glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
			}
		}
		// clear place holders for VBO and VAO
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
		// Empty buffers
		if(posBuffer != null) MemoryUtil.memFree(posBuffer);
		if(textureBuffer != null) MemoryUtil.memFree(textureBuffer);
		if(normalsBuffer != null) MemoryUtil.memFree(normalsBuffer);
		if(weightsBuffer != null) MemoryUtil.memFree(weightsBuffer);
		if(jointBuffer != null) MemoryUtil.memFree(jointBuffer);
		if(indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
	}

	public void render() {
		if (texture != null) {
			// Create place for texture
			glActiveTexture(GL_TEXTURE0);
			// Add the texture
			glBindTexture(GL_TEXTURE_2D, texture.getId());
		}

		// Add the VAO with the mesh
		glBindVertexArray(vao);

		// Render the mesh
		glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

		// Clear the place holders
		glBindVertexArray(0);
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void delete() {
		glDisableVertexAttribArray(0);

		// Delete the VBOs
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		for (int vbo : vboArr) {
			glDeleteBuffers(vbo);
		}

		// Delete the texture
		if (texture != null) {
			texture.delete();
		}

		// Delete the VAO
		glBindVertexArray(0);
		glDeleteVertexArrays(vao);
	}

	// Set the texture with existing texture
	public void setTexture(Texture sTexture) {
		try {
			this.texture = sTexture;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Texture getTexture() {
		return texture;
	}

	// Return true or false is mesh has texture
	public boolean isTextured() {
		if (texture != null) {
			return true;
		} else {
			return false;
		}
	}

	public void setColour(float x, float y, float z, float w) {
		colour.x = x;
		colour.y = y;
		colour.z = z;
		colour.w = w;
	}

	public Vector4f getColour() {
		return colour;
	}
}