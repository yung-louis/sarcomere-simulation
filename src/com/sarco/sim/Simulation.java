package com.sarco.sim;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import static org.lwjgl.opengl.GL30.*;
import com.sarco.sim.utilities.LoadShader;
import com.sarco.sim.utilities.TimeTracker;

import lwjglgamedev.modelLoaders.AnimatedFrame;

import java.util.ArrayList;

public class Simulation implements Runnable {

	private Window window;
	private TimeTracker timer;
	private ShaderProgram sceneShader;
	private ShaderProgram hudShader;
	private Camera camera;

	private boolean contract = false;
	private boolean autoPlay = true;
	private boolean moveActin;
	private boolean actinReturn = false;
	private int speed = 100;
	private boolean vsync = true;
	private int fps = 100;
	private float step = 0;
	private int liveFPS;
	private String quality = "Medium";

	private double mouseX;
	private double mouseY;
	private boolean mouseHold;
	private double lastX;
	private double lastY;

	private ArrayList<SimObject> objects;
	private ArrayList<TextObject> textObjects;
	private ArrayList<Slider> sliderObjects;
	private Quality q = new Quality();

	@Override
	public void run() {
		try {
			init();
			simLoop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		delete();
	}

	public void init() throws Exception {
		// initialising classes
		window = new Window();
		timer = new TimeTracker();
		window.init(vsync);
		camera = new Camera();
		TextMesh textMesh = new TextMesh();
		q = new Quality();

		// Adding input callbacks
		glfwSetKeyCallback(window.getWindow(), keyCallback);
		glfwSetScrollCallback(window.getWindow(), scrollCallback);
		glfwSetCursorPosCallback(window.getWindow(), cursorCallback);
		glfwSetMouseButtonCallback(window.getWindow(), mouseCallback);

		// adding shaders to shaderprograms
		sceneShader = new ShaderProgram(LoadShader.load("/assets/vertex.vs"), LoadShader.load("/assets/fragment.fs"));
		hudShader = new ShaderProgram(LoadShader.load("/assets/hud_vertex.vs"), LoadShader.load("/assets/fragment.fs"));

		// Creating uniforms for the shaders
		sceneShader.createUniform("sceneMatrix");
		sceneShader.createUniform("objectCameraMatrix");
		sceneShader.createUniform("textureSampler");
		sceneShader.createUniform("colour");
		sceneShader.createUniform("textured");
		sceneShader.createUniform("jointsMatrix");
		sceneShader.createUniform("animObj");
		hudShader.createUniform("hudMatrix");
		hudShader.createUniform("colour");

		// setting up camera position
		camera.setRotation(10, 20, 0);
		camera.setPosition(0, 0, 3);
		camera.setScale(0.2f);

		// Calling class that initialises 3D objects
		objects = (ArrayList<SimObject>) CreateSceneObjects.gen();

		// Initialising arrays for the ui objects
		textObjects = (ArrayList<TextObject>) CreateHudObjects.genTexts(textMesh, window);
		sliderObjects = (ArrayList<Slider>) CreateHudObjects.genSliders(textMesh);

		// set view port to screen size
		glViewport(0, 0, window.getWidth(), window.getHeight());
	}

	public void simLoop() throws Exception {
		float timeSince;
		// array to store fps to then get average
		ArrayList<Integer> averageFPS = new ArrayList<>();
		// while the window is open keep the game loop running
		while (!window.shouldClose()) {
			timeSince = timer.getTimeSince();
			// add FPS to average
			averageFPS.add(Math.round(1 / timeSince));

			// gets the average of 10 frames
			if (averageFPS.size() >= 10) {
				int total = 0;
				for (Integer i : averageFPS) {
					total += i;
				}
				liveFPS = total / 10;
				averageFPS = new ArrayList<>();
			}
			// run the loop functions
			update(timeSince);
			render();
			window.update(vsync);
			// if vsync is not true run the wait function
			if (!vsync) {
				delay();
			}
		}
	}

	public void delay() {
		// make the program sleep for as many milliseconds as required to match FPS
		float waitTime = 1f / fps;
		double endTime = timer.getLastLoop() + waitTime;
		while (timer.getSystemTime() < endTime) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void update(float timeSince) {
		// step is the amount of frames of the animation to play
		// if animation is slower than fps step will be added to each frame until it
		// equals 1
		step += timeSince / (1 / (float) speed);
		if (step >= 1) {
			int stepInt = Math.round(step);
			moveActin = false;
			// if the actin is fully contracted during autoplay uncontract
			if (objects.get(3).getPosition().x <= 0.3 && autoPlay) {
				contract = false;
			}

			// if contracting play animation and move actin
			if (objects.get(3).getPosition().x > 0.3 && !actinReturn && contract) {
				objects.forEach(object -> {
					if (object instanceof AnimatedObject) {
						((AnimatedObject) object).nextFrame(stepInt);
						int frame = ((AnimatedObject) object).getFrameInt();
						if (frame >= 32 && frame <= 64) {
							moveActin = true;
						}
					}
				});

				if (moveActin) {
					float amount = (float) (0.058 / 30) * stepInt;
					objects.get(3).movePosition(-amount, 0, 0);
					objects.get(4).movePosition(-amount, 0, 0);
					objects.get(5).movePosition(amount, 0, 0);
					objects.get(6).movePosition(amount, 0, 0);
				}
				// if not contracting and not returning set myosin to releaxed state
			} else if (!actinReturn && !contract) {
				objects.forEach(object -> {
					if (object instanceof AnimatedObject) {
						((AnimatedObject) object).nextFrame(stepInt);
						int frame = ((AnimatedObject) object).getFrameInt();
						if (frame <= 16 || frame >= 128) {
							actinReturn = true;
						}
					}
				});
				// if not contracting and returning move actin back until its at neggining
			} else if (actinReturn || !contract) {
				if (objects.get(3).getPosition().x < 5) {
					float amount = (float) (0.058 / 30) * stepInt;
					objects.get(3).movePosition(amount, 0, 0);
					objects.get(4).movePosition(amount, 0, 0);
					objects.get(5).movePosition(-amount, 0, 0);
					objects.get(6).movePosition(-amount, 0, 0);
					// once fully returned and auto play is true start contracting again
				} else if (autoPlay) {
					contract = true;
					actinReturn = false;
				}

				// return myosin to relaxed state
				objects.forEach(object -> {
					if (object instanceof AnimatedObject) {
						int frame = ((AnimatedObject) object).getFrameInt();
						if (frame > 0) {
							((AnimatedObject) object).nextFrame(-stepInt);
						}
					}
				});
			}
			// reset steps
			step = 0;
			// based on current position of myosin set current process text to the
			// corresponding stage
			int curFrame = ((AnimatedObject) objects.get(20)).getFrameInt();
			if (!contract) {
				textObjects.get(2).setText("Current Process: No calcium is present so actin binding sites are covered");
			} else {
				if (curFrame >= 0 && curFrame < 16) {
					textObjects.get(2).setText("Current Process: Myosin heads are resting with ADP + P attached");
				}
				if (curFrame >= 16 && curFrame < 32) {
					textObjects.get(2).setText("Current Process: Myosin heads attach to actin binding site");
				}
				if (curFrame >= 32 && curFrame < 64) {
					textObjects.get(2).setText("Current Process: ADP + P Unbind causing powerstroke");
				}
				if (curFrame >= 64 && curFrame < 110) {
					textObjects.get(2).setText("Current Process: ATP binds causing head to release");
				}
				if (curFrame >= 110 && curFrame < 128) {
					textObjects.get(2).setText("Current Process: ATP breaks down into ADP + P");
				}
			}
		}

		// next few if statements update text to match values
		if (autoPlay) {
			textObjects.get(0).setText("AutoPlay:On");
		} else {
			textObjects.get(0).setText("AutoPlay:Off");
		}
		textObjects.get(1).setText("FPS: " + liveFPS);
		textObjects.get(15).setText("vsync: " + vsync);
		if (contract) {
			textObjects.get(3).setText("Contracting:On");
		} else {
			textObjects.get(3).setText("Contracting:Off");
		}

		textObjects.get(13).setText("Speed:" + speed);
		textObjects.get(14).setText("Quality:" + quality);
		textObjects.get(16).setText("Target FPS:" + fps);

		// update object colours to match sliders
		for (SimObject object : objects) {
			if (object instanceof AnimatedObject) {
				Vector3f i = valueToColour(sliderObjects.get(0).value);
				int j = (int) sliderObjects.get(1).value;
				object.getMesh().setColour(i.x, i.y, i.z, (float) j / 100);
			}
		}
		Vector3f i = valueToColour(sliderObjects.get(2).value);
		int j = (int) sliderObjects.get(3).value;
		objects.get(3).getMesh().setColour(i.x, i.y, i.z, (float) j / 100);
		objects.get(4).getMesh().setColour(i.x, i.y, i.z, (float) j / 100);
		objects.get(5).getMesh().setColour(i.x, i.y, i.z, (float) j / 100);
		objects.get(6).getMesh().setColour(i.x, i.y, i.z, (float) j / 100);

		// update values to match sliders
		speed = Math.round(sliderObjects.get(4).value * 5);
		fps = Math.round(sliderObjects.get(5).value * 2 + 20);
	}

	public void render() throws Exception {
		// clear window
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glClearColor(0.4f, 0.4f, 0.4f, 1.0f);
		// fix positions of hud items and viewport when window is resized
		if (window.isResized()) {
			textObjects.get(1).setPosition(window.getWidth() - 190f, window.getHeight() - 40f, 1);
			textObjects.get(2).setPosition(10f, window.getHeight() - 60f, 1);
			textObjects.get(3).setPosition(window.getWidth() - 300f, 5f, 1);
			glViewport(0, 0, window.getWidth(), window.getHeight());
			window.setResized(false);
		}
		sceneShader.open();

		// set projection matrices
		Matrix4f sceneMatrix = Transform.getSceneMatrix(window);
		sceneShader.setUniform("sceneMatrix", sceneMatrix);

		sceneShader.setUniform("textureSampler", 0);

		// set camera matrix
		Matrix4f cameraMatrix = Transform.getCameraMatrix(camera);

		// render objects with corresponding uniforms
		for (SimObject object : objects) {

			// set transformation matrix
			Matrix4f objectCameraMatrix = Transform.getObjectCameraMatrix(object, cameraMatrix);
			sceneShader.setUniform("objectCameraMatrix", objectCameraMatrix);
			sceneShader.setUniform("colour", object.getMesh().getColour());
			sceneShader.setUniform("textured", object.getMesh().isTextured() ? 0 : 1);
			sceneShader.setUniform("animObj", 0);

			if (object instanceof AnimatedObject) {
				AnimatedObject animObject = (AnimatedObject) object;
				AnimatedFrame frame = animObject.getFrame();
				sceneShader.setUniform("jointsMatrix", frame.getJointMatrices());
				sceneShader.setUniform("animObj", 1);
			}

			object.getMesh().render();
		}
		sceneShader.close();

		hudShader.open();

		// render hud objects with corresponding uniforms
		Matrix4f hud = Transform.getHudProjectionMatrix(window);
		for (TextObject object1 : textObjects) {
			for (SimObject object : object1.getLetter()) {
				Mesh mesh = object.getMesh();
				// Set matrices
				Matrix4f hudMatrix = Transform.getHudProjTextMatrix(object, hud);
				hudShader.setUniform("hudMatrix", hudMatrix);
				hudShader.setUniform("colour", object.getMesh().getColour());
				// Render the mesh for this HUD item
				mesh.render();
			}
		}
		for (Slider slide : sliderObjects) {
			for (SimObject object : slide.getPicker().getLetter()) {
				Mesh mesh = object.getMesh();
				// Set matrices
				Matrix4f hudMatrix = Transform.getHudProjTextMatrix(object, hud);
				hudShader.setUniform("hudMatrix", hudMatrix);
				hudShader.setUniform("colour", object.getMesh().getColour());
				// Render the mesh for this HUD item
				mesh.render();
			}
			for (SimObject object : slide.getBar().getLetter()) {
				Mesh mesh = object.getMesh();
				// Set matrices
				Matrix4f hudMatrix = Transform.getHudProjTextMatrix(object, hud);
				hudShader.setUniform("hudMatrix", hudMatrix);
				hudShader.setUniform("colour", object.getMesh().getColour());
				// Render the mesh for this HUD item
				mesh.render();
			}
		}

		hudShader.close();

		// if qualitys dont match update quality to new quality
		if (!quality.equals(q.getQuality())) {
			q.set(quality, objects);
		}

	}

	public void delete() {
		// Delete objects
		window.delete();
		sceneShader.delete();
		objects.forEach(object -> object.getMesh().delete());
		textObjects.forEach(object -> {
			for (SimObject obj : object.getLetter()) {
				if (obj.getMesh() != null) {
					obj.getMesh().delete();
				}
			}
		});
	}

	private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
		@Override
		public void invoke(long window, int key, int scancode, int action, int mods) {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
				glfwSetWindowShouldClose(window, true);
			}
			if (key == GLFW_KEY_RIGHT && (action == GLFW_REPEAT || action == GLFW_PRESS)) {
				camera.movePosition(0.1f, 0, 0);
			}
			if (key == GLFW_KEY_LEFT && (action == GLFW_REPEAT || action == GLFW_PRESS)) {
				camera.movePosition(-0.1f, 0, 0);
			}
			if (key == GLFW_KEY_UP && (action == GLFW_REPEAT || action == GLFW_PRESS)) {
				camera.movePosition(0, 0.01f, 0);
			}
			if (key == GLFW_KEY_DOWN && (action == GLFW_REPEAT || action == GLFW_PRESS)) {
				camera.movePosition(0, -0.01f, 0);
			}
			if (key == GLFW_KEY_C && action == GLFW_PRESS) {
				camera.setRotation(0, 0, 0);
				camera.setPosition(0, 0, 3);
				camera.setScale(0.2f);
			}
			if (key == GLFW_KEY_SPACE && !autoPlay && (action == GLFW_PRESS || action == GLFW_RELEASE)) {
				// contract the sarcomere
				contract = !contract;
				if (actinReturn) {
					actinReturn = false;
				}
			}
			if (key == GLFW_KEY_A && action == GLFW_PRESS) {
				// toggle autoplay
				autoPlay = !autoPlay;
				contract = true;
				if (actinReturn) {
					actinReturn = false;
				}
				if (!autoPlay) {
					actinReturn = true;
					contract = false;
				}
			}
		}
	};

	private GLFWMouseButtonCallback mouseCallback = new GLFWMouseButtonCallback() {
		@Override
		public void invoke(long window, int button, int action, int mods) {
			//set mouse hold
			if (button == GLFW_MOUSE_BUTTON_LEFT && GLFW_PRESS == action) {
				mouseHold = true;
				lastX = mouseX;
				lastY = mouseY;
			}
			//set mout release
			if (button == GLFW_MOUSE_BUTTON_LEFT && GLFW_RELEASE == action) {
				mouseHold = false;
				lastX = mouseX;
				lastY = mouseY;
			}
			if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
				// if mouse clickers slider
				for (Slider obj : sliderObjects) {
					// move slider to mouse
					if (obj.getBar().isMouseOver(mouseX, mouseY)) {
						obj.pickerClick((int) mouseX);
					}
				}
				for (TextObject obj : textObjects) {
					if (obj.isMouseOver(mouseX, mouseY)) {
						mouseHold = false;
						// toggle autoplay
						if (obj.getText().contains("AutoPlay:")) {
							autoPlay = !autoPlay;
							contract = true;
							if (actinReturn) {
								actinReturn = false;
							}
							if (!autoPlay) {
								actinReturn = true;
								contract = false;
							}
						}
						// toggle vsync
						if (obj.getText().contains("vsync:") || obj.getText().equals("vsync:")) {
							vsync = !vsync;
						}
						// re centre camera
						if (obj.getText().equals("Re-Centre Camera")) {
							camera.setRotation(0, 0, 0);
							camera.setPosition(0, 0, 3);
							camera.setScale(0.2f);
						}
						// toggle quality
						if (obj.getText().contains("Quality")) {
							if (obj.getText().contains("Low")) {
								quality = "Medium";
							} else if (obj.getText().contains("Medium")) {
								quality = "High";
							} else {
								quality = "Low";
							}
						}
						if (obj.getText().equals("Appearance \21")) { // open appearance menu
							for (int i = 8; i < 12; i++) {
								textObjects.get(i).toggleVis();
							}
							for (int i = 0; i < 4; i++) {
								sliderObjects.get(i).toggleVis();
							}
							textObjects.get(5).moveDown(8);
							textObjects.get(6).moveDown(8);
							textObjects.get(7).moveDown(8);
							for (int i = 12; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(8);
							}
							for (int i = 4; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(8);
							}
							obj.setText("Appearance \20");
						} else if (obj.getText().equals("Appearance \20")) { // close appearance menu
							for (int i = 8; i < 12; i++) {
								textObjects.get(i).toggleVis();
							}
							for (int i = 0; i < 4; i++) {
								sliderObjects.get(i).toggleVis();
							}
							textObjects.get(5).moveDown(-8);
							textObjects.get(6).moveDown(-8);
							textObjects.get(7).moveDown(-8);
							for (int i = 12; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(-8);
							}
							for (int i = 4; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(-8);
							}
							obj.setText("Appearance \21");
						}
						if (obj.getText().equals("View \21")) { // open view menu
							for (int i = 12; i < 14; i++) {
								textObjects.get(i).toggleVis();
							}
							sliderObjects.get(4).toggleVis();
							textObjects.get(6).moveDown(3);
							textObjects.get(7).moveDown(3);
							for (int i = 14; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(3);
							}
							for (int i = 5; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(3);
							}
							obj.setText("View \20");
						} else if (obj.getText().equals("View \20")) {// close view menu
							for (int i = 12; i < 14; i++) {
								textObjects.get(i).toggleVis();
							}
							sliderObjects.get(4).toggleVis();
							textObjects.get(6).moveDown(-3);
							textObjects.get(7).moveDown(-3);
							for (int i = 14; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(-3);
							}
							for (int i = 5; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(-3);
							}
							obj.setText("View \21");
						}
						if (obj.getText().equals("Performance \21")) { // open performance menu
							for (int i = 14; i < 17; i++) {
								textObjects.get(i).toggleVis();
							}
							sliderObjects.get(5).toggleVis();
							textObjects.get(7).moveDown(4);
							for (int i = 17; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(4);
							}
							for (int i = 6; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(4);
							}
							obj.setText("Performance \20");
						} else if (obj.getText().equals("Performance \20")) { // close performance menu
							for (int i = 14; i < 17; i++) {
								textObjects.get(i).toggleVis();
							}
							sliderObjects.get(5).toggleVis();
							textObjects.get(7).moveDown(-4);
							for (int i = 17; i < textObjects.size(); i++) {
								textObjects.get(i).moveDown(-4);
							}
							for (int i = 6; i < sliderObjects.size(); i++) {
								sliderObjects.get(i).moveDown(-4);
							}
							obj.setText("Performance \21");
						}
						if (obj.getText().equals("Controls \21")) { // open controls menu
							for (int i = 17; i < 23; i++) {
								textObjects.get(i).toggleVis();
							}
							obj.setText("Controls \20");
						} else if (obj.getText().equals("Controls \20")) { // close controls menu
							for (int i = 17; i < 23; i++) {
								textObjects.get(i).toggleVis();
							}
							obj.setText("Controls \21");
						}
					}
				}
			}
		}
	};

	private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {

		@Override
		public void invoke(long window, double xoffset, double yoffset) {
			camera.camZoom((float) yoffset / 5);
		}
	};

	public Vector3f valueToColour(float value) {
		// convert slider colour to rgb value
		Vector3f col = new Vector3f(0, 0, 0);
		if (value < (100 / 6)) {
			col.z = 1;
			col.y = (float) (((100 / (double) 6) - value) / 100 * 6);
		}
		if (value >= (100 / 6) && value < ((100 / 6) * 2)) {
			col.x = (float) ((value - (100 / (double) 6)) / 100 * 6);
			col.z = 1;
		}
		if (value >= ((100 / 6) * 2) && value < ((100 / 6) * 3)) {
			col.x = 1;
			col.z = (float) ((((100 / (double) 6) * 3) - value) / 100 * 6);
		}
		if (value >= ((100 / 6) * 3) && value < ((100 / 6) * 4)) {
			col.y = (float) ((value - ((100 / (double) 6) * 3)) / 100 * 6);
			col.x = 1;
		}
		if (value >= ((100 / 6) * 4) && value < ((100 / 6) * 5)) {
			col.y = 1;
			col.x = (float) ((((100 / (double) 6) * 5) - value) / 100 * 6);
		}
		if (value >= ((100 / 6) * 5)) {
			col.z = (float) ((value - ((100 / (double) 6) * 5)) / 100 * 6);
			col.y = 1;
		}

		return col;
	}

	private GLFWCursorPosCallback cursorCallback = new GLFWCursorPosCallback() {
		boolean mouseOnSlider = false;

		@Override
		public void invoke(long window, double xpos, double ypos) {
			mouseX = xpos;
			mouseY = ypos;
			float mouseMoveX = ((float) lastY - (float) mouseY);
			mouseMoveX *= -1;
			mouseMoveX *= 0.5f;
			float mouseMoveY = ((float) lastX - (float) mouseX);
			mouseMoveY *= -1;
			mouseMoveY *= 0.5f;
			// if mouse held
			if (mouseHold) {
				// if mouse on slider move slider
				for (Slider obj : sliderObjects) {
					if (obj.getBar().isMouseOver(mouseX, mouseY)) {
						obj.pickerClick((int) mouseX);
						mouseOnSlider = true;
					}
				}
				// if mouse not on slider move camera
				if (!mouseOnSlider) {
					camera.moveRotation(mouseMoveX, mouseMoveY, 0);
					lastX = mouseX;
					lastY = mouseY;
				}
			} else {
				mouseOnSlider = false;
			}
		}

	};

}
