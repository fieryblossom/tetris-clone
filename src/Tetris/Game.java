package Tetris;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    private long window;

    private int vertexArrayID;
    private int programID;

    private int vertexBuffer;
    private FloatBuffer buffer;

    private int vertices;

    private GLFWErrorCallback errorCallback;

    private boolean running;
    private boolean restart;

    private final double targetTime = 1000 / 60;

    private final double movementFreq = 500;
    private double lastMovement;
    private double movementLag = 0;

    private Block[][] matrix;
    private Piece piece;

    private static final float scale = 0.09f; //static?
    private static final float translateX = -0.45f;
    private static final float translateY = -0.9f;
    private static final float border = 0.005f;

    private static final float[] grey = {0.3f, 0.3f, 0.3f};

    public Game(){
        init();
        do {
            setup();
            //start screen
            loop();
            //game over screen
        } while(restart);
        dispose();
    }
    private void setup(){
        if(matrix == null) {
            matrix = new Block[20][10];
            for (int i = 0; i < 20; i++)
                for(int j = 0; j < 10; j++) {
                    matrix[i][j] = new Block();
                }
        } else
            for (int i = 0; i < 20; i++)
                for(int j = 0; j < 10; j++)
                    matrix[i][j].show = false;

        if(piece == null) piece = new Piece();

        piece.set((byte) ThreadLocalRandom.current().nextInt(0, 6 + 1));

        vertices = 0;

        running = true;
        restart = false;
    }
    private void init(){
        errorCallback = GLFWErrorCallback.createPrint();
        glfwSetErrorCallback(errorCallback);

        if(!glfwInit()){
            throw new IllegalStateException("Unable to initialize GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(600, 600, "Tetris", NULL, NULL);
        if(window==NULL){
            glfwTerminate();
            throw new RuntimeException("Failed to create the GLFW window!");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glClearColor(0f, 0f, 0f, 0f);

        vertexArrayID = glGenVertexArrays();
        glBindVertexArray(vertexArrayID);

        programID = Shader.load("VertexShader.glsl", "FragmentShader.glsl");

        buffer = MemoryUtil.memAllocFloat(4096*2);

        vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5*Float.BYTES, 0);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 5*Float.BYTES, 2*Float.BYTES);

        glfwSetKeyCallback(window, GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if(key == GLFW_KEY_LEFT && action == GLFW_PRESS) piece.moveLeft(matrix);
            else if(key == GLFW_KEY_RIGHT && action == GLFW_PRESS) piece.moveRight(matrix);
            else if(key == GLFW_KEY_UP && action == GLFW_PRESS) piece.rotate();
            else if(key == GLFW_KEY_DOWN && action == GLFW_PRESS) piece.hardDrop(matrix);
        }));

        running = true;
    }
    private void loop(){
        try{Thread.sleep(1000);} catch (InterruptedException ignored){} //prep time

        lastMovement = System.currentTimeMillis();

        while(running){
            double start = System.currentTimeMillis();

            //update();
            //if (piece.checkOverlap(matrix)) running = false;

            putGrid();
            //putPiece();
            putMatrix();
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if(glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS || glfwWindowShouldClose(window))
                running = false;

            double now = System.currentTimeMillis();
            double elapsed = now - start;
            Game.wait(targetTime - elapsed, now);
        }
    }
    private void dispose(){
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glDeleteBuffers(vertexBuffer);
        glDeleteProgram(programID);
        glDeleteVertexArrays(vertexArrayID);

        glfwTerminate();
    }
    private void update(){
        double now = System.currentTimeMillis();
        movementLag = now - lastMovement;

        if(movementLag >= movementFreq){
            lastMovement = now;
            if(piece.stop(matrix)) {
                piece.place(matrix);
                piece.set((byte) ThreadLocalRandom.current().nextInt(0, 6 + 1));
            } else
                piece.moveDown();
        }

        purge();
    }
    private void purge(){
        for(byte i=0; i<20; i++)
            while(filled(i))
                removeLine(i);
    }
    private boolean filled(int i){
        for(int j = 0; j < 10; j++)
            if(!matrix[i][j].show) return false;
        return true;
    }
    private void removeLine(int i){
        Block[] temp = matrix[i];
        for(int k = i; k < 19; k++)
            matrix[i] = matrix[i+1];
        matrix[19] = temp;

        for(int j = 0; j < 10; j++)
            matrix[19][i].show = false;
    }
    private void putMatrix(){
        for(int y = 0; y < 20; y++)
            for(int x = 0; x < 10; x++){
                if(matrix[y][x].show) {
                    buffer.put(x * scale + translateX + border).put(y * scale + translateY + border)
                            .put(matrix[y][x].colorVec);
                    buffer.put((x + 1) * scale + translateX - border).put(y * scale + translateY + border)
                            .put(matrix[y][x].colorVec);
                    buffer.put((x + 1) * scale + translateX - border).put((y + 1) * scale + translateY - border)
                            .put(matrix[y][x].colorVec);

                    buffer.put(x * scale + translateX + border).put(y * scale + translateY + border)
                            .put(matrix[y][x].colorVec);
                    buffer.put((x + 1) * scale + translateX - border).put((y + 1) * scale + translateY - border)
                            .put(matrix[y][x].colorVec);
                    buffer.put(x * scale + translateX + border).put((y + 1) * scale + translateY - border)
                            .put(matrix[y][x].colorVec);

                    vertices += 6;
                }
            }
    }
    private void putPiece(){
        //figure this out...
        vertices += 24;
    }
    private void putGrid(){
        for(float x = 0f; x <= 0.99f; x += 0.09f) {
            buffer.put(-0.4525f + x).put(-0.9025f).put(grey);
            buffer.put(-0.4475f + x).put(-0.9025f).put(grey);
            buffer.put(-0.4475f + x).put(0.9025f).put(grey);
            buffer.put(-0.4525f + x).put(-0.9025f).put(grey);
            buffer.put(-0.4475f + x).put(0.9025f).put(grey);
            buffer.put(-0.4525f + x).put(0.9025f).put(grey);

            vertices += 6;
        }
        for(float y = 0f; y <= 1.89f; y += 0.09f) {
            buffer.put(-0.4475f).put(-0.9025f + y).put(grey);
            buffer.put(0.4475f).put(-0.9025f + y).put(grey);
            buffer.put(0.4475f).put(-0.8975f + y).put(grey);
            buffer.put(-0.4475f).put(-0.9025f + y).put(grey);
            buffer.put(0.4475f).put(-0.8975f + y).put(grey);
            buffer.put(-0.4475f).put(-0.8975f + y).put(grey);

            vertices += 6;
        }
    }
    private void render(){
        buffer.flip();

        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(programID);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glDrawArrays(GL_TRIANGLES, 0, vertices);

        buffer.clear();
        vertices = 0;
    }
    public static void wait(double time, double start){
        while(System.currentTimeMillis() - start < time){
            try{ Thread.sleep(1); } catch (InterruptedException e) {}
        }
    }
    public static void main(String[] args){
        new Game();
    }
}