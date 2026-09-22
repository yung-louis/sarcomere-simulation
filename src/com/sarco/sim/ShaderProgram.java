package com.sarco.sim;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.system.MemoryStack;

public class ShaderProgram {

    private int program;

    private Map<String, Integer> uniforms  = new HashMap<>();

    public ShaderProgram(String vertexShader, String fragShader) {
    	// get id for program
        program = glCreateProgram();
        
        // add vertex shader
        int vShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vShader, vertexShader);
        glCompileShader(vShader);
        checkShader(vShader, "vertex");
        glAttachShader(program, vShader);

        // add fragment shader
        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fShader, fragShader);
        glCompileShader(fShader);
        checkShader(fShader, "fragment");
        glAttachShader(program, fShader);
        
        // link fragment and vertex shader to program
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("Shader program failed to link: " + glGetProgramInfoLog(program));
        }
        glDetachShader(program, vShader);
        glDetachShader(program, fShader);
        glValidateProgram(program);
    }
    
    private static void checkShader(int shader, String type) {
        // report compile errors instead of failing silently
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(type + " shader failed to compile: " + glGetShaderInfoLog(shader));
        }
    }

    public void createUniform(String uniform){
    	//create uniform and add to hashmap
        uniforms.put(uniform, glGetUniformLocation(program, uniform));
    }
    
    public void setUniform(String uniform, Matrix4f[] matrices) {
    	// Add matrices to to uniform
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int length = matrices != null ? matrices.length : 0;
            FloatBuffer fb = stack.mallocFloat(16 * length); 
            for (int i = 0; i < length; i++) {
                matrices[i].get(16 * i, fb);
            }
            glUniformMatrix4fv(uniforms.get(uniform), false, fb);
        }
    }

    public void setUniform(String uniform, Matrix4f value) {
    	// add matrix to uniform
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(uniforms.get(uniform), false, value.get(stack.mallocFloat(16)));
        }
    }
    
    public void setUniform(String uniform, Vector4f value) {
    	// add vector 4 to uniform
        glUniform4f(uniforms.get(uniform), value.x, value.y, value.z, value.w);
    }
    
    public void setUniform(String uniform, int value) {
    	// add int to uniform
        glUniform1i(uniforms.get(uniform), value);
    }
    
    public void open() {
        glUseProgram(program);
    }

    public void close() {
        glUseProgram(0);
    }

    public void delete() {
        close();
        if (program != 0) {
            glDeleteProgram(program);
        }
    }
}